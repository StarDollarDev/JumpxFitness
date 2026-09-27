const $ = (sel) => document.querySelector(sel);

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/** Como jxApi pero para formularios con archivos (multipart/form-data). */
async function jxApiForm(controller, formData) {
    try {
        const res = await fetch(controller, { method: 'POST', body: formData });
        const texto = await res.text();
        try { return JSON.parse(texto); }
        catch (e) {
            console.error('Respuesta no-JSON de ' + controller + ':', texto);
            return { success: false, message: 'El servidor respondió con un error inesperado.' };
        }
    } catch (e) {
        console.error('Error de red llamando a ' + controller + ':', e);
        return { success: false, message: 'No se pudo conectar con el servidor.' };
    }
}

function mostrarAlerta(id, tipo, mensaje) {
    const el = $(id);
    el.className = 'alert py-2 ' + (tipo === 'success' ? 'alert-success' : 'alert-danger');
    el.textContent = mensaje;
    el.classList.remove('d-none');
}

/* ============================================================
   Tabs simples
   ============================================================ */
function inicializarTabsMultimedia() {
    document.querySelectorAll('#multimedia-tabs button').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#multimedia-tabs button').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.multimedia-pane').forEach(p => p.classList.add('d-none'));
            btn.classList.add('active');
            $('#tab-' + btn.dataset.tab).classList.remove('d-none');
        });
    });
}

/* ============================================================
   Galería de cancha
   ============================================================ */
async function cargarGaleriaAdmin() {
    const r = await jxApi('GaleriaController', 'GET', { action: 'listarAdmin' });
    const items = r.data || [];
    const grid = $('#galeria-admin-grid');

    if (items.length === 0) {
        grid.innerHTML = '<p class="text-muted">Aún no hay fotos ni videos.</p>';
        return;
    }
    grid.innerHTML = items.map(item => `
        <div class="col-6 col-lg-4">
            <div class="item-admin-card">
                ${item.tipo === 'VIDEO'
                    ? `<div class="d-flex align-items-center justify-content-center bg-dark" style="aspect-ratio:4/3;"><i class="bi bi-play-circle-fill text-white fs-1"></i></div>`
                    : `<img src="${escapeHtml(item.url)}" alt="">`}
                <div class="item-admin-tipo">${item.tipo}${item.titulo ? ' · ' + escapeHtml(item.titulo) : ''}</div>
                <button class="btn btn-sm btn-danger item-admin-btn-eliminar" onclick="eliminarItemGaleria(${item.idItem})">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>
    `).join('');
}

async function eliminarItemGaleria(idItem) {
    const conf = await Swal.fire({ icon: 'warning', title: '¿Eliminar este elemento?', showCancelButton: true, confirmButtonText: 'Eliminar' });
    if (!conf.isConfirmed) return;
    const r = await jxApi('GaleriaController', 'POST', { action: 'eliminar', idItem });
    jxToast(r.success ? 'success' : 'error', r.message);
    if (r.success) await cargarGaleriaAdmin();
}

function inicializarFormFoto() {
    $('#foto-archivo').addEventListener('change', (e) => {
        const file = e.target.files[0];
        const preview = $('#foto-preview');
        if (!file) { preview.classList.add('d-none'); return; }
        preview.src = URL.createObjectURL(file);
        preview.classList.remove('d-none');
    });

    $('#form-foto').addEventListener('submit', async (e) => {
        e.preventDefault();
        const boton = $('#foto-submit');
        const original = boton.innerHTML;
        boton.disabled = true;
        boton.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Subiendo...';

        const fd = new FormData();
        fd.append('action', 'subirFoto');
        fd.append('archivo', $('#foto-archivo').files[0]);
        fd.append('titulo', $('#foto-titulo').value.trim());

        const r = await jxApiForm('GaleriaController', fd);
        mostrarAlerta('#foto-alert', r.success ? 'success' : 'error', r.message);
        if (r.success) {
            $('#form-foto').reset();
            $('#foto-preview').classList.add('d-none');
            await cargarGaleriaAdmin();
        }
        boton.disabled = false;
        boton.innerHTML = original;
    });
}

function inicializarFormVideo() {
    $('#form-video').addEventListener('submit', async (e) => {
        e.preventDefault();
        const r = await jxApi('GaleriaController', 'POST', {
            action: 'agregarVideo',
            url: $('#video-url').value.trim(),
            titulo: $('#video-titulo').value.trim()
        });
        mostrarAlerta('#video-alert', r.success ? 'success' : 'error', r.message);
        if (r.success) { $('#form-video').reset(); await cargarGaleriaAdmin(); }
    });
}

/* ============================================================
   Banner de sábados
   ============================================================ */
