const { escapar } = window.PETEXPRESS_CATALOGO;
const byId = id => document.getElementById('seller-' + id);
const destinoPainel = { 'vendedor-vendas.html': 'vendas', 'vendedor-configuracoes.html': 'configuracoes', 'vendedor-relatorios.html': 'relatorios', 'vendedor-resumo.html': 'resumo', 'vendedor-promocoes.html': 'promocoes', 'vendedor-estoque.html': 'estoque', 'vendedor-pedidos.html': 'pedidos-vendedor' };
const loginUrl = 'login.html?redirect=' + (destinoPainel[location.pathname.split('/').pop()] || 'vendedor');

function readSession() {
  try {
    return JSON.parse(localStorage.getItem('usuarioLogado'));
  } catch {
    return null;
  }
}

let sellerProfile;
function request(path, options = {}) {
  return requestApi('/api/vendedor' + path, options);
}
async function requestApi(path, options = {}) {
  const response = await fetch(window.PETEXPRESS_API_URL + path, {
    ...options,
    cache: 'no-store',
    headers: {
      Authorization: 'Bearer ' + readSession()?.token,
      'Content-Type': 'application/json'
    }
  });
  if (!response.ok) {
    if ([401, 403].includes(response.status)) {
      const error = new Error('Entre com uma conta autorizada para acessar o painel do vendedor.');
      error.accessDenied = true;
      throw error;
    }
    const data = await response.json().catch(() => ({}));
    const error = new Error(data.message || 'Não foi possível concluir a operação.');
    error.status = response.status;
    throw error;
  }
  if (response.status === 204 || response.headers.get('content-length') === '0') return null;
  const texto = await response.text();
  return texto ? JSON.parse(texto) : null;
}

function sairDaConta() {
  localStorage.removeItem('usuarioLogado');
  localStorage.removeItem('usuarioCadastrado');
  location.href = loginUrl;
}
byId('logout').addEventListener('click', sairDaConta);
byId('account-logout').addEventListener('click', sairDaConta);

async function init(carregar) {
  if (!readSession()?.token) {
    location.replace(loginUrl);
    return;
  }
  try {
    const [loja, perfil] = await Promise.all([
      request('/configuracoes'), requestApi('/api/usuarios/' + readSession().id)
    ]);
    sellerProfile = perfil;
    atualizarIdentidade(perfil);
    byId('shop-name').textContent = loja.nome;
    await carregar();
    byId('message').textContent = '';
    byId('catalog').hidden = false;
    byId('nav').hidden = false;
  } catch (error) {
    byId('message').textContent = error.message;
    byId('access').hidden = !error.accessDenied;
  }
}


function atualizarIdentidade(perfil) {
  const nome = [perfil.nome,perfil.sobrenome].filter(Boolean).join(' ');
  const iniciais = [perfil.nome?.[0],perfil.sobrenome?.[0]].filter(Boolean).join('').toLocaleUpperCase('pt-BR');
  byId('account-avatar').textContent = iniciais || 'V';
  byId('account-name').textContent = perfil.nome || 'Minha conta';
  byId('account-fullname').textContent = nome || 'Minha conta';
  byId('account-email').textContent = perfil.email || '';
  byId('account-toggle').setAttribute('aria-label','Abrir conta de ' + (nome || 'vendedor'));
}
