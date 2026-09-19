const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function criarAmbiente(fetchImpl) {
  const elementos = new Map();
  const domCallbacks = [];
  const localStorageData = new Map([['X-Usuario', 'p-carla']]);
  const criarElemento = (id) => ({
    id,
    value: '',
    hidden: false,
    className: '',
    innerHTML: '',
    textContent: '',
    listeners: {},
    addEventListener(evento, callback) {
      this.listeners[evento] = callback;
    }
  });
  const document = {
    getElementById(id) {
      if (!elementos.has(id)) elementos.set(id, criarElemento(id));
      return elementos.get(id);
    },
    addEventListener(evento, callback) {
      if (evento === 'DOMContentLoaded') domCallbacks.push(callback);
    }
  };
  const context = {
    console,
    document,
    window: { addEventListener() {} },
    localStorage: {
      getItem: (chave) => localStorageData.get(chave) || null,
      setItem: (chave, valor) => localStorageData.set(chave, valor)
    },
    fetch: fetchImpl,
    setTimeout,
    clearTimeout
  };
  vm.createContext(context);
  for (const arquivo of ['app.js', 'certificados.js']) {
    const codigo = fs.readFileSync(path.join(__dirname, '..', arquivo), 'utf8');
    vm.runInContext(codigo, context, { filename: arquivo });
  }
  for (const callback of domCallbacks) callback();
  return { context, elementos };
}

test('verifica certificado usando resposta da API fake', async () => {
  const ambiente = criarAmbiente(async (url) => ({
    ok: true,
    status: 200,
    async json() {
      return {
        codigo: 'SA26-TESTE',
        participante: 'Carla Mendes Souza',
        atividade: 'Palestra de teste',
        cargaHorariaMinutos: 60,
        emitidoEm: '2026-10-20T12:00:00-03:00'
      };
    }
  }));
  const campo = ambiente.context.document.getElementById('codigo-certificado');
  campo.value = 'SA26-TESTE';

  await ambiente.context.verificarCertificado();

  const resultado = ambiente.context.document.getElementById('verification-result');
  assert.match(resultado.innerHTML, /Carla Mendes Souza/);
  assert.match(resultado.innerHTML, /Palestra de teste/);
});

test('exibe erro amigavel quando a API rejeita o certificado', async () => {
  const ambiente = criarAmbiente(async () => ({
    ok: false,
    status: 404,
    async json() {
      return { erro: 'NAO_ENCONTRADO', mensagem: 'Certificado não encontrado' };
    }
  }));
  ambiente.context.document.getElementById('codigo-certificado').value = 'SA26-INVALIDO';

  await ambiente.context.verificarCertificado();

  const alerta = ambiente.context.document.getElementById('alert-box');
  assert.equal(alerta.className, 'alert error');
  assert.match(alerta.innerHTML, /NAO_ENCONTRADO/);
});
