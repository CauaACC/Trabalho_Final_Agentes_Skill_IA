function escapeCertificateText(value) {
  return String(value ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function formatCertificateDate(value) {
  return value ? new Date(value).toLocaleString('pt-BR') : '-';
}

function showCertificatePanels() {
  document.getElementById('participant-certificates').hidden = getSelectedUser().startsWith('org-');
}

function renderCertificates(certificates) {
  const list = document.getElementById('certificate-list');
  list.innerHTML = certificates.length
    ? certificates.map(certificado => `
      <article class="certificate-item">
        <div>
          <strong>${escapeCertificateText(certificado.codigo)}</strong>
          <span>${escapeCertificateText(certificado.atividadeId)} · ${certificado.cargaHorariaMinutos} min</span>
        </div>
        <small>Emitido em ${formatCertificateDate(certificado.emitidoEm)}</small>
      </article>`).join('')
    : '<p class="empty-state">Nenhum certificado emitido.</p>';
}

async function carregarCertificados() {
  try {
    const [certificados, extrato] = await Promise.all([apiFetch('/certificados'), apiFetch('/extrato')]);
    renderCertificates(certificados);
    document.getElementById('total-palestras').textContent = `${extrato.palestrasMinutos} min`;
    document.getElementById('total-minicursos').textContent = `${extrato.minicursosMinutos} min`;
    document.getElementById('total-horas').textContent = `${extrato.totalMinutos} min`;
    document.getElementById('total-aproveitado').textContent = `${extrato.aproveitadoMinutos} min`;
    clearAlert('alert-box');
  } catch (err) {
    showError('alert-box', err);
  }
}

async function verificarCertificado() {
  const codigo = document.getElementById('codigo-certificado').value.trim().toUpperCase();
  if (!codigo) {
    showError('alert-box', { error: 'DADOS_INVALIDOS', message: 'Informe o código do certificado.' });
    return;
  }

  try {
    const certificado = await apiFetch(`/certificados/${encodeURIComponent(codigo)}`, { headers: { 'X-Usuario': '' } });
    document.getElementById('verification-result').className = 'verification-result';
    document.getElementById('verification-result').innerHTML = `
      <strong>${escapeCertificateText(certificado.participante)}</strong>
      <span>${escapeCertificateText(certificado.atividade)}</span>
      <span>${certificado.cargaHorariaMinutos} minutos · emitido em ${formatCertificateDate(certificado.emitidoEm)}</span>`;
    clearAlert('alert-box');
  } catch (err) {
    showError('alert-box', err);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  showCertificatePanels();
  document.getElementById('btn-carregar-certificados').addEventListener('click', carregarCertificados);
  document.getElementById('btn-verificar-certificado').addEventListener('click', verificarCertificado);
  window.addEventListener('userChanged', () => {
    showCertificatePanels();
    if (!getSelectedUser().startsWith('org-')) carregarCertificados();
  });
  if (!getSelectedUser().startsWith('org-')) carregarCertificados();
});