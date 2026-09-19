const OFFLINE_QUEUE_KEY = 'm3-presencas-pendentes';
let cameraStream;

function isOrganizationUser() {
  return getSelectedUser().startsWith('org-');
}

function pendingPresences() {
  return JSON.parse(localStorage.getItem(OFFLINE_QUEUE_KEY) || '[]');
}

function savePendingPresences(items) {
  localStorage.setItem(OFFLINE_QUEUE_KEY, JSON.stringify(items));
  renderOfflineQueue();
}

function showPanelForUser() {
  const organization = isOrganizationUser();
  document.getElementById('organization-panel').hidden = !organization;
  document.getElementById('participant-panel').hidden = organization;
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString('pt-BR');
}

function renderOfflineQueue() {
  const queue = pendingPresences();
  document.getElementById('offline-count').textContent = `${queue.length} pendentes`;
  document.getElementById('offline-queue').innerHTML = queue.length
    ? queue.map(item => `<div class="queue-item"><strong>${item.encontroId}</strong><span>${item.codigo}</span></div>`).join('')
    : '<p class="empty-state">Nenhuma leitura pendente.</p>';
}

async function carregarCodigo() {
  const encontroId = document.getElementById('encontro-organizacao').value.trim();
  if (!encontroId) {
    showError('alert-box', { error: 'DADOS_INVALIDOS', message: 'Informe o ID do encontro.' });
    return;
  }

  try {
    const codigo = await apiFetch(`/encontros/${encodeURIComponent(encontroId)}/codigo`);
    document.getElementById('qr-display').textContent = codigo.codigo;
    document.getElementById('qr-display').className = 'qr-display';
    document.getElementById('troca-em').textContent = formatDate(codigo.trocaEm);
    document.getElementById('valido-ate').textContent = formatDate(codigo.validoAte);
    clearAlert('alert-box');
  } catch (err) {
    showError('alert-box', err);
  }
}

async function alternarTelaCheia() {
  const display = document.getElementById('qr-display');
  if (document.fullscreenElement) {
    await document.exitFullscreen();
    return;
  }
  await display.requestFullscreen();
}

async function lerPelaCamera() {
  const video = document.getElementById('camera-preview');
  if (!('BarcodeDetector' in window)) {
    showError('alert-box', { error: 'CAMERA_INDISPONIVEL', message: 'Seu navegador não oferece leitura automática. Digite o código do QR.' });
    return;
  }

  try {
    cameraStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
    video.srcObject = cameraStream;
    video.hidden = false;
    await video.play();
    const detector = new BarcodeDetector({ formats: ['qr_code'] });
    const procurarCodigo = async () => {
      if (video.hidden) return;
      const resultados = await detector.detect(video);
      if (resultados.length > 0) {
        document.getElementById('codigo-presenca').value = resultados[0].rawValue.toUpperCase();
        pararCamera();
        showSuccess('alert-box', 'Código lido. Confirme o registro da presença.');
        return;
      }
      requestAnimationFrame(procurarCodigo);
    };
    requestAnimationFrame(procurarCodigo);
  } catch (err) {
    showError('alert-box', { error: 'CAMERA_NEGADA', message: 'Não foi possível acessar a câmera. Digite o código do QR.' });
  }
}

function pararCamera() {
  if (cameraStream) {
    cameraStream.getTracks().forEach(track => track.stop());
    cameraStream = null;
  }
  const video = document.getElementById('camera-preview');
  video.hidden = true;
  video.srcObject = null;
}

async function registrarPresenca() {
  const encontroId = document.getElementById('encontro-participante').value.trim();
  const codigo = document.getElementById('codigo-presenca').value.trim().toUpperCase();
  if (!encontroId || !codigo) {
    showError('alert-box', { error: 'DADOS_INVALIDOS', message: 'Informe o encontro e o código do QR.' });
    return;
  }

  try {
    const response = await apiFetch(`/encontros/${encodeURIComponent(encontroId)}/presencas`, {
      method: 'POST',
      body: JSON.stringify({ codigo })
    });
    showSuccess('alert-box', `Presença registrada com sucesso (${response.origem}).`);
    document.getElementById('codigo-presenca').value = '';
  } catch (err) {
    if (err.error === 'FALHA_DE_REDE') {
      const queue = pendingPresences();
      queue.push({ encontroId, codigo, lidoEm: new Date().toISOString() });
      savePendingPresences(queue);
      showSuccess('alert-box', 'Sem conexão. A leitura foi salva e será sincronizada depois.');
      return;
    }
    showError('alert-box', err);
  }
}

async function sincronizarPendentes() {
  const queue = pendingPresences();
  const remaining = [];
  for (const item of queue) {
    try {
      await apiFetch(`/encontros/${encodeURIComponent(item.encontroId)}/presencas`, {
        method: 'POST',
        body: JSON.stringify({ codigo: item.codigo, lidoEm: item.lidoEm })
      });
    } catch (err) {
      remaining.push(item);
    }
  }
  savePendingPresences(remaining);
  showSuccess('alert-box', remaining.length ? 'Algumas leituras continuam pendentes.' : 'Leituras sincronizadas.');
}

async function listarPresencas() {
  const encontroId = document.getElementById('encontro-organizacao').value.trim();
  if (!encontroId) {
    showError('alert-box', { error: 'DADOS_INVALIDOS', message: 'Informe o ID do encontro.' });
    return;
  }

  try {
    const presencas = await apiFetch(`/encontros/${encodeURIComponent(encontroId)}/presencas`);
    document.getElementById('presencas-organizacao').innerHTML = presencas.length
      ? presencas.map(presenca => `<div class="queue-item"><strong>${presenca.participanteId}</strong><span>${presenca.origem} · ${formatDate(presenca.registradaEm)}</span></div>`).join('')
      : '<p class="empty-state">Nenhuma presença registrada.</p>';
    clearAlert('alert-box');
  } catch (err) {
    showError('alert-box', err);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  showPanelForUser();
  renderOfflineQueue();
  document.getElementById('btn-atualizar-qr').addEventListener('click', carregarCodigo);
  document.getElementById('qr-display').addEventListener('dblclick', alternarTelaCheia);
  document.getElementById('btn-listar-presencas').addEventListener('click', listarPresencas);
  document.getElementById('btn-camera').addEventListener('click', lerPelaCamera);
  document.getElementById('btn-registrar-presenca').addEventListener('click', registrarPresenca);
  document.getElementById('btn-sincronizar').addEventListener('click', sincronizarPendentes);
  window.addEventListener('userChanged', showPanelForUser);
});