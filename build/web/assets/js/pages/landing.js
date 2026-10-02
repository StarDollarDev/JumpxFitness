let landingPlans = [];
let carouselIndex = 0;
let carouselVisible = 3;
let leadModal;

const $ = (selector) => document.querySelector(selector);

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function updateVisibleCount() {
    carouselVisible = window.innerWidth <= 991 ? 1 : 3;
    const max = Math.max(0, landingPlans.length - carouselVisible);
    carouselIndex = Math.min(carouselIndex, max);
    renderCarouselPosition();
}

function buildBenefits(plan) {
    const people = Number(plan.cantidadPersonas || 1);
    const days = Number(plan.diasVigencia || 1);
    const benefits = [
        `${people} persona${people === 1 ? '' : 's'} incluida${people === 1 ? '' : 's'}`,
        `Vigencia de ${days} día${days === 1 ? '' : 's'}`,
        'Acceso a sesiones de Jumping'
    ];
    if (days >= 30) benefits.push('Ideal para mantener una rutina constante');
    return benefits;
}

function renderPlans() {
    const status = $('#planes-status');
    const carousel = $('#planes-carousel');
    const track = $('#planes-track');

    if (!landingPlans.length) {
        status.textContent = 'Actualmente no hay planes publicados. Escríbenos para conocer las próximas opciones.';
        carousel.classList.add('d-none');
        return;
    }

    status.classList.add('d-none');
    carousel.classList.remove('d-none');
    track.innerHTML = landingPlans.map(plan => {
        const image = plan.imagen ? escapeHtml(plan.imagen) : 'assets/img/logo.png';
        const benefits = buildBenefits(plan).map(item => `<li><i class="bi bi-check-circle-fill"></i><span>${escapeHtml(item)}</span></li>`).join('');
        return `
            <article class="plan-card-public">
                <img class="plan-image" src="${image}" alt="Plan ${escapeHtml(plan.nombre)}" loading="lazy">
                <div class="plan-body">
                    <span class="plan-meta">${Number(plan.cantidadPersonas || 1)} persona${Number(plan.cantidadPersonas || 1) === 1 ? '' : 's'}</span>
                    <h3>${escapeHtml(plan.nombre)}</h3>
                    <div class="plan-price">S/ ${Number(plan.precio || 0).toFixed(2)} <small>/ plan</small></div>
                    <ul class="plan-benefits">${benefits}</ul>
                    <button type="button" class="btn btn-jx-primary rounded-pill w-100" data-plan-id="${Number(plan.id_plan)}">
                        Saber más <i class="bi bi-arrow-right ms-1"></i>
                    </button>
                </div>
            </article>`;
    }).join('');

    track.querySelectorAll('[data-plan-id]').forEach(button => {
        button.addEventListener('click', () => abrirLead(Number(button.dataset.planId)));
    });

    renderDots();
    updateVisibleCount();
}

function renderDots() {
    const dots = $('#planes-dots');
    const pages = Math.max(1, landingPlans.length - carouselVisible + 1);
    dots.innerHTML = Array.from({length: pages}, (_, index) =>
        `<button class="carousel-dot ${index === carouselIndex ? 'active' : ''}" type="button" aria-label="Ir al grupo ${index + 1}" data-carousel-index="${index}"></button>`
    ).join('');
    dots.querySelectorAll('[data-carousel-index]').forEach(dot => {
        dot.addEventListener('click', () => {
            carouselIndex = Number(dot.dataset.carouselIndex);
            renderCarouselPosition();
        });
    });
}

function renderCarouselPosition() {
    const track = $('#planes-track');
    if (!track || !track.children.length) return;
    const first = track.children[0];
    const gap = parseFloat(getComputedStyle(track).gap) || 20;
    const cardWidth = first.getBoundingClientRect().width;
    track.style.transform = `translateX(-${carouselIndex * (cardWidth + gap)}px)`;

    const max = Math.max(0, landingPlans.length - carouselVisible);
    $('#planes-prev').disabled = carouselIndex <= 0;
    $('#planes-next').disabled = carouselIndex >= max;

    document.querySelectorAll('.carousel-dot').forEach((dot, i) => dot.classList.toggle('active', i === carouselIndex));
}

function moveCarousel(direction) {
    const max = Math.max(0, landingPlans.length - carouselVisible);
    carouselIndex = Math.max(0, Math.min(max, carouselIndex + direction));
    renderCarouselPosition();
}

function abrirLead(idPlan = null) {
    const plan = landingPlans.find(p => Number(p.id_plan) === Number(idPlan));
    $('#lead-id-plan').value = plan ? plan.id_plan : '';
    $('#lead-plan-nombre').value = plan ? plan.nombre : '';
    $('#selected-plan').textContent = plan ? `Plan seleccionado: ${plan.nombre}` : 'Consulta general sobre JumpxFitness';
    $('#lead-feedback').className = 'alert d-none mb-0';
    $('#lead-feedback').textContent = '';
    $('#lead-form').classList.remove('was-validated');
    leadModal.show();
}

