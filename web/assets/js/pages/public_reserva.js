/* ============================================================
   Reserva pública de cancha
   No requiere sesión ni cuenta.
   ============================================================ */

function inicializarReservaPublica() {
    const botonAbrir = document.getElementById('btn-reservar-cancha');
    const formulario = document.getElementById('form-reserva-publica');

    if (!botonAbrir || !formulario || botonAbrir.dataset.bound) return;

    botonAbrir.dataset.bound = '1';

    const modalElement = document.getElementById('modalReservaPublica');
    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
    const fecha = document.getElementById('public-reserva-fecha');
    const error = document.getElementById('public-reserva-error');

    function hoyLocal() {
        const d = new Date();
        const offset = d.getTimezoneOffset() * 60000;
        return new Date(d.getTime() - offset).toISOString().slice(0, 10);
    }

    botonAbrir.addEventListener('click', () => {
        formulario.reset();
        error.classList.add('d-none');
        fecha.min = hoyLocal();
        fecha.value = hoyLocal();
        modal.show();
    });

    formulario.addEventListener('submit', async (e) => {
        e.preventDefault();

        const boton = document.getElementById('btn-enviar-reserva-publica');
        const original = boton.innerHTML;
        error.classList.add('d-none');

        const minutos = Number(document.getElementById('public-reserva-duracion').value);
        const horaInicio = document.getElementById('public-reserva-hora').value;

        if (!horaInicio) {
            error.textContent = 'Selecciona la hora de inicio.';
            error.classList.remove('d-none');
            return;
        }

        // Calculamos la hora final en el navegador para mantener el formulario simple.
        const partes = horaInicio.split(':').map(Number);
        const totalMinutos = partes[0] * 60 + partes[1] + minutos;

        if (totalMinutos >= 24 * 60) {
            error.textContent = 'La reserva debe terminar antes de las 00:00.';
            error.classList.remove('d-none');
            return;
        }

        const horasFin = String(Math.floor(totalMinutos / 60)).padStart(2, '0');
        const minutosFin = String(totalMinutos % 60).padStart(2, '0');
        const horaFin = `${horasFin}:${minutosFin}`;

        try {
            boton.disabled = true;
            boton.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Enviando...';

            const respuesta = await fetch('ReservaCanchaController', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({
                action: 'solicitarPublica',
                nombre: document.getElementById('public-reserva-nombre').value.trim(),
                telefono: document.getElementById('public-reserva-telefono').value.trim(),
                deporte: document.getElementById('public-reserva-deporte').value,
                fecha: fecha.value,
                horaInicio,
                horaFin
                }).toString()
            });

            const r = await respuesta.json();

            if (r.success) {
                modal.hide();
                if (window.Swal) {
                    await Swal.fire({
                        icon: 'success',
                        title: '¡Solicitud enviada!',
                        text: 'Recibimos tu solicitud. El administrador revisará la disponibilidad y confirmará la reserva.',
                        confirmButtonText: 'Entendido'
                    });
                } else {
                    alert('¡Solicitud enviada! El administrador revisará la disponibilidad y confirmará la reserva.');
                }
            } else {
                error.textContent = r.message || 'No se pudo enviar la solicitud.';
                error.classList.remove('d-none');
            }
        } catch (err) {
            console.error('Error en reserva pública:', err);
            error.textContent = 'Ocurrió un error inesperado. Intenta nuevamente.';
            error.classList.remove('d-none');
        } finally {
            boton.disabled = false;
            boton.innerHTML = original;
        }
    });
}

document.addEventListener('DOMContentLoaded', inicializarReservaPublica);
