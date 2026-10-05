package Util;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Guarda una imagen subida por un <input type="file"> en
 * ${catalina.base}/jx-uploads/&lt;subcarpeta&gt;/ — FUERA de la carpeta
 * desplegada de la app. Así un Clean and Build + Deploy nunca la toca: las
 * fotos ya subidas sobreviven a cualquier redespliegue.
 *
 * No hace falta configurar nada: ${catalina.base} ya lo da el propio
 * servidor (es la misma carpeta donde vive tu conf/jx-notify.properties),
 * así que la carpeta de subidas queda en ${catalina.base}/jx-uploads/,
 * se crea sola la primera vez que alguien sube algo.
 *
 * Los archivos se sirven con ArchivoEstaticoController, mapeado a
 * "/uploads/*", porque viven fuera de web/ y Tomcat no los serviría solo.
 */
public final class SubidaArchivoHelper {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024; // 5 MB

    private SubidaArchivoHelper() {
    }

    /**
     * @param part       el archivo recibido (request.getPart("archivo"))
     * @param subcarpeta "cancha" o "banner"
     * @return la ruta relativa a guardar en la BD (ej. "uploads/cancha/uuid.jpg")
     */
    public static String guardarImagen(Part part, String subcarpeta) throws IOException {
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
        Path carpetaDestino = carpetaBase().resolve(subcarpeta);
        Files.createDirectories(carpetaDestino);

        Path destino = carpetaDestino.resolve(nombreArchivo);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        return "uploads/" + subcarpeta + "/" + nombreArchivo;
    }

    /** Borra un archivo previamente guardado con guardarImagen(), si existe. */
    public static void eliminarSiExiste(String rutaRelativa) {
        if (rutaRelativa == null || !rutaRelativa.startsWith("uploads/")) {
            return; // nunca borres nada que no hayamos guardado nosotros mismos
        }
        try {
            Path archivo = resolverDentroDeBase(rutaRelativa.substring("uploads/".length()));
            if (archivo != null) {
                Files.deleteIfExists(archivo);
            }
        } catch (IOException e) {
            System.err.println("No se pudo borrar el archivo " + rutaRelativa + ": " + e.getMessage());
        }
    }

    /**
     * Resuelve "cancha/uuid.jpg" a la ruta real dentro de la carpeta base,
     * verificando que no se escape de ella (protección contra path traversal
     * con "../"). La usa ArchivoEstaticoController para servir el archivo.
     * @return la ruta real, o null si el valor recibido es sospechoso.
     */
    public static Path resolverDentroDeBase(String rutaRelativa) throws IOException {
        Path base = carpetaBase().normalize();
        Path candidato = base.resolve(rutaRelativa).normalize();
        if (!candidato.startsWith(base)) {
            return null; // alguien intentó salirse de la carpeta de subidas
        }
        return candidato;
    }

    private static Path carpetaBase() {
        String base = System.getProperty("catalina.base");
        if (base == null || base.isBlank()) {
            // No debería pasar dentro de Tomcat; red de seguridad por si acaso.
            base = System.getProperty("java.io.tmpdir");
        }
        return Paths.get(base, "jx-uploads");
    }

    private static String extraerExtension(String nombreArchivo) {
        if (nombreArchivo == null) return null;
        int punto = nombreArchivo.lastIndexOf('.');
        if (punto < 0 || punto == nombreArchivo.length() - 1) return null;
        return nombreArchivo.substring(punto + 1).toLowerCase();
    }
}
