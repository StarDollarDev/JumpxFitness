package Controllers;

import Dao.GaleriaItemDaoImpl;
import Model.GaleriaItem;
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
 * Galería pública de fotos y videos de la cancha.
 * Fotos: se suben como archivo (jpg/png/webp, máx. 5MB) y se guardan en
 * web/assets/uploads/cancha/. Videos: NO se suben como archivo (pesarían
 * demasiado para este servidor); se guarda un link embebido (YouTube,
 * Facebook, etc.) que el frontend muestra en un iframe.
 */
@WebServlet(name = "GaleriaController", urlPatterns = {"/GaleriaController"})
@MultipartConfig(maxFileSize = 5L * 1024 * 1024, maxRequestSize = 6L * 1024 * 1024)
public class GaleriaController extends HttpServlet {

    private final GaleriaItemDaoImpl galeriaDao = new GaleriaItemDaoImpl();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        JsonObject out = new JsonObject();

        try (PrintWriter writer = response.getWriter()) {
            if ("listar".equals(action)) {
                out.addProperty("success", true);
                out.add("data", gson.toJsonTree(galeriaDao.listaActivos()));
                writer.print(out);

            } else if ("listarAdmin".equals(action)) {
                if (!esAdmin(request)) { noAutorizado(response, writer, out); return; }
                out.addProperty("success", true);
                out.add("data", gson.toJsonTree(galeriaDao.listaTodos()));
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

            if ("subirFoto".equals(action)) {
                Part part = request.getPart("archivo");
                String titulo = limpiar(request, "titulo");
                String realPath = getServletContext().getRealPath("/");

                String ruta;
                try {
                    ruta = SubidaArchivoHelper.guardarImagen(part, realPath, "cancha");
                } catch (IllegalArgumentException ex) {
                    out.addProperty("success", false);
                    out.addProperty("message", ex.getMessage());
                    writer.print(out);
                    return;
                }

                GaleriaItem item = new GaleriaItem();
                item.setTipo("FOTO");
                item.setTitulo(titulo);
                item.setUrl(ruta);
                item.setOrden(0);
                item.setActivo(true);

                boolean ok = galeriaDao.insertar(item);
                if (ok) {
                    AuditoriaHelper.registrar(request, "INSERT", "galeria_cancha_jx", item.getIdItem(),
                            "Foto de cancha subida" + (titulo.isEmpty() ? "" : " ('" + titulo + "')"));
                }
                out.addProperty("success", ok);
                out.addProperty("message", ok ? "Foto subida correctamente." : "No se pudo guardar la foto.");
                if (ok) out.add("data", gson.toJsonTree(item));
                writer.print(out);

            } else if ("agregarVideo".equals(action)) {
                String titulo = limpiar(request, "titulo");
                String url = limpiar(request, "url");
                if (url.isEmpty()) {
                    out.addProperty("success", false);
                    out.addProperty("message", "Pega el link del video.");
                    writer.print(out);
                    return;
                }
                GaleriaItem item = new GaleriaItem();
                item.setTipo("VIDEO");
                item.setTitulo(titulo);
                item.setUrl(url);
                item.setOrden(0);
                item.setActivo(true);

                boolean ok = galeriaDao.insertar(item);
                if (ok) {
                    AuditoriaHelper.registrar(request, "INSERT", "galeria_cancha_jx", item.getIdItem(),
                            "Video de cancha agregado" + (titulo.isEmpty() ? "" : " ('" + titulo + "')"));
                }
                out.addProperty("success", ok);
                out.addProperty("message", ok ? "Video agregado correctamente." : "No se pudo agregar el video.");
                if (ok) out.add("data", gson.toJsonTree(item));
                writer.print(out);

            } else if ("eliminar".equals(action)) {
                int idItem = parseIntSeguro(request.getParameter("idItem"));
                GaleriaItem item = galeriaDao.SearchById(idItem);
                if (item == null) {
                    out.addProperty("success", false);
                    out.addProperty("message", "No se encontró ese elemento.");
                    writer.print(out);
                    return;
                }
                boolean ok = galeriaDao.eliminar(idItem);
                if (ok) {
                    if ("FOTO".equals(item.getTipo())) {
                        SubidaArchivoHelper.eliminarSiExiste(item.getUrl(), getServletContext().getRealPath("/"));
                    }
                    AuditoriaHelper.registrar(request, "DELETE", "galeria_cancha_jx", idItem,
                            "Elemento de galería de cancha eliminado");
                }
                out.addProperty("success", ok);
                out.addProperty("message", ok ? "Eliminado." : "No se pudo eliminar.");
                writer.print(out);

            } else if ("reordenar".equals(action)) {
                int idItem = parseIntSeguro(request.getParameter("idItem"));
                int nuevoOrden = parseIntSeguro(request.getParameter("orden"));
                boolean ok = galeriaDao.actualizarOrden(idItem, nuevoOrden);
                out.addProperty("success", ok);
                writer.print(out);

            } else {
                out.addProperty("success", false);
                out.addProperty("message", "Acción no válida");
                writer.print(out);
            }
        } catch (Exception e) {
            response.setStatus(500);
            System.err.println(">>> ERROR GENERAL en GaleriaController <<<");
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
