package Controllers;

import Util.SubidaArchivoHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sirve los archivos guardados por SubidaArchivoHelper (fuera del .war, en
 * ${catalina.base}/jx-uploads/) bajo la URL /uploads/&lt;subcarpeta&gt;/&lt;archivo&gt;.
 * Existe porque esos archivos ya no viven dentro de web/, así que un link
 * directo tipo "assets/uploads/..." no los encontraría.
 */
@WebServlet(name = "ArchivoEstaticoController", urlPatterns = {"/uploads/*"})
public class ArchivoEstaticoController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String rutaRelativa = request.getPathInfo(); // ej. "/cancha/uuid.jpg"
        if (rutaRelativa == null || rutaRelativa.isBlank() || rutaRelativa.equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        rutaRelativa = rutaRelativa.startsWith("/") ? rutaRelativa.substring(1) : rutaRelativa;

        Path archivo = SubidaArchivoHelper.resolverDentroDeBase(rutaRelativa);
        if (archivo == null || !Files.isRegularFile(archivo)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(archivo);
        response.setContentType(contentType != null ? contentType : "application/octet-stream");
        response.setContentLengthLong(Files.size(archivo));
        // Las imágenes no cambian nunca (nombre = UUID nuevo cada vez que se sube una),
        // así que el navegador las puede cachear tranquilamente por un buen rato.
        response.setHeader("Cache-Control", "public, max-age=604800"); // 7 días

        try (var in = Files.newInputStream(archivo)) {
            in.transferTo(response.getOutputStream());
        }
    }
}
