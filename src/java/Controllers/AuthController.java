package Controllers;

import Dao.ClienteDaoImpl;
import Dao.PersonaDaoImpl;
import Dao.UsuarioDaoImpl;
import Interface.IUsuario;
import Model.Cliente;
import Model.Persona;
import Model.Rol;
import Model.Usuario;
import Util.AuditoriaHelper;
import Util.JwtHelper;
import Util.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Autenticación unificada: login y registro público en el mismo endpoint.
 *
 * Seguridad del registro: el público SOLO puede crear cuentas de tipo
 * CLIENTE. El rol nunca se lee de lo que manda el navegador; esta clase
 * asigna Rol.CLIENTE de forma fija sin importar qué parámetro llegue, así
 * que no existe ninguna forma de registrarse como ADMIN desde este
 * formulario. Las cuentas ADMIN solo se crean desde UsuarioController por
 * otro administrador ya autenticado.
 *
 * Política de sesión por rol:
 *  - ADMIN: sesión normal de servidor (cookie JSESSIONID sin Max-Age), así
 *    que muere sola al cerrar el navegador; mientras el navegador siga
 *    abierto, no expira por inactividad salvo que pasen ADMIN_SESION_SEG.
 *  - CLIENTE: además de la sesión normal, se emite una cookie persistente
 *    ("jx_remember", un JWT propio firmado con HS256, ver Util/JwtHelper)
 *    con Max-Age de 7 días. Si el cliente vuelve tras cerrar el navegador y
 *    ya no tiene sesión activa, "verificar" reconoce esa cookie y le
 *    recrea la sesión sola, sin pedirle loguearse de nuevo.
 */
@WebServlet(name = "AuthController", urlPatterns = {"/AuthController"})
public class AuthController extends HttpServlet {

    private final IUsuario uDao = new UsuarioDaoImpl();
    private final ClienteDaoImpl clienteDao = new ClienteDaoImpl();
    private final PersonaDaoImpl personaDao = new PersonaDaoImpl();
    private final Gson gson = new Gson();

