package Util;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Guarda una imagen subida por un <input type="file"> dentro de la propia
 * carpeta desplegada de la app (web/assets/uploads/&lt;subcarpeta&gt;/).
 *
 * IMPORTANTE (avísalo si no lo sabías): si vuelves a desplegar el .war desde
 * cero (no un simple redeploy en caliente), Tomcat puede recrear la carpeta
 * de la app y estos archivos se perderían. Para un uso serio a futuro,
 * conviene guardar las imágenes fuera del .war (una carpeta aparte en el
 * servidor) o en un servicio externo; para el tamaño de este proyecto, esto
 * alcanza siempre que no borres/redespliegues desde cero sin respaldar
 * assets/uploads.
 */
public final class SubidaArchivoHelper {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024; // 5 MB

    private SubidaArchivoHelper() {
    }

    /**
     * @param part       el archivo recibido (request.getPart("archivo"))
     * @param realPathWeb el real path de /web que da el servlet (getServletContext().getRealPath("/"))
     * @param subcarpeta "cancha" o "banner"
     * @return la ruta relativa a guardar en la BD (ej. "assets/uploads/cancha/uuid.jpg"), o null si falló validación
     */
    public static String guardarImagen(Part part, String realPathWeb, String subcarpeta) throws IOException {
        if (part == null || part.getSize() == 0) {
            throw new IllegalArgumentException("No se recibió ningún archivo.");
        }
        if (part.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("La imagen supera el máximo de 5 MB.");
        }

        String nombreOriginal = part.getSubmittedFileName();
        String extension = extraerExtension(nombreOriginal);
        if (extension == null || !EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Formato no permitido. Usa JPG, PNG o WEBP.");
        }

        String nombreArchivo = UUID.randomUUID() + "." + extension;
        Path carpetaDestino = Paths.get(realPathWeb, "assets", "uploads", subcarpeta);
        Files.createDirectories(carpetaDestino);

        Path destino = carpetaDestino.resolve(nombreArchivo);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        return "assets/uploads/" + subcarpeta + "/" + nombreArchivo;
    }

    /** Borra un archivo previamente guardado con guardarImagen(), si existe. */
    public static void eliminarSiExiste(String rutaRelativa, String realPathWeb) {
        if (rutaRelativa == null || rutaRelativa.isBlank() || !rutaRelativa.startsWith("assets/uploads/")) {
            return; // nunca borres nada que no hayamos guardado nosotros mismos
        }
        try {
            Path archivo = Paths.get(realPathWeb, rutaRelativa);
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            System.err.println("No se pudo borrar el archivo " + rutaRelativa + ": " + e.getMessage());
        }
    }

    private static String extraerExtension(String nombreArchivo) {
        if (nombreArchivo == null) return null;
        int punto = nombreArchivo.lastIndexOf('.');
        if (punto < 0 || punto == nombreArchivo.length() - 1) return null;
        return nombreArchivo.substring(punto + 1).toLowerCase();
    }
}
