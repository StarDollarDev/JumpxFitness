
package Controllers;

import Dao.ClienteDaoImpl;
import Dao.ReservaCanchaDaoImpl;
import Interface.ICliente;
import Interface.IReservaCancha;
import Model.Cliente;
import Model.Deporte;
import Model.EstadoPago;
import Model.EstadoReserva;
import Model.MetodoPago;
import Model.Persona;
import Model.ReservaCancha;
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
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@WebServlet(name = "ReservaCanchaController", urlPatterns = {"/ReservaCanchaController"})
public class ReservaCanchaController extends HttpServlet {

    private final IReservaCancha rDao = new ReservaCanchaDaoImpl();
    private final ICliente cDao = new ClienteDaoImpl();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        JsonObject jsonResponse = new JsonObject();

        System.out.println("=== RESERVA CANCHA CONTROLLER (GET) ===");
        System.out.println("Action: " + action);

        try (PrintWriter out = response.getWriter()) {
            if ("listar".equals(action)) {
                List<ReservaCancha> lista = rDao.lista();
                JsonArray jsonArray = gson.toJsonTree(lista).getAsJsonArray();

                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Reservas de cancha listadas correctamente");
                jsonResponse.add("data", jsonArray);
                out.print(jsonResponse.toString());

            } else if ("buscar".equals(action)) {
                String idParam = request.getParameter("id");
                if (idParam != null && !idParam.isEmpty()) {
                    int id = Integer.parseInt(idParam);
                    ReservaCancha r = rDao.SearchById(id);
                    if (r != null) {
                        jsonResponse.addProperty("success", true);
                        jsonResponse.add("data", gson.toJsonTree(r));
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Reserva no encontrada");
                    }
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "ID requerido");
                }
                out.print(jsonResponse.toString());

            } else if ("listarPorCliente".equals(action)) {
                String idClienteParam = request.getParameter("idCliente");
                if (idClienteParam != null && !idClienteParam.isEmpty()) {
                    int idCliente = Integer.parseInt(idClienteParam);
                    List<ReservaCancha> lista = rDao.SearchByClienteId(idCliente);
                    JsonArray jsonArray = gson.toJsonTree(lista).getAsJsonArray();

                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Reservas del cliente listadas correctamente");
                    jsonResponse.add("data", jsonArray);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "ID de cliente requerido");
                }
                out.print(jsonResponse.toString());

            } else if ("listarPorFecha".equals(action)) {
                String fechaStr = request.getParameter("fecha");
                if (fechaStr != null && !fechaStr.isEmpty()) {
                    Date fecha = Date.valueOf(fechaStr);
                    List<ReservaCancha> lista = rDao.SearchByFecha(fecha);
                    JsonArray jsonArray = gson.toJsonTree(lista).getAsJsonArray();

                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Reservas por fecha listadas correctamente");
                    jsonResponse.add("data", jsonArray);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Fecha requerida (formato: YYYY-MM-DD)");
                }
                out.print(jsonResponse.toString());

            } else if ("listarPorEstado".equals(action)) {
                String estado = request.getParameter("estado");
                if (estado != null && !estado.isEmpty()) {
                    List<ReservaCancha> lista = rDao.SearchByEstadoReserva(estado);
                    JsonArray jsonArray = gson.toJsonTree(lista).getAsJsonArray();

                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Reservas por estado listadas correctamente");
                    jsonResponse.add("data", jsonArray);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Estado requerido");
                }
                out.print(jsonResponse.toString());

            } else if ("listarPorDeporte".equals(action)) {
                String deporte = request.getParameter("deporte");
                if (deporte != null && !deporte.isEmpty()) {
                    List<ReservaCancha> lista = rDao.SearchByDeporte(deporte);
                    JsonArray jsonArray = gson.toJsonTree(lista).getAsJsonArray();

                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Reservas por deporte listadas correctamente");
                    jsonResponse.add("data", jsonArray);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Deporte requerido (FUTBOL o VOLEY)");
                }
                out.print(jsonResponse.toString());

            } else if ("reservasActivas".equals(action)) {
                List<ReservaCancha> lista = rDao.SearchReservasActivas();
                JsonArray jsonArray = gson.toJsonTree(lista).getAsJsonArray();

                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Reservas activas listadas correctamente");
                jsonResponse.add("data", jsonArray);
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

        System.out.println("=== RESERVA CANCHA CONTROLLER (POST) ===");
        System.out.println("Action: " + action);

        try (PrintWriter out = response.getWriter()) {
            if ("solicitarPublica".equals(action)) {

                // Solicitud pública: NO requiere iniciar sesión ni tener cuenta.
                // La solicitud queda PENDIENTE para que el administrador la confirme.
                String nombre = request.getParameter("nombre");
                String telefono = request.getParameter("telefono");
                String deporte = request.getParameter("deporte");
                String fechaStr = request.getParameter("fecha");
                String horaInicioStr = request.getParameter("horaInicio");
                String horaFinStr = request.getParameter("horaFin");

                if (nombre == null || nombre.trim().isEmpty()
                        || telefono == null || telefono.trim().isEmpty()
                        || deporte == null || deporte.isEmpty()
                        || fechaStr == null || fechaStr.isEmpty()
                        || horaInicioStr == null || horaInicioStr.isEmpty()
                        || horaFinStr == null || horaFinStr.isEmpty()) {

                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message",
                            "Nombre, teléfono, deporte, fecha y horario son requeridos");
                    out.print(jsonResponse.toString());
                    return;
                }

                try {
                    LocalDate fechaLocal = LocalDate.parse(fechaStr);
                    LocalTime horaInicioLocal = LocalTime.parse(horaInicioStr);
                    LocalTime horaFinLocal = LocalTime.parse(horaFinStr);

                    if (fechaLocal.isBefore(LocalDate.now())) {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "La fecha de reserva no puede ser anterior a hoy");
                        out.print(jsonResponse.toString());
                        return;
                    }

                    long minutosReserva = java.time.Duration
                            .between(horaInicioLocal, horaFinLocal).toMinutes();

                    if (minutosReserva <= 0) {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "La hora de fin debe ser posterior a la hora de inicio");
                        out.print(jsonResponse.toString());
                        return;
                    }

                    if (minutosReserva != 60 && minutosReserva != 90 && minutosReserva != 120) {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "La reserva debe ser de 1, 1.5 o 2 horas");
                        out.print(jsonResponse.toString());
                        return;
                    }

                    // La reserva pública no crea una cuenta de acceso.
                    // Se crea únicamente el registro de cliente necesario para
                    // relacionarlo con la reserva.
                    Persona persona = new Persona();
                    persona.setNombre(nombre.trim());
                    persona.setApellido("Reserva web");
                    persona.setDocumento("WEB");
                    persona.setNumeroDoc("WEB-" + System.currentTimeMillis());
                    persona.setTelefono(telefono.trim());

                    Cliente nuevoCliente = new Cliente();
                    nuevoCliente.setPersona(persona);

                    if (!cDao.insertar(nuevoCliente)) {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "No se pudo registrar los datos del cliente");
                        out.print(jsonResponse.toString());
                        return;
                    }

                    // Mantiene el precio que actualmente utiliza el formulario
                    // administrativo del proyecto.
                    double precioPorHora = 30.0;
                    double total = (minutosReserva / 60.0) * precioPorHora;

                    ReservaCancha reserva = new ReservaCancha();
                    reserva.setCliente(nuevoCliente);
                    reserva.setDeporte(Deporte.valueOf(deporte));
                    reserva.setPrecioHora(precioPorHora);
                    reserva.setMetodoPago(null);
                    reserva.setFecha(Date.valueOf(fechaLocal));
                    reserva.setHoraInicio(Time.valueOf(horaInicioLocal));
                    reserva.setHoraFin(Time.valueOf(horaFinLocal));
                    reserva.setMontoAdelanto(0.0);
                    reserva.setFaltaPagar(total);
                    reserva.setTotal(total);
                    reserva.setEstadoPago(EstadoPago.PENDIENTE);
                    reserva.setEstadoReserva(EstadoReserva.PENDIENTE);

                    boolean resultado = rDao.insertar(reserva);

                    jsonResponse.addProperty("success", resultado);

                    if (resultado) {
                        jsonResponse.addProperty("message",
                                "Solicitud de reserva enviada correctamente");
                        jsonResponse.addProperty("id", reserva.getId_reserva());
                        jsonResponse.add("data", gson.toJsonTree(reserva));

                        AuditoriaHelper.registrar(request, "INSERT", "reservacancha",
                                reserva.getId_reserva(),
                                "Solicitud pública de reserva de " + nombre.trim()
                                + " el " + fechaStr + " " + horaInicioStr + "-" + horaFinStr);
                    } else {
                        jsonResponse.addProperty("message",
                                "No se pudo registrar la solicitud de reserva");
                    }

                    out.print(jsonResponse.toString());

                } catch (Exception e) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "No se pudo procesar la solicitud: " + e.getMessage());
                    out.print(jsonResponse.toString());
                }

            } else if ("insertar".equals(action)) {

                
                String idClienteStr = request.getParameter("idCliente");
                String deporte = request.getParameter("deporte");
                String precioHoraStr = request.getParameter("precioHora");
                String metodoPago = request.getParameter("metodoPago");
                String fechaStr = request.getParameter("fecha");
                String horaInicioStr = request.getParameter("horaInicio");
                String horaFinStr = request.getParameter("horaFin");
                String montoAdelantoStr = request.getParameter("montoAdelanto");
                String estadoPagoStr = request.getParameter("estadoPago");
                String estadoReservaStr = request.getParameter("estadoReserva");

                // Datos para crear cliente
                String nombre = request.getParameter("nombre");
                String apellido = request.getParameter("apellido");
                String documento = request.getParameter("documento");
                String numeroDoc = request.getParameter("numeroDoc");
                String telefono = request.getParameter("telefono");

                if (deporte == null || deporte.isEmpty()
                        || precioHoraStr == null || precioHoraStr.isEmpty()
                        || fechaStr == null || fechaStr.isEmpty()
                        || horaInicioStr == null || horaInicioStr.isEmpty()
                        || horaFinStr == null || horaFinStr.isEmpty()) {

                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message",
                            "deporte, precioHora, fecha, horaInicio y horaFin son requeridos");
                    out.print(jsonResponse.toString());
                    return;
                }

                try {

                    Cliente cliente = null;
                    boolean clienteNuevo = false;

                    // CLIENTE EXISTENTE
                    
                    if (idClienteStr != null && !idClienteStr.isEmpty()) {

                        int idCliente = Integer.parseInt(idClienteStr);

                        cliente = cDao.SearchById(idCliente);

                        if (cliente == null) {
                            jsonResponse.addProperty("success", false);
                            jsonResponse.addProperty("message", "Cliente no encontrado");
                            out.print(jsonResponse.toString());
                            return;
                        }

                    } 
                    // CREAR CLIENTE NUEVO

                    else {

                        if (nombre == null || nombre.isEmpty()
                                || apellido == null || apellido.isEmpty()
                                || documento == null || documento.isEmpty()
                                || numeroDoc == null || numeroDoc.isEmpty()) {

                            jsonResponse.addProperty("success", false);
                            jsonResponse.addProperty("message",
                                    "Debe enviar idCliente o los datos del nuevo cliente.");
                            out.print(jsonResponse.toString());
                            return;
                        }

                        Persona persona = new Persona();
                        persona.setNombre(nombre);
                        persona.setApellido(apellido);
                        persona.setDocumento(documento);
                        persona.setNumeroDoc(numeroDoc);
                        persona.setTelefono(telefono);

                        Cliente nuevoCliente = new Cliente();
                        nuevoCliente.setPersona(persona);

                        boolean creado = cDao.insertar(nuevoCliente);

                        if (!creado) {
                            jsonResponse.addProperty("success", false);
                            jsonResponse.addProperty("message", "No se pudo crear el cliente");
                            out.print(jsonResponse.toString());
                            return;
                        }

                        cliente = nuevoCliente;
                        clienteNuevo = true;
                    }
                    
                    // PRECIO POR HORA (enviado directamente, ya no hay tabla de tarifas)

                    double precioPorHora = Double.parseDouble(precioHoraStr);

                    Date fecha = Date.valueOf(fechaStr);

                    Time horaInicio = Time.valueOf(horaInicioStr + ":00");

                    LocalTime horaInicioLocal = LocalTime.parse(horaInicioStr);
                    LocalTime horaFinLocal = LocalTime.parse(horaFinStr);

                    // La duración ya no se recibe: se deduce del rango reservado.
                    long minutosReserva = java.time.Duration
                            .between(horaInicioLocal, horaFinLocal).toMinutes();

                    if (minutosReserva <= 0) {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message",
                                "La hora de fin debe ser posterior a la hora de inicio");
                        out.print(jsonResponse.toString());
                        return;
                    }

                    Time horaFin = Time.valueOf(horaFinLocal);

                    // ==========================
                    // CALCULAR TOTAL (según horas alquiladas)
                    // ==========================
                    double total = (minutosReserva / 60.0) * precioPorHora;

                    double montoAdelanto = montoAdelantoStr != null
                            && !montoAdelantoStr.isEmpty()
                            ? Double.parseDouble(montoAdelantoStr)
                            : 0.0;

                    double faltaPagar = total - montoAdelanto;

                    if (faltaPagar < 0) {
                        faltaPagar = 0;
                    }

                    // ==========================
                    // CREAR RESERVA
                    // ==========================
                    ReservaCancha reserva = new ReservaCancha();

                    reserva.setCliente(cliente);
                    reserva.setDeporte(Deporte.valueOf(deporte));
                    reserva.setPrecioHora(precioPorHora);

                    reserva.setMetodoPago(
                            metodoPago != null && !metodoPago.isEmpty()
                            ? MetodoPago.valueOf(metodoPago)
                            : null);

                    reserva.setFecha(fecha);
                    reserva.setHoraInicio(horaInicio);
                    reserva.setHoraFin(horaFin);
                    reserva.setMontoAdelanto(montoAdelanto);
                    reserva.setFaltaPagar(faltaPagar);
                    reserva.setTotal(total);

                    reserva.setEstadoPago(
                            estadoPagoStr != null && !estadoPagoStr.isEmpty()
                            ? EstadoPago.valueOf(estadoPagoStr)
                            : EstadoPago.PENDIENTE);

                    reserva.setEstadoReserva(
                            estadoReservaStr != null && !estadoReservaStr.isEmpty()
                            ? EstadoReserva.valueOf(estadoReservaStr)
                            : EstadoReserva.CONFIRMADO);

                    boolean resultado = rDao.insertar(reserva);

                    jsonResponse.addProperty("success", resultado);

                    if (resultado) {

                        jsonResponse.addProperty(
                                "message",
                                clienteNuevo
                                        ? "Cliente creado y reserva registrada correctamente"
                                        : "Reserva registrada correctamente");

                        jsonResponse.addProperty("id", reserva.getId_reserva());
                        jsonResponse.addProperty("total", total);
                        jsonResponse.addProperty("faltaPagar", faltaPagar);

                        jsonResponse.add("data", gson.toJsonTree(reserva));

                        AuditoriaHelper.registrar(request, "INSERT", "reservacancha", reserva.getId_reserva(),
                                "Reserva de " + deporte + " el " + fechaStr + " " + horaInicioStr + "-" + horaFinStr
                                + " por " + total);

                    } else {

                        jsonResponse.addProperty("message",
                                "Error al registrar la reserva");

                    }

                    out.print(jsonResponse.toString());

                } catch (NumberFormatException e) {

                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message",
                            "Error en formato de números");

                    out.print(jsonResponse.toString());

                } catch (Exception e) {

                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", e.getMessage());

                    out.print(jsonResponse.toString());

                }

            } else if ("actualizar".equals(action)) {
                String idParam = request.getParameter("id");
                String idClienteStr = request.getParameter("idCliente");
                String deporte = request.getParameter("deporte");
                String precioHoraStr = request.getParameter("precioHora");
                String metodoPago = request.getParameter("metodoPago");
                String fechaStr = request.getParameter("fecha");
                String horaInicioStr = request.getParameter("horaInicio");
                String horaFinStr = request.getParameter("horaFin");
                String montoAdelantoStr = request.getParameter("montoAdelanto");
                String estadoPagoStr = request.getParameter("estadoPago");
                String estadoReservaStr = request.getParameter("estadoReserva");

                if (idParam == null || idParam.isEmpty()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "ID de la reserva requerido");
                    out.print(jsonResponse.toString());
                    return;
                }

                int id = Integer.parseInt(idParam);
                ReservaCancha reserva = rDao.SearchById(id);

                if (reserva == null) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Reserva no encontrada");
                    out.print(jsonResponse.toString());
                    return;
                }
                try {
                    if (idClienteStr != null && !idClienteStr.isEmpty()) {
                        Cliente cliente = cDao.SearchById(Integer.parseInt(idClienteStr));
                        if (cliente != null) {
                            reserva.setCliente(cliente);
                        }
                    }
                    if (deporte != null && !deporte.isEmpty()) {
                        reserva.setDeporte(Deporte.valueOf(deporte));
                    }
                    if (precioHoraStr != null && !precioHoraStr.isEmpty()) {
                        reserva.setPrecioHora(Double.parseDouble(precioHoraStr));
                    }
                    if (metodoPago != null && !metodoPago.isEmpty()) {
                        reserva.setMetodoPago(MetodoPago.valueOf(metodoPago));
                    }
                    if (fechaStr != null && !fechaStr.isEmpty()) {
                        reserva.setFecha(Date.valueOf(fechaStr));
                    }
                    if (horaInicioStr != null && !horaInicioStr.isEmpty()) {
                        Time horaInicio = Time.valueOf(horaInicioStr + ":00");
                        reserva.setHoraInicio(horaInicio);
                    }
                    if (horaFinStr != null && !horaFinStr.isEmpty()) {
                        reserva.setHoraFin(Time.valueOf(LocalTime.parse(horaFinStr)));
                    }

                    // La duracion ya no se recibe: se deduce del rango hora_inicio -> hora_fin,
                    // asi que recalculamos el total ante cualquier cambio de horario.
                    if ((horaInicioStr != null && !horaInicioStr.isEmpty())
                            || (horaFinStr != null && !horaFinStr.isEmpty())) {

                        long minutosReserva = java.time.Duration.between(
                                reserva.getHoraInicio().toLocalTime(),
                                reserva.getHoraFin().toLocalTime()).toMinutes();

                        if (minutosReserva <= 0) {
                            jsonResponse.addProperty("success", false);
                            jsonResponse.addProperty("message",
                                    "La hora de fin debe ser posterior a la hora de inicio");
                            out.print(jsonResponse.toString());
                            return;
                        }

                        double total = (minutosReserva / 60.0) * reserva.getPrecioHora();
                        reserva.setTotal(total);
                        reserva.setFaltaPagar(total - reserva.getMontoAdelanto());
                    }
                    if (montoAdelantoStr != null && !montoAdelantoStr.isEmpty()) {
                        double montoAdelanto = Double.parseDouble(montoAdelantoStr);
                        reserva.setMontoAdelanto(montoAdelanto);
                        reserva.setFaltaPagar(reserva.getTotal() - montoAdelanto);
                    }
                    if (estadoPagoStr != null && !estadoPagoStr.isEmpty()) {
                        reserva.setEstadoPago(EstadoPago.valueOf(estadoPagoStr));
                    }
                    if (estadoReservaStr != null && !estadoReservaStr.isEmpty()) {
                        reserva.setEstadoReserva(EstadoReserva.valueOf(estadoReservaStr));
                    }

                    boolean resultado = rDao.update(reserva);

                    jsonResponse.addProperty("success", resultado);
                    jsonResponse.addProperty("message", resultado ? "Reserva actualizada correctamente" : "Error al actualizar reserva");
                    if (resultado) {
                        jsonResponse.add("data", gson.toJsonTree(reserva));
                        AuditoriaHelper.registrar(request, "UPDATE", "reservacancha", id,
                                "Actualización de reserva id " + id);
                    }
                } catch (NumberFormatException e) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Error en formato de números");
                }
                out.print(jsonResponse.toString());

            } else if ("cambiarEstado".equals(action)) {
                String idParam = request.getParameter("id");
                String estadoReservaStr = request.getParameter("estadoReserva");

                if (idParam == null || idParam.isEmpty() || estadoReservaStr == null || estadoReservaStr.isEmpty()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "ID y estadoReserva son requeridos");
                    out.print(jsonResponse.toString());
                    return;
                }

                int id = Integer.parseInt(idParam);
                ReservaCancha reserva = rDao.SearchById(id);

                if (reserva == null) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Reserva no encontrada");
                    out.print(jsonResponse.toString());
                    return;
                }

                reserva.setEstadoReserva(EstadoReserva.valueOf(estadoReservaStr));
                boolean resultado = rDao.update(reserva);

                jsonResponse.addProperty("success", resultado);
                jsonResponse.addProperty("message", resultado ? "Estado de la reserva actualizado correctamente" : "Error al actualizar estado");
                if (resultado) {
                    jsonResponse.addProperty("estadoReserva", reserva.getEstadoReserva().name());
                    jsonResponse.add("data", gson.toJsonTree(reserva));
                    AuditoriaHelper.registrar(request, "UPDATE", "reservacancha", id,
                            "Cambio de estado de reserva a " + reserva.getEstadoReserva().name());
                }
                out.print(jsonResponse.toString());

            } else if ("cambiarEstadoPago".equals(action)) {
                String idParam = request.getParameter("id");
                String estadoPagoStr = request.getParameter("estadoPago");

                if (idParam == null || idParam.isEmpty() || estadoPagoStr == null || estadoPagoStr.isEmpty()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "ID y estadoPago son requeridos");
                    out.print(jsonResponse.toString());
                    return;
                }

                int id = Integer.parseInt(idParam);
                ReservaCancha reserva = rDao.SearchById(id);

                if (reserva == null) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Reserva no encontrada");
                    out.print(jsonResponse.toString());
                    return;
                }

                reserva.setEstadoPago(EstadoPago.valueOf(estadoPagoStr));

                // Si se marca como PAGADO, actualizar faltaPagar a 0
                if (estadoPagoStr.equals("PAGADO")) {
                    reserva.setFaltaPagar(0);
                }

                boolean resultado = rDao.update(reserva);

                jsonResponse.addProperty("success", resultado);
                jsonResponse.addProperty("message", resultado ? "Estado de pago actualizado correctamente" : "Error al actualizar estado de pago");
                if (resultado) {
                    jsonResponse.addProperty("estadoPago", reserva.getEstadoPago().name());
                    jsonResponse.addProperty("faltaPagar", reserva.getFaltaPagar());
                    jsonResponse.add("data", gson.toJsonTree(reserva));
                    AuditoriaHelper.registrar(request, "UPDATE", "reservacancha", id,
                            "Cambio de estado de pago a " + reserva.getEstadoPago().name());
                }
                out.print(jsonResponse.toString());

            } else if ("eliminar".equals(action)) {
                String idParam = request.getParameter("id");

                if (idParam == null || idParam.isEmpty()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "ID de la reserva requerido");
                    out.print(jsonResponse.toString());
                    return;
                }

                int id = Integer.parseInt(idParam);
                boolean resultado = rDao.delete(id);

                jsonResponse.addProperty("success", resultado);
                jsonResponse.addProperty("message", resultado ? "Reserva eliminada correctamente" : "Error al eliminar reserva");
                if (resultado) {
                    AuditoriaHelper.registrar(request, "DELETE", "reservacancha", id, "Eliminación de reserva id " + id);
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
}