async function cargarPlanesLanding() {
    try {
        const response = await fetch('PlanJumpingController?action=listarActivos', { cache: 'no-store' });
        if (!response.ok) throw new Error('No se pudieron cargar los planes');
        const result = await response.json();
        if (!result.success) throw new Error(result.message || 'No se pudieron cargar los planes');
        landingPlans = Array.isArray(result.data) ? result.data : [];
        renderPlans();
    } catch (error) {
        console.error(error);
        $('#planes-status').textContent = 'No pudimos cargar los planes en este momento. Puedes dejarnos tus datos para recibir información.';
        $('#planes-carousel').classList.add('d-none');
    }
}

async function enviarLead(event) {
    event.preventDefault();
    const form = $('#lead-form');
    form.classList.add('was-validated');
    if (!form.checkValidity()) return;

    const button = $('#lead-submit');
    const feedback = $('#lead-feedback');
    button.disabled = true;
    button.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Enviando...';
    feedback.className = 'alert d-none mb-0';

    const payload = {
        nombreCompleto: $('#lead-nombre').value.trim(),
        correo: $('#lead-correo').value.trim(),
        telefono: $('#lead-telefono').value.trim(),
        idPlan: $('#lead-id-plan').value ? Number($('#lead-id-plan').value) : null,
        planNombre: $('#lead-plan-nombre').value.trim()
    };

    try {
        const response = await fetch('LeadController', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: JSON.stringify(payload)
        });
        const result = await response.json();
        if (!response.ok || !result.success) throw new Error(result.message || 'No se pudo enviar el formulario');

        feedback.className = 'alert alert-success mb-0';
        feedback.textContent = result.message;
        form.reset();
        form.classList.remove('was-validated');
        $('#lead-id-plan').value = payload.idPlan ?? '';
        $('#lead-plan-nombre').value = payload.planNombre;
        setTimeout(() => leadModal.hide(), 1600);
    } catch (error) {
        feedback.className = 'alert alert-danger mb-0';
        feedback.textContent = error.message || 'Ocurrió un error. Inténtalo nuevamente.';
    } finally {
        button.disabled = false;
        button.innerHTML = 'Enviar solicitud';
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    leadModal = bootstrap.Modal.getOrCreateInstance($('#leadModal'));
    $('#lead-form').addEventListener('submit', enviarLead);
    $('#planes-prev').addEventListener('click', () => moveCarousel(-1));
    $('#planes-next').addEventListener('click', () => moveCarousel(1));
    $('#contact-main-cta').addEventListener('click', () => abrirLead());
    await cargarPlanesLanding();
    await cargarBannerSabados();
    await cargarGaleriaCancha();
    await verificarSesionPublica();
    window.addEventListener('resize', () => {
        const oldVisible = carouselVisible;
        updateVisibleCount();
        if (oldVisible !== carouselVisible) renderDots();
    });
});

/* ============================================================
   Banner de eventos de sábados
   ============================================================ */
async function cargarBannerSabados() {
    try {
        const res = await fetch('BannerEventoController?action=activo');
        const r = await res.json();
        const wrap = $('#banner-sabados-wrap');
        const cont = $('#banner-sabados');

        if (!r.success || !r.hayBanner) {
            wrap.classList.add('d-none');
            return;
        }
        const b = r.data;

        // Primero mostramos el banner con su degradado. Solo usamos la imagen
        // si el archivo realmente existe; así una ruta rota no deja el banner gris.
        cont.innerHTML = `
            <div class="banner-sabados">
                <div class="container banner-sabados-contenido">
                    <div>
                        <span class="banner-sabados-eyebrow"><i class="bi bi-stars me-1"></i>Solo los sábados</span>
                        <h3>${escapeHtml(b.titulo)}</h3>
                        ${b.descripcion ? `<p>${escapeHtml(b.descripcion)}</p>` : ''}
                    </div>
                    ${b.horaTexto ? `<div class="fs-5 fw-bold text-nowrap"><i class="bi bi-clock me-2"></i>${escapeHtml(b.horaTexto)}</div>` : ''}
                </div>
            </div>
        `;

        if (b.imagenFondo) {
            const imagen = new Image();
            imagen.onload = () => {
                const banner = $('#banner-sabados .banner-sabados');
                if (banner) banner.style.backgroundImage = 'url(\"' + b.imagenFondo + '\")';
            };
            // Si falla, no hacemos nada: queda visible el degradado original.
            imagen.src = b.imagenFondo;
        }

        wrap.classList.remove('d-none');
    } catch (e) {
        console.error('No se pudo cargar el banner de sábados:', e);
    }
}