function renderBannerPreview() {
    const titulo = $('#banner-titulo').value.trim() || 'Título del evento';
    const descripcion = $('#banner-descripcion').value.trim();
    const hora = $('#banner-hora').value.trim();
    const archivo = $('#banner-imagen').files[0];
    const estilo = archivo ? ` style="background-image:url('${URL.createObjectURL(archivo)}')"` : '';

    $('#banner-preview').innerHTML = `
        <div class="banner-sabados" style="border-radius:16px;"${estilo}>
            <div class="banner-sabados-contenido" style="padding:0 20px;">
                <div>
                    <span class="banner-sabados-eyebrow">Solo los sábados</span>
                    <h3>${escapeHtml(titulo)}</h3>
                    ${descripcion ? `<p>${escapeHtml(descripcion)}</p>` : ''}
                </div>
                ${hora ? `<div class="fs-5 fw-bold">${escapeHtml(hora)}</div>` : ''}
            </div>
        </div>
    `;
}

async function cargarBannersAdmin() {
    const r = await jxApi('BannerEventoController', 'GET', { action: 'listar' });
    const banners = r.data || [];
    $('#banner-lista').innerHTML = banners.map(b => `
        <div class="list-group-item d-flex justify-content-between align-items-center">
            <div>
                <strong>${escapeHtml(b.titulo)}</strong>
                ${b.activo ? '<span class="badge bg-success ms-2">Activo</span>' : ''}
            </div>
            <div>
                <button class="btn btn-sm btn-outline-secondary me-1" onclick="editarBanner(${b.idBanner})"><i class="bi bi-pencil"></i></button>
                <button class="btn btn-sm btn-outline-danger" onclick="eliminarBanner(${b.idBanner})"><i class="bi bi-trash"></i></button>
            </div>
        </div>
    `).join('') || '<p class="text-muted">Aún no has creado ningún banner.</p>';

    window.__bannersCache = banners;
}

function editarBanner(idBanner) {
    const b = (window.__bannersCache || []).find(x => x.idBanner === idBanner);
    if (!b) return;
    $('#banner-id').value = b.idBanner;
    $('#banner-titulo').value = b.titulo || '';
    $('#banner-descripcion').value = b.descripcion || '';
    $('#banner-hora').value = b.horaTexto || '';
    $('#banner-activo').checked = !!b.activo;
    $('#banner-nuevo').classList.remove('d-none');
    renderBannerPreview();
}

function inicializarBotonNuevoBanner() {
    $('#banner-nuevo').addEventListener('click', () => {
        $('#form-banner').reset();
        $('#banner-id').value = 0;
        $('#banner-nuevo').classList.add('d-none');
        renderBannerPreview();
    });
}

async function eliminarBanner(idBanner) {
    const conf = await Swal.fire({ icon: 'warning', title: '¿Eliminar este banner?', showCancelButton: true, confirmButtonText: 'Eliminar' });
    if (!conf.isConfirmed) return;
    const r = await jxApi('BannerEventoController', 'POST', { action: 'eliminar', idBanner });
    jxToast(r.success ? 'success' : 'error', r.message);
    if (r.success) await cargarBannersAdmin();
}

function inicializarFormBanner() {
    ['#banner-titulo', '#banner-descripcion', '#banner-hora'].forEach(sel => {
        $(sel).addEventListener('input', renderBannerPreview);
    });
    $('#banner-imagen').addEventListener('change', renderBannerPreview);

    $('#form-banner').addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData();
        fd.append('action', 'guardar');
        fd.append('idBanner', $('#banner-id').value);
        fd.append('titulo', $('#banner-titulo').value.trim());
        fd.append('descripcion', $('#banner-descripcion').value.trim());
        fd.append('horaTexto', $('#banner-hora').value.trim());
        fd.append('activo', $('#banner-activo').checked ? 'true' : 'false');
        if ($('#banner-imagen').files[0]) fd.append('imagen', $('#banner-imagen').files[0]);

        const r = await jxApiForm('BannerEventoController', fd);
        mostrarAlerta('#banner-alert', r.success ? 'success' : 'error', r.message);
        if (r.success) {
            $('#form-banner').reset();
            $('#banner-id').value = 0;
            $('#banner-nuevo').classList.add('d-none');
            renderBannerPreview();
            await cargarBannersAdmin();
        }
    });
}

async function jxPageInit() {
    if (!jxRequireLogin()) return; // por defecto exige rol ADMIN
    inicializarTabsMultimedia();
    inicializarFormFoto();
    inicializarFormVideo();
    inicializarFormBanner();
    inicializarBotonNuevoBanner();
    renderBannerPreview();
    await Promise.all([cargarGaleriaAdmin(), cargarBannersAdmin()]);
}
