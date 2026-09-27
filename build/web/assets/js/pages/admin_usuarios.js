let usuariosCache = [];

        async function jxPageInit() {
            if (!jxRequireLogin()) return;
            await cargarTablaUsuarios();
            document.getElementById('form-usuario').addEventListener('submit', guardarUsuario);
        }

        function abrirModalNuevo() {
            document.getElementById('form-usuario').reset();
            document.getElementById('form-usuario').dataset.id = '';
            document.getElementById('u_password').required = true;
            document.getElementById('u_password').placeholder = '';
            bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).show();
        }

        async function guardarUsuario(e) {
            e.preventDefault();
            const form = document.getElementById('form-usuario');
            const payload = {
                nombre: document.getElementById('u_nombre').value.trim(),
                apellido: document.getElementById('u_apellido').value.trim(),
                documento: document.getElementById('u_documento').value,
                numeroDoc: document.getElementById('u_numeroDoc').value.trim(),
                telefono: document.getElementById('u_telefono').value.trim(),
                usuario: document.getElementById('u_usuario').value.trim(),
                password: document.getElementById('u_password').value,
                rol: 'ADMIN'
            };
            const r = await jxApi('UsuarioController', 'POST', { action: 'insertar', ...payload });
            if (r.success) {
                bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).hide();
                jxToast('success', 'Administrador creado');
                cargarTablaUsuarios();
            } else {
                jxToast('error', r.message || 'No se pudo guardar');
            }
        }

        async function eliminarUsuario(id) {
            const conf = await Swal.fire({ title: '¿Eliminar este administrador?', icon: 'warning', showCancelButton: true, confirmButtonText: 'Sí, eliminar' });
            if (!conf.isConfirmed) return;
            const r = await jxApi('UsuarioController', 'POST', { action: 'eliminar', id });
            if (r.success) { jxToast('success', 'Administrador eliminado'); cargarTablaUsuarios(); }
            else jxToast('error', r.message || 'No se pudo eliminar');
        }

        async function cargarTablaUsuarios() {
            const r = await jxApi('UsuarioController', 'GET', { action: 'listarPorRol', rol: 'ADMIN' });
            usuariosCache = r.data || [];
            const tbody = document.querySelector('#tabla-usuarios tbody');
            tbody.innerHTML = usuariosCache.map(u => `
                <tr>
                    <td>${u.usuario}</td>
                    <td>${u.persona ? u.persona.nombre + ' ' + u.persona.apellido : '-'}</td>
                    <td>${u.persona ? u.persona.numeroDoc : '-'}</td>
                    <td>${u.persona ? (u.persona.telefono || '-') : '-'}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-danger" onclick="eliminarUsuario(${u.id_usuario})"><i class="bi bi-trash"></i></button>
                    </td>
                </tr>
            `).join('');
            $('#tabla-usuarios').DataTable({ destroy: true, language: { url: 'https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json' } });
        }
