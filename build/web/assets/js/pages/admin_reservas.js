const ESTADOS_RESERVA = ['PENDIENTE','CONFIRMADO','PAGADA','EN_CURSO','FINALIZADA','CANCELADA'];
let reservasConocidas = null;
let intervaloNuevasReservas = null;
let audioNotificacion = null;

        async function jxPageInit() {
            if (!jxRequireLogin('ADMIN')) return;
            document.getElementById('filtro_fecha').value = new Date().toISOString().slice(0, 10);
            document.getElementById('filtro_fecha').addEventListener('change', () => { reservasConocidas = null; cargarTabla(true); });
            document.getElementById('form-reserva').addEventListener('submit', guardarReserva);
            await cargarTabla(true);
            iniciarMonitorReservas();
        }

        let rsIdCliente = null;

        function abrirModalReserva() {
            document.getElementById('form-reserva').reset();
            rsIdCliente = null;
            document.getElementById('rs_cliente_info').classList.add('d-none');
            document.getElementById('rs_cliente_nuevo').classList.add('d-none');
            document.getElementById('rs_fecha').value = document.getElementById('filtro_fecha').value;
            bootstrap.Modal.getOrCreateInstance(document.getElementById('modalReserva')).show();
        }

        async function buscarClienteReserva() {
            const documento = document.getElementById('rs_doc_tipo').value;
            const numeroDoc = document.getElementById('rs_doc_num').value.trim();
            if (!numeroDoc) { jxToast('error', 'Ingresa el número de documento'); return; }

            const r = await jxApi('ClienteController', 'GET', { action: 'buscarDocumento', documento, numeroDoc });
            const info = document.getElementById('rs_cliente_info');
            const nuevo = document.getElementById('rs_cliente_nuevo');
            if (r.success && r.data) {
                rsIdCliente = r.data.id_cliente;
                info.textContent = 'Cliente encontrado: ' + r.data.persona.nombre + ' ' + r.data.persona.apellido;
                info.classList.remove('d-none');
                nuevo.classList.add('d-none');
            } else {
                rsIdCliente = null;
                info.classList.add('d-none');
                nuevo.classList.remove('d-none');
            }
        }

        async function guardarReserva(e) {
            e.preventDefault();

            const payload = {
                action: 'insertar',
                deporte: document.getElementById('rs_deporte').value,
                fecha: document.getElementById('rs_fecha').value,
                horaInicio: document.getElementById('rs_hora_inicio').value,
                horaFin: document.getElementById('rs_hora_fin').value,
                precioHora: document.getElementById('rs_precio_hora').value,
                metodoPago: document.getElementById('rs_metodo').value,
                montoAdelanto: document.getElementById('rs_adelanto').value || 0,
                estadoPago: document.getElementById('rs_estado_pago').value
            };

            if (rsIdCliente) {
                payload.idCliente = rsIdCliente;
            } else {
                const nombre = document.getElementById('rs_nombre').value.trim();
                const apellido = document.getElementById('rs_apellido').value.trim();
                const numeroDoc = document.getElementById('rs_doc_num').value.trim();
                if (!nombre || !apellido || !numeroDoc) {
                    jxToast('error', 'Busca un cliente o completa nombre, apellido y documento');
                    return;
                }
                payload.nombre = nombre;
                payload.apellido = apellido;
                payload.documento = document.getElementById('rs_doc_tipo').value;
                payload.numeroDoc = numeroDoc;
                payload.telefono = document.getElementById('rs_telefono').value.trim();
            }

            const r = await jxApi('ReservaCanchaController', 'POST', payload);
            if (r.success) {
                bootstrap.Modal.getOrCreateInstance(document.getElementById('modalReserva')).hide();
                jxToast('success', 'Reserva registrada');
                document.getElementById('filtro_fecha').value = payload.fecha;
                cargarTabla(true);
            } else {
                jxToast('error', r.message || 'No se pudo registrar la reserva');
            }
        }

        function nombreCliente(res) {
            if (!res.cliente || !res.cliente.persona) return '—';
            return `${res.cliente.persona.nombre} ${res.cliente.persona.apellido}`;
        }

        async function togglePago(id, actual) {
            const nuevo = actual === 'PAGADO' ? 'PENDIENTE' : 'PAGADO';
            const r = await jxApi('ReservaCanchaController', 'POST', { action: 'cambiarEstadoPago', id, estadoPago: nuevo });
            if (r.success) { jxToast('success', 'Estado de pago actualizado'); cargarTabla(true); }
            else jxToast('error', r.message || 'Error al actualizar pago');
        }

        async function cambiarEstado(id, nuevoEstado) {
            const r = await jxApi('ReservaCanchaController', 'POST', { action: 'cambiarEstado', id, estadoReserva: nuevoEstado });
            if (r.success) { jxToast('success', 'Estado actualizado'); cargarTabla(true); }
            else jxToast('error', r.message || 'Error al actualizar estado');
        }

        async function eliminarReserva(id) {
            const conf = await Swal.fire({ title: '¿Eliminar esta reserva?', icon: 'warning', showCancelButton: true, confirmButtonText: 'Sí, eliminar' });
            if (!conf.isConfirmed) return;
            const r = await jxApi('ReservaCanchaController', 'POST', { action: 'eliminar', id });
            if (r.success) { jxToast('success', 'Reserva eliminada'); cargarTabla(true); }
            else jxToast('error', r.message || 'No se pudo eliminar');
        }

        async function cargarTabla(inicial = false) {
            const fecha = document.getElementById('filtro_fecha').value;
            const r = await jxApi('ReservaCanchaController', 'GET', { action: 'listarPorFecha', fecha });
            const tbody = document.querySelector('#tabla-reservas tbody');
            const data = r.data || [];

            // La primera carga solo establece la referencia.
            // Las cargas siguientes detectan IDs que antes no existían.
            if (reservasConocidas === null || inicial) {
                reservasConocidas = new Set(data.map(res => Number(res.id_reserva)));
            } else {
                const nuevas = data.filter(res => !reservasConocidas.has(Number(res.id_reserva)));
                nuevas.forEach(res => {
                    reservasConocidas.add(Number(res.id_reserva));
                    notificarNuevaReserva(res);
                });
            }

            tbody.innerHTML = data.map(res => `
                <tr>
                    <td>${nombreCliente(res)}</td>
                    <td>${res.deporte}</td>
                    <td>${(res.horaInicio||'').slice(0,5)} - ${(res.horaFin||'').slice(0,5)}</td>
                    <td>${jxMoney(res.total)}</td>
                    <td>${jxMoney(res.faltaPagar)}</td>
                    <td>
                        <button class="btn btn-sm ${res.estadoPago === 'PAGADO' ? 'btn-success' : 'btn-warning'}"
                                onclick="togglePago(${res.id_reserva}, '${res.estadoPago}')">${res.estadoPago}</button>
                    </td>
                    <td>
                        <select class="form-select form-select-sm" onchange="cambiarEstado(${res.id_reserva}, this.value)">
                            ${ESTADOS_RESERVA.map(e => `<option value="${e}" ${e === res.estadoReserva ? 'selected' : ''}>${e}</option>`).join('')}
                        </select>
                    </td>
                    <td><button class="btn btn-sm btn-outline-danger" onclick="eliminarReserva(${res.id_reserva})"><i class="bi bi-trash"></i></button></td>
                </tr>
            `).join('');
            $('#tabla-reservas').DataTable({ destroy: true, language: { url: 'https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json' } });
        }


