package Util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implementación mínima de JWT (HS256) sin librerías externas, para la
 * sesión persistente de 7 días de los clientes (cookie "jx_remember").
 * No es para reemplazar HttpSession del todo: solo sirve para reconocer
 * automáticamente a un cliente que vuelve tras cerrar el navegador, y
 * recrearle una sesión normal (ver AuthController).
 *
 * La firma se valida en el servidor con una llave secreta que NUNCA sale de
 * aquí. Se lee (en este orden) de variable de entorno, propiedad -D, o
 * ${catalina.base}/conf/jx-notify.properties (clave JX_JWT_SECRET). Si no
 * está configurada, se genera una al azar por arranque del servidor —
 * funciona, pero invalida las sesiones largas existentes cada reinicio;
 * para que la sesión de 7 días sobreviva reinicios del servidor, define
 * JX_JWT_SECRET con un valor fijo (cualquier texto largo y aleatorio).
 */
public final class JwtHelper {

    private static final String ALGORITMO_HMAC = "HmacSHA256";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private static final Properties ARCHIVO = cargarArchivo();
    private static final byte[] SECRETO = resolverSecreto();

    private JwtHelper() {
    }

    /** Genera un token válido por {@code dias} días para este id de usuario. */
    public static String generar(int idUsuario, int dias) {
        long exp = Instant.now().getEpochSecond() + (dias * 24L * 60 * 60);
        JsonObject payload = new JsonObject();
        payload.addProperty("sub", idUsuario);
        payload.addProperty("exp", exp);

        String headerB64 = B64.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = B64.encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
        String firma = firmar(headerB64 + "." + payloadB64);
        return headerB64 + "." + payloadB64 + "." + firma;
    }

    /** @return el id de usuario si el token es válido, no fue alterado y no expiró; null en cualquier otro caso. */
    public static Integer validar(String token) {
        try {
            if (token == null) return null;
            String[] partes = token.split("\\.");
            if (partes.length != 3) return null;

            String firmaEsperada = firmar(partes[0] + "." + partes[1]);
            byte[] a = firmaEsperada.getBytes(StandardCharsets.UTF_8);
            byte[] b = partes[2].getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(a, b)) {
                return null; // firma inválida: alguien lo modificó o la llave cambió
            }

            JsonObject payload = JsonParser.parseString(
                    new String(B64D.decode(partes[1]), StandardCharsets.UTF_8)).getAsJsonObject();
            long exp = payload.get("exp").getAsLong();
            if (Instant.now().getEpochSecond() > exp) {
                return null; // expiró (más de 7 días)
            }
            return payload.get("sub").getAsInt();
        } catch (Exception e) {
            return null; // cualquier token mal formado se trata como inválido, nunca como error
        }
    }

    private static String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            mac.init(new SecretKeySpec(SECRETO, ALGORITMO_HMAC));
            return B64.encodeToString(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token JWT", e);
        }
    }

    private static byte[] resolverSecreto() {
        String v = System.getenv("JX_JWT_SECRET");
        if (v == null || v.isBlank()) v = System.getProperty("JX_JWT_SECRET");
        if (v == null || v.isBlank()) v = ARCHIVO.getProperty("JX_JWT_SECRET");
        if (v != null && !v.isBlank()) {
            return v.trim().getBytes(StandardCharsets.UTF_8);
        }
        System.out.println("[JwtHelper] Aviso: JX_JWT_SECRET no está configurado; se genera uno temporal "
                + "para este arranque. Las sesiones largas de clientes no sobrevivirán un reinicio del "
                + "servidor hasta que definas JX_JWT_SECRET.");
        byte[] aleatorio = new byte[32];
        new SecureRandom().nextBytes(aleatorio);
        return aleatorio;
    }

    private static Properties cargarArchivo() {
        Properties p = new Properties();
        String base = System.getProperty("catalina.base");
        if (base != null) {
            Path f = Paths.get(base, "conf", "jx-notify.properties");
            if (Files.isReadable(f)) {
                try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    p.load(r);
                } catch (IOException ignored) {
                }
            }
        }
        return p;
    }
}
