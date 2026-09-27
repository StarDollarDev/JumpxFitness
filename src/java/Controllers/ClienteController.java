package Controllers;

import Dao.ClienteDaoImpl;
import Dao.PersonaDaoImpl;
import Dao.RegistroJumpingDaoImpl;
import Dao.UsuarioDaoImpl;
import Interface.ICliente;
import Interface.IPersona;
import Model.Cliente;
import Model.Persona;
import Model.RegistroJumping;
import Model.Rol;
import Model.Usuario;
import Util.AuditoriaHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "ClienteController", urlPatterns = {"/ClienteController"})
public class ClienteController extends HttpServlet {

    private final ICliente cDao = new ClienteDaoImpl();
    private final IPersona pDao = new PersonaDaoImpl();
    private final RegistroJumpingDaoImpl registroDao = new RegistroJumpingDaoImpl();
    private final UsuarioDaoImpl usuarioDao = new UsuarioDaoImpl();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        JsonObject jsonResponse = new JsonObject();

        System.out.println("=== CLIENTE CONTROLLER ===");
        System.out.println("Action: " + action);

        try (PrintWriter out = response.getWriter()) {
            if ("listar".equals(action)) {
                List<Cliente> lista = cDao.lista();
                JsonArray jsonArray = gson.toJsonTree(lista).getAsJsonArray();

                jsonResponse.addProperty("success", true);
                jsonResponse.add("data", jsonArray);
                out.print(jsonResponse.toString());
            } else if ("listarConPlan".equals(action)) {
                HttpSession session = request.getSession(false);
                Usuario sesion = session == null ? null : (Usuario) session.getAttribute("usuario");
                if (sesion == null || sesion.getRol() != Rol.ADMIN) {
                    response.setStatus(403);
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "No tienes permiso para ver esto.");
                    out.print(jsonResponse.toString());
                    return;
                }

                JsonArray jsonArray = new JsonArray();
                for (Cliente c : cDao.lista()) {
                    JsonObject fila = new JsonObject();
                    fila.addProperty("id_cliente", c.getId_cliente());
                    fila.add("persona", gson.toJsonTree(c.getPersona()));

                    // ¿Este cliente tiene cuenta propia (se registró él mismo) o lo dio de
                    // alta el admin sin cuenta? Ambos casos son válidos y conviven.
                    Usuario cuenta = c.getPersona() != null ? usuarioDao.SearchByPersonaId(c.getPersona().getId_persona()) : null;
                    fila.addProperty("tieneCuenta", cuenta != null);
                    fila.addProperty("usuario", cuenta != null ? cuenta.getUsuario() : null);

                    // Plan actual: el más reciente de sus registros (igual que en su propio dashboard).
                    List<RegistroJumping> registros = registroDao.SearchByClienteId(c.getId_cliente());
                    if (registros != null && !registros.isEmpty()) {
                        RegistroJumping ultimo = registros.get(0);
                        LocalDate inicio = ultimo.getFechaIngreso().toLocalDate();
                        LocalDate vence = inicio.plusDays(ultimo.getPlan().getDiasVigencia());
                        long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), vence);

                        fila.addProperty("tienePlan", true);
                        fila.addProperty("nombrePlan", ultimo.getPlan().getNombre());
                        fila.addProperty("montoPlan", ultimo.getMonto());
                        fila.addProperty("fechaInicioPlan", inicio.toString());
                        fila.addProperty("diasRestantes", diasRestantes);
                        fila.addProperty("vencido", diasRestantes < 0);
                    } else {
                        fila.addProperty("tienePlan", false);
                    }