    private static final Pattern PASSWORD_VALIDA = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+() \\-]{7,20}$");

    private static final String COOKIE_REMEMBER = "jx_remember";
    private static final int REMEMBER_DIAS = 7;
    private static final int ADMIN_SESION_SEG = -1;              // -1 = nunca expira por inactividad; solo muere al cerrar el navegador (cookie de sesión)
    private static final int CLIENTE_SESION_SEG = 12 * 60 * 60; // la persistencia real de 7 días la da la cookie, no esto

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        JsonObject jsonResponse = new JsonObject();

        try (PrintWriter out = response.getWriter()) {

            if ("verificar".equals(action)) {
                HttpSession session = request.getSession(false);
                Usuario us = session == null ? null : (Usuario) session.getAttribute("usuario");

                // Sin sesión activa: intenta reconocer la cookie persistente del cliente
                // (solo aplica a CLIENTE; un admin siempre debe volver a loguearse).
                if (us == null) {
                    us = intentarReautenticarPorCookie(request);
                    if (us != null) {
                        HttpSession nueva = request.getSession(true);
                        nueva.setAttribute("usuario", us);
                        nueva.setMaxInactiveInterval(CLIENTE_SESION_SEG);
                    }
                }

                if (us != null) {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("logueado", true);
                    jsonResponse.addProperty("usuario", us.getUsuario());
                    jsonResponse.addProperty("rol", us.getRol().name());
                    if (us.getPersona() != null) {
                        String nombreCompleto = (us.getPersona().getNombre() + " " + us.getPersona().getApellido()).trim();
                        jsonResponse.addProperty("nombreCompleto", nombreCompleto.isEmpty() ? us.getUsuario() : nombreCompleto);
                    }
                } else {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("logueado", false);
                }
                out.print(jsonResponse.toString());
            }
        } catch (Exception e) {
            response.setStatus(500);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Error: " + e.getMessage());
            response.getWriter().print(jsonResponse.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        JsonObject jsonResponse = new JsonObject();

        try (PrintWriter out = response.getWriter()) {

            if ("login".equals(action)) {
                String user = request.getParameter("usuario");
                String pass = request.getParameter("password");

                if (!RateLimiter.permitir("login:" + clienteIp(request), 10, 10 * 60 * 1000L)) {
                    response.setStatus(429);
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Demasiados intentos. Espera unos minutos e inténtalo de nuevo.");
                    out.print(jsonResponse.toString());
                    return;
                }

                // validate() compara con BCrypt internamente: se envia en texto plano.
                Usuario us = uDao.validate(user, pass);

                if (us != null && us.getUsuario() != null) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute("usuario", us);

                    if (us.getRol() == Rol.CLIENTE) {
                        session.setMaxInactiveInterval(CLIENTE_SESION_SEG);
                        emitirCookieRemember(request, response, us.getId_usuario());
                    } else {
                        session.setMaxInactiveInterval(ADMIN_SESION_SEG);
                        // Los admins no reciben cookie persistente: su sesión debe morir
                        // al cerrar el navegador (JSESSIONID por defecto, sin Max-Age).
                    }

                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Inicio de sesión exitoso");
                    jsonResponse.addProperty("usuario", us.getUsuario());
                    jsonResponse.addProperty("rol", us.getRol().name());
                    jsonResponse.add("userData", gson.toJsonTree(us));

                    AuditoriaHelper.registrar(request, "LOGIN", "usuario", us.getId_usuario(),
                            "Inicio de sesión exitoso de '" + us.getUsuario() + "'");
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Usuario o contraseña incorrecta");

                    AuditoriaHelper.registrarConUsuario(request, null, user, "LOGIN_FALLIDO", "usuario", null,
                            "Intento de inicio de sesión fallido para '" + user + "'");
                }
                out.print(jsonResponse.toString());

            } else if ("registro".equals(action)) {
                registrarCliente(request, response, out, jsonResponse);

            } else if ("logout".equals(action)) {
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("usuario") != null) {
                    Usuario us = (Usuario) session.getAttribute("usuario");
                    AuditoriaHelper.registrar(request, "LOGOUT", "usuario", us.getId_usuario(),
                            "Cierre de sesión de '" + us.getUsuario() + "'");
                }
                if (session != null) {
                    session.invalidate();
                }
                borrarCookieRemember(request, response);

                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Sesión cerrada exitosamente");

                out.print(jsonResponse.toString());
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Acción no válida");
                out.print(jsonResponse.toString());
            }
        } catch (Exception e) {
            response.setStatus(500);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Error: " + e.getMessage());
            try {
                response.getWriter().print(gson.toJson(jsonResponse));
            } catch (IOException ignored) {
            }
        }
    }

    // ------------------------------------------------------------------
    // Sesión persistente de 7 días (solo CLIENTE)
    // ------------------------------------------------------------------

    private void emitirCookieRemember(HttpServletRequest request, HttpServletResponse response, int idUsuario) {
        String token = JwtHelper.generar(idUsuario, REMEMBER_DIAS);
        Cookie cookie = new Cookie(COOKIE_REMEMBER, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(REMEMBER_DIAS * 24 * 60 * 60);
        cookie.setSecure(request.isSecure()); // Secure solo si ya estás en HTTPS
        response.addCookie(cookie);
    }

    private void borrarCookieRemember(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_REMEMBER, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }

    /** Si hay una cookie "jx_remember" válida y no vencida, devuelve al Usuario dueño (siempre CLIENTE); si no, null. */
    private Usuario intentarReautenticarPorCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie c : cookies) {
            if (COOKIE_REMEMBER.equals(c.getName())) {
                Integer idUsuario = JwtHelper.validar(c.getValue());
                if (idUsuario == null) return null;
                Usuario us = uDao.SearchById(idUsuario);
                // Defensa extra: aunque nunca deberíamos emitir esta cookie para un ADMIN,
                // si por lo que sea llegara una, la ignoramos igual.
                return (us != null && us.getRol() == Rol.CLIENTE) ? us : null;
            }
        }
        return null;
    }

    /**
     * Registro público de CLIENTES. El rol NO se toma de request: siempre
     * es Rol.CLIENTE, sin excepción (ver nota de seguridad de la clase).
     */
    private void registrarCliente(HttpServletRequest request, HttpServletResponse response,
                                   PrintWriter out, JsonObject jsonResponse) {

        if (!RateLimiter.permitir("registro:" + clienteIp(request), 5, 15 * 60 * 1000L)) {
            response.setStatus(429);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Demasiados intentos de registro. Espera unos minutos.");
            out.print(jsonResponse.toString());
            return;
        }

        String usuarioTxt = limpiar(request, "usuario");
        String password = request.getParameter("password") == null ? "" : request.getParameter("password");
        String nombre = limpiar(request, "nombre");
        String apellido = limpiar(request, "apellido");
        String documento = limpiar(request, "documento").toUpperCase(); // DNI | CE
        String numeroDoc = limpiar(request, "numeroDoc");
        String telefono = limpiar(request, "telefono");

        if (usuarioTxt.length() < 4 || usuarioTxt.length() > 40) {
            fallar(out, jsonResponse, "El usuario debe tener entre 4 y 40 caracteres.");
            return;
        }
        if (!PASSWORD_VALIDA.matcher(password).matches()) {
            fallar(out, jsonResponse, "La contraseña debe tener al menos 8 caracteres, con al menos una mayúscula y un número.");
            return;
        }
        if (password.length() > 72) {
            fallar(out, jsonResponse, "La contraseña es demasiado larga (máximo 72 caracteres).");
            return;
        }
        if (nombre.length() < 2 || nombre.length() > 60 || apellido.length() < 2 || apellido.length() > 60) {
            fallar(out, jsonResponse, "Ingresa tu nombre y apellido.");
            return;
        }
        if (!documento.equals("DNI") && !documento.equals("CE")) {
            fallar(out, jsonResponse, "Selecciona un tipo de documento válido (DNI o CE).");
            return;
        }
        if (numeroDoc.length() < 6 || numeroDoc.length() > 15) {
            fallar(out, jsonResponse, "Ingresa un número de documento válido.");
            return;
        }
        if (!PHONE.matcher(telefono).matches()) {
            fallar(out, jsonResponse, "Ingresa un número de teléfono válido.");
            return;
        }

        if (uDao.SearchByUsername(usuarioTxt) != null) {
            fallar(out, jsonResponse, "Ese nombre de usuario ya está en uso.");
            return;
        }
        if (personaDao.existeNumeroDoc(numeroDoc)) {
            fallar(out, jsonResponse, "Ese número de documento ya está registrado.");
            return;
        }
        if (personaDao.existeTelefono(telefono)) {
            fallar(out, jsonResponse, "Ese número de teléfono ya está en uso.");
            return;
        }

        Persona persona = new Persona();
        persona.setNombre(nombre);
        persona.setApellido(apellido);
        persona.setDocumento(documento);
        persona.setNumeroDoc(numeroDoc);
        persona.setTelefono(telefono);

        Usuario nuevo = new Usuario();
        nuevo.setUsuario(usuarioTxt);
        nuevo.setContraseña(nuevo.HashPassword(password));
        nuevo.setRol(Rol.CLIENTE); // fijo: nunca viene del request
        nuevo.setPersona(persona);

        boolean ok = uDao.insertar(nuevo);
        if (!ok) {
            fallar(out, jsonResponse, "No se pudo completar el registro. Inténtalo nuevamente.");
            return;
        }

        // Todo cliente con cuenta necesita también su fila en `cliente` para
        // poder contratar/renovar planes (registrojumping referencia id_cliente).
        Cliente cliente = clienteDao.crearParaPersonaExistente(nuevo.getPersona().getId_persona());
        if (cliente == null) {
            System.err.println("Aviso: se creó el usuario " + nuevo.getUsuario()
                    + " pero no se pudo crear su fila en 'cliente'.");
        }

        AuditoriaHelper.registrarConUsuario(request, nuevo.getId_usuario(), nuevo.getUsuario(),
                "INSERT", "usuario", nuevo.getId_usuario(),
                "Autorregistro de cliente '" + nuevo.getUsuario() + "'");

        jsonResponse.addProperty("success", true);
        jsonResponse.addProperty("message", "¡Cuenta creada! Ya puedes iniciar sesión.");
        out.print(jsonResponse.toString());
    }

    private void fallar(PrintWriter out, JsonObject jsonResponse, String mensaje) {
        jsonResponse.addProperty("success", false);
        jsonResponse.addProperty("message", mensaje);
        out.print(jsonResponse.toString());
    }

    private String limpiar(HttpServletRequest request, String param) {
        String v = request.getParameter(param);
        if (v == null) {
            return "";
        }
        return v.trim().replaceAll("[\\u0000-\\u001F]", "");
    }

    private String clienteIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
