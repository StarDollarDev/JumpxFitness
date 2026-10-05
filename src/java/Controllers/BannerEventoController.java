package Controllers;

import Dao.BannerEventoDaoImpl;
import Model.BannerEvento;
import Model.Rol;
import Model.Usuario;
import Util.AuditoriaHelper;
import Util.SubidaArchivoHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

/**
 * Banner destacado de eventos de sábados. Solo un banner puede estar activo
 * a la vez (lo garantiza BannerEventoDaoImpl.guardar()); la web pública solo
 * pide "activo" y, si no hay ninguno, simplemente no muestra nada.
 */
@WebServlet(name = "BannerEventoController", urlPatterns = {"/BannerEventoController"})
@MultipartConfig(maxFileSize = 5L * 1024 * 1024, maxRequestSize = 6L * 1024 * 1024)
public class BannerEventoController extends HttpServlet {

    private final BannerEventoDaoImpl bannerDao = new BannerEventoDaoImpl();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        JsonObject out = new JsonObject();

        try (PrintWriter writer = response.getWriter()) {
            if ("activo".equals(action)) {
                BannerEvento b = bannerDao.buscarActivo();
                out.addProperty("success", true);
                out.addProperty("hayBanner", b != null);
                if (b != null) out.add("data", gson.toJsonTree(b));
                writer.print(out);

            } else if ("listar".equals(action)) {
                if (!esAdmin(request)) { noAutorizado(response, writer, out); return; }
                out.addProperty("success", true);
                out.add("data", gson.toJsonTree(bannerDao.listaTodos()));
                writer.print(out);

            } else {
                out.addProperty("success", false);
                out.addProperty("message", "Acción no válida");
                writer.print(out);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        JsonObject out = new JsonObject();

        try (PrintWriter writer = response.getWriter()) {
            if (!esAdmin(request)) { noAutorizado(response, writer, out); return; }

            if ("guardar".equals(action)) {
                int idBanner = parseIntSeguro(request.getParameter("idBanner"));
                String titulo = limpiar(request, "titulo");
                String descripcion = limpiar(request, "descripcion");
                String horaTexto = limpiar(request, "horaTexto");
                boolean activo = "true".equalsIgnoreCase(limpiar(request, "activo"));

                if (titulo.length() < 3 || titulo.length() > 150) {
                    out.addProperty("success", false);
                    out.addProperty("message", "Ingresa un título para el banner.");
                    writer.print(out);
                    return;
                }

                BannerEvento existente = idBanner > 0 ? bannerDao.SearchById(idBanner) : null;
                String imagenFondo = existente != null ? existente.getImagenFondo() : null;

                // La imagen es opcional; si mandan una nueva, reemplaza (y borra) la anterior.
                Part part = request.getPart("imagen");
                if (part != null && part.getSize() > 0) {
                    try {
                        String nuevaRuta = SubidaArchivoHelper.guardarImagen(part, "banner");
                        if (imagenFondo != null) {
                            SubidaArchivoHelper.eliminarSiExiste(imagenFondo);
                        }
                        imagenFondo = nuevaRuta;
                    } catch (IllegalArgumentException ex) {
                        out.addProperty("success", false);
                        out.addProperty("message", ex.getMessage());
                        writer.print(out);
                        return;
                    }
                }

                BannerEvento banner = new BannerEvento();
                banner.setIdBanner(idBanner > 0 ? idBanner : 0);
                banner.setTitulo(titulo);
                banner.setDescripcion(descripcion);
                banner.setHoraTexto(horaTexto);
                banner.setImagenFondo(imagenFondo);
                banner.setActivo(activo);

                boolean ok = bannerDao.guardar(banner);
                if (ok) {
                    AuditoriaHelper.registrar(request, idBanner > 0 ? "UPDATE" : "INSERT",
                            "banner_evento_jx", banner.getIdBanner(),
                            "Banner de sábados '" + titulo + "' guardado (activo=" + activo + ")");
                }
                out.addProperty("success", ok);
                out.addProperty("message", ok ? "Banner guardado." : "No se pudo guardar el banner.");
                if (ok) out.add("data", gson.toJsonTree(banner));
                writer.print(out);

            } else if ("eliminar".equals(action)) {
                int idBanner = parseIntSeguro(request.getParameter("idBanner"));
                BannerEvento banner = bannerDao.SearchById(idBanner);
                boolean ok = bannerDao.eliminar(idBanner);
                if (ok) {
                    if (banner != null && banner.getImagenFondo() != null) {
                        SubidaArchivoHelper.eliminarSiExiste(banner.getImagenFondo());
                    }
                    AuditoriaHelper.registrar(request, "DELETE", "banner_evento_jx", idBanner, "Banner de sábados eliminado");
                }
                out.addProperty("success", ok);
                out.addProperty("message", ok ? "Eliminado." : "No se pudo eliminar.");
                writer.print(out);

            } else {
                out.addProperty("success", false);
                out.addProperty("message", "Acción no válida");
                writer.print(out);
            }
        } catch (Exception e) {
            response.setStatus(500);
            System.err.println(">>> ERROR GENERAL en BannerEventoController <<<");
            e.printStackTrace();
            out.addProperty("success", false);
            out.addProperty("message", "Ocurrió un error al procesar la solicitud.");
            try {
                response.getWriter().print(gson.toJson(out));
            } catch (IOException ignored) {
            }
        }
    }

    private boolean esAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Usuario sesion = session == null ? null : (Usuario) session.getAttribute("usuario");
        return sesion != null && sesion.getRol() == Rol.ADMIN;
    }

    private void noAutorizado(HttpServletResponse response, PrintWriter writer, JsonObject out) {
        response.setStatus(403);
        out.addProperty("success", false);
        out.addProperty("message", "No tienes permiso para realizar esta acción.");
        writer.print(out);
    }

    private int parseIntSeguro(String v) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return -1; }
    }

    private String limpiar(HttpServletRequest request, String param) {
        String v = request.getParameter(param);
        return v == null ? "" : v.trim().replaceAll("[\\u0000-\\u001F]", "");
    }
}
