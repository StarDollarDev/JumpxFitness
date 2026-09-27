function jxMoneySafe(n) {
    return n === null || n === undefined ? '-' : jxMoney(n);
}

function badgeVigencia(fila) {
    if (!fila.tienePlan) return '<span class="badge bg-secondary">Sin plan</span>';
    if (fila.vencido) return '<span class="badge bg-danger">Vencido</span>';
    if (fila.diasRestantes <= 5) return `<span class="badge bg-warning text-dark">${fila.diasRestantes} día(s)</span>`;
    return `<span class="badge bg-success">${fila.diasRestantes} día(s)</span>`;
}

async function cargarTablaClientes() {
    const r = await jxApi('ClienteController', 'GET', { action: 'listarConPlan' });
    const clientes = r.data || [];
    const tbody = document.querySelector('#tabla-clientes tbody');

    tbody.innerHTML = clientes.map(c => `
        <tr>
            <td>${c.persona ? c.persona.nombre + ' ' + c.persona.apellido : '-'}</td>
            <td>${c.persona ? (c.persona.documento + ' ' + c.persona.numeroDoc) : '-'}</td>
            <td>${c.persona ? (c.persona.telefono || '-') : '-'}</td>
            <td>${c.tieneCuenta ? `<span class="badge bg-info text-dark">${c.usuario}</span>` : '<span class="badge bg-light text-dark border">Sin cuenta</span>'}</td>
            <td>${c.tienePlan ? c.nombrePlan : '-'}</td>
            <td>${c.tienePlan ? jxMoneySafe(c.montoPlan) : '-'}</td>
            <td>${badgeVigencia(c)}</td>
        </tr>
    `).join('');

    if ($.fn.DataTable.isDataTable('#tabla-clientes')) {
        $('#tabla-clientes').DataTable().destroy();
    }
    $('#tabla-clientes').DataTable({ language: { url: 'https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json' } });
}

async function jxPageInit() {
    if (!jxRequireLogin()) return; // por defecto exige rol ADMIN
    await cargarTablaClientes();
}