/* ============================================================
   Galería de cancha (fotos subidas + videos embebidos)
   ============================================================ */
let canchaLightboxModal;

function urlVideoEmbebido(url) {
    // Convierte links normales de YouTube a su versión /embed/ para el iframe.
    const yt = url.match(/(?:youtu\.be\/|youtube\.com\/(?:watch\?v=|embed\/))([\w-]{6,})/);
    if (yt) return `https://www.youtube.com/embed/${yt[1]}`;
    return url; // Facebook u otros: se usa tal cual (debe ser ya un link "embebible")
}

async function cargarGaleriaCancha() {
    const status = $('#cancha-galeria-status');
    const grid = $('#cancha-galeria');
    try {
        const res = await fetch('GaleriaController?action=listar');
        const r = await res.json();
        const items = r.data || [];

        if (!r.success || items.length === 0) {
            status.textContent = 'Pronto subiremos fotos y videos de la cancha.';
            return;
        }

        grid.innerHTML = items.map((item, i) => {
            if (item.tipo === 'VIDEO') {
                return `
                    <div class="cancha-item" onclick="abrirCanchaLightbox(${i})" data-tipo="video" data-url="${escapeHtml(item.url)}">
                        <div class="cancha-play-badge"><i class="bi bi-play-circle-fill"></i></div>
                        ${item.titulo ? `<div class="cancha-item-titulo">${escapeHtml(item.titulo)}</div>` : ''}
                    </div>`;
            }
            return `
                <div class="cancha-item" onclick="abrirCanchaLightbox(${i})" data-tipo="foto" data-url="${escapeHtml(item.url)}">
                    <img src="${escapeHtml(item.url)}" alt="${escapeHtml(item.titulo || 'Foto de la cancha')}" loading="lazy" onerror="this.onerror=null; this.src='assets/img/logo.png';">
                    ${item.titulo ? `<div class="cancha-item-titulo">${escapeHtml(item.titulo)}</div>` : ''}
                </div>`;
        }).join('');

        window.__canchaItems = items;
        status.classList.add('d-none');
        grid.classList.remove('d-none');
    } catch (e) {
        console.error('No se pudo cargar la galería de cancha:', e);
        status.textContent = 'No se pudo cargar la galería.';
    }
}

function abrirCanchaLightbox(indice) {
    const item = (window.__canchaItems || [])[indice];
    if (!item) return;
    const body = $('#cancha-lightbox-body');

    body.innerHTML = item.tipo === 'VIDEO'
        ? `<div class="ratio ratio-16x9"><iframe src="${escapeHtml(urlVideoEmbebido(item.url))}" allowfullscreen allow="autoplay; encrypted-media"></iframe></div>`
        : `<img src="${escapeHtml(item.url)}" class="w-100 rounded-3" alt="${escapeHtml(item.titulo || '')}" onerror="this.onerror=null; this.src='assets/img/logo.png';">`;

    canchaLightboxModal = canchaLightboxModal || bootstrap.Modal.getOrCreateInstance($('#cancha-lightbox-modal'));
    canchaLightboxModal.show();

    // Al cerrar, se quita el iframe/imagen para que un video no siga sonando de fondo.
    $('#cancha-lightbox-modal').addEventListener('hidden.bs.modal', () => { body.innerHTML = ''; }, { once: true });
}

/* ============================================================
   Navbar público consciente de la sesión: si hay una sesión activa
   (de admin o de cliente, incluida la persistente de 7 días de un
   cliente que ya cerró el navegador), reemplaza "Iniciar Sesión" por
   el nombre de la cuenta y un acceso directo a su panel.
   ============================================================ */
const JX_HOME_POR_ROL_PUBLICO = { ADMIN: 'admin_dashboard.html', CLIENTE: 'cliente_dashboard.html' };

async function verificarSesionPublica() {
    try {
        const res = await fetch('AuthController?action=verificar');
        const r = await res.json();
        if (!r.success || !r.logueado) return; // se queda con "Iniciar Sesión" (comportamiento por defecto)

        $('#nav-auth-guest').classList.add('d-none');
        $('#nav-auth-user').classList.remove('d-none');
        $('#nav-auth-nombre').textContent = r.nombreCompleto || r.usuario;

        const panel = $('#nav-auth-panel');
        panel.href = JX_HOME_POR_ROL_PUBLICO[r.rol] || 'index.html';

        $('#nav-auth-logout').addEventListener('click', async (e) => {
            e.preventDefault();
            await fetch('AuthController', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: 'action=logout'
            });
            window.location.reload();
        });
    } catch (e) {
        console.error('No se pudo verificar la sesión:', e);
    }
}