                    jsonArray.add(fila);
                }

                jsonResponse.addProperty("success", true);
                jsonResponse.add("data", jsonArray);
                out.print(jsonResponse.toString());
            } else if ("buscar".equals(action)) {
                String idParam = request.getParameter("id");
                if (idParam != null && !idParam.isEmpty()) {
                    int id = Integer.parseInt(idParam);
                    Cliente c = cDao.SearchById(id);
                    if (c != null) {
                        jsonResponse.addProperty("success", true);
                        jsonResponse.add("data", gson.toJsonTree(c));
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Cliente no encontrado");
                    }
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "ID requerido");
                }
                out.print(jsonResponse.toString());
            } else if ("buscarDocumento".equals(action)){
                String documento = request.getParameter("documento");
                String numeroDoc = request.getParameter("numeroDoc");
                
                if (documento != null && !documento.isEmpty() && numeroDoc != null && !numeroDoc.isEmpty()) {
                    Persona p = pDao.SearchByDocumento(documento, numeroDoc);
                    if (p != null) {
                        Cliente c = cDao.SearchByPersonaId(p.getId_persona());
                        if (c != null) {
                            jsonResponse.addProperty("success", true);
                            jsonResponse.add("data", gson.toJsonTree(c));
                        } else {
                            jsonResponse.addProperty("success", false);
                            jsonResponse.addProperty("message", "Cliente no encontrado");
                        }
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Persona no encontrada");
                    } 
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Documento y número son requeridos");
                }
                out.print(jsonResponse.toString());
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Acción no válida: " + action);
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

        System.out.println("=== CLIENTE CONTROLLER (POST) ===");
        System.out.println("Action: " + action);
        
        try (PrintWriter out = response.getWriter()){
            
            if ("insertar".equals(action)) {
                Persona persona = new Persona();
                persona.setNombre(request.getParameter("nombre"));
                persona.setApellido(request.getParameter("apellido"));
                persona.setDocumento(request.getParameter("documento"));
                persona.setNumeroDoc(request.getParameter("numeroDoc"));
                persona.setTelefono(request.getParameter("telefono"));
                
                if (persona.getNombre() == null || persona.getNombre().trim().isEmpty() ||
                    persona.getApellido() == null || persona.getApellido().trim().isEmpty() ||
                    persona.getDocumento() == null || persona.getDocumento().trim().isEmpty() ||
                    persona.getNumeroDoc() == null || persona.getNumeroDoc().trim().isEmpty()) {
                    
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Todos los campos son requeridos");
                    out.print(jsonResponse.toString());
                    return;
                }
                
                Cliente cliente = new Cliente();
                cliente.setPersona(persona);
                
                boolean resultado = cDao.insertar(cliente);
                
                jsonResponse.addProperty("success", resultado);
                if (resultado) {
                    jsonResponse.addProperty("message", "Cliente insertado correctamente");
                    jsonResponse.addProperty("id_cliente", cliente.getId_cliente());
                    jsonResponse.addProperty("id_persona", cliente.getPersona().getId_persona());
                    jsonResponse.add("data", gson.toJsonTree(cliente));
                    AuditoriaHelper.registrar(request, "INSERT", "cliente", cliente.getId_cliente(),
                            "Alta de cliente '" + persona.getNombre() + " " + persona.getApellido() + "'");
                } else {
                    jsonResponse.addProperty("message", "Error al insertar cliente");
                }
                out.print(jsonResponse.toString());
            } else if ("actualizar".equals(action)){
                String idParam = request.getParameter("id_cliente");
                
                if (idParam != null && !idParam.isEmpty()) {
                    int id = Integer.parseInt(idParam);
                    Cliente clienteExistente = cDao.SearchById(id);
                    
                    if (clienteExistente !=null) {
                        Persona persona = clienteExistente.getPersona();
                        
                        String nombre = request.getParameter("nombre");
                        String apellido = request.getParameter("apellido");
                        String documento = request.getParameter("documento");
                        String numeroDoc = request.getParameter("numeroDoc");
                        String telefono = request.getParameter("telefono");
                        
                        if (nombre != null && !nombre.trim().isEmpty()) {
                            persona.setNombre(nombre.trim());
                        }
                        if (apellido != null && !apellido.trim().isEmpty()) {
                            persona.setApellido(apellido.trim());
                        }
                        if (documento != null && !documento.trim().isEmpty()) {
                            persona.setDocumento(documento.trim());
                        }
                        if (numeroDoc != null && !numeroDoc.trim().isEmpty()) {
                            persona.setNumeroDoc(numeroDoc.trim());
                        }
                        if (telefono != null && !telefono.trim().isEmpty()) {
                            persona.setTelefono(telefono.trim());
                        }
                        boolean resultado = cDao.update(clienteExistente);
                        
                        jsonResponse.addProperty("success", resultado);
                        jsonResponse.addProperty("message", resultado ? "Cliente actualizado correctamente" : "Error al actualizar cliente");
                        
                        if (resultado) {
                            jsonResponse.add("data", gson.toJsonTree(clienteExistente));
                            AuditoriaHelper.registrar(request, "UPDATE", "cliente", id,
                                    "Actualización de cliente '" + persona.getNombre() + " " + persona.getApellido() + "'");
                        }
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Cliente no encontrado");
                    }
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "ID del cliente requerido");
                }
                out.print(jsonResponse.toString());
            } else if ("eliminar".equals(action)){
                String idParam = request.getParameter("id_cliente");
                
                if (idParam != null && !idParam.isEmpty()) {
                    
                    int id = Integer.parseInt(idParam);
                    boolean resultado = cDao.delete(id);
                    
                    jsonResponse.addProperty("success", resultado);
                    jsonResponse.addProperty("message", resultado ? "Cliente eliminado correctamente" : "Error al eliminar cliente");
                    if (resultado) {
                        AuditoriaHelper.registrar(request, "DELETE", "cliente", id, "Eliminación de cliente id " + id);
                    }
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "ID del cliente requerido");
                }
                out.print(jsonResponse.toString());
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Acción no válida: " + action);
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
    public String getServletInfo() {
        return "Short description";
    }

}
