const API_URL = 'http://localhost:3000';

function getSelectedUser() {
  const select = document.getElementById('global-user-select');
  if (select) {
    return select.value;
  }
  return localStorage.getItem('X-Usuario') || 'org-ana';
}

function initUserSelector() {
  const container = document.getElementById('user-selector-container');
  if (!container) return;

  const users = [
    { id: 'org-ana', name: 'Ana Beatriz Lima (Organização)' },
    { id: 'org-bruno', name: 'Bruno Tavares (Organização)' },
    { id: 'p-carla', name: 'Carla Mendes (Participante)' },
    { id: 'p-diego', name: 'Diego Alves (Participante)' }
  ];

  const savedUser = localStorage.getItem('X-Usuario') || 'org-ana';

  container.innerHTML = `
    <div class="user-config">
      <label for="global-user-select">Usuário Logado (X-Usuario):</label>
      <select id="global-user-select">
        ${users.map(u => `<option value="${u.id}" ${u.id === savedUser ? 'selected' : ''}>${u.name} (${u.id})</option>`).join('')}
      </select>
    </div>
  `;

  document.getElementById('global-user-select').addEventListener('change', (e) => {
    localStorage.setItem('X-Usuario', e.target.value);
    // Trigger custom event or reload if needed
    window.dispatchEvent(new CustomEvent('userChanged', { detail: e.target.value }));
  });
}

async function apiFetch(endpoint, options = {}) {
  const user = getSelectedUser();
  const headers = {
    'Content-Type': 'application/json',
    'X-Usuario': user,
    ...(options.headers || {})
  };

  try {
    const response = await fetch(`${API_URL}${endpoint}`, {
      ...options,
      headers
    });

    const data = response.status !== 204 ? await response.json() : null;

    if (!response.ok) {
      throw {
        status: response.status,
        error: data && data.erro ? data.erro : 'ERRO_DESCONHECIDO',
        message: data && data.mensagem ? data.mensagem : 'Erro desconhecido na requisição'
      };
    }

    return data;
  } catch (err) {
    if (err.status) {
      throw err;
    }
    throw {
      status: 500,
      error: 'FALHA_DE_REDE',
      message: 'Não foi possível conectar à API. Verifique se o servidor está rodando na porta 3000.'
    };
  }
}

function showError(containerId, errorObj) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.className = 'alert error';
  el.innerHTML = `<strong>[${errorObj.error}]</strong> ${errorObj.message}`;
}

function showSuccess(containerId, message) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.className = 'alert success';
  el.innerHTML = message;
}

function clearAlert(containerId) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.className = 'alert';
  el.innerHTML = '';
}

document.addEventListener('DOMContentLoaded', () => {
  initUserSelector();
});