/* ============================================================
   Monitor de nuevas solicitudes públicas
   ============================================================ */

function iniciarMonitorReservas() {
    if (intervaloNuevasReservas) clearInterval(intervaloNuevasReservas);

    intervaloNuevasReservas = setInterval(async () => {
        try {
            await cargarTabla(false);
        } catch (e) {
            console.error('No se pudo comprobar nuevas reservas:', e);
        }
    }, 5000);
}

function reproducirSonidoReserva() {
    try {
        if (!audioNotificacion) {
            audioNotificacion = new (window.AudioContext || window.webkitAudioContext)();
        }

        const ctx = audioNotificacion;
        if (ctx.state === 'suspended') {
            ctx.resume();
        }

        const ahora = ctx.currentTime;
        const oscilador = ctx.createOscillator();
        const ganancia = ctx.createGain();

        oscilador.type = 'sine';
        oscilador.frequency.setValueAtTime(880, ahora);
        oscilador.frequency.setValueAtTime(1174.66, ahora + 0.10);

        ganancia.gain.setValueAtTime(0.0001, ahora);
        ganancia.gain.exponentialRampToValueAtTime(0.16, ahora + 0.02);
        ganancia.gain.exponentialRampToValueAtTime(0.0001, ahora + 0.30);

        oscilador.connect(ganancia);
        ganancia.connect(ctx.destination);

        oscilador.start(ahora);
        oscilador.stop(ahora + 0.32);
    } catch (e) {
        console.warn('El navegador no permitió reproducir el sonido:', e);
    }
}

function notificarNuevaReserva(res) {
    const cliente = nombreCliente(res);
    const fecha = res.fecha || '';
    const horaInicio = (res.horaInicio || '').slice(0, 5);
    const horaFin = (res.horaFin || '').slice(0, 5);

    reproducirSonidoReserva();

    Swal.fire({
        icon: 'info',
        title: '¡Nueva solicitud de cancha!',
        html: `<strong>${cliente}</strong><br>
               ${res.deporte || ''} · ${fecha}<br>
               ${horaInicio} - ${horaFin}`,
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 5500,
        timerProgressBar: true
    });
}
