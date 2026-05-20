const CART_KEY = 'carrinho';
const LOGGED_KEY = 'usuarioLogado';
const API_BASE_URL = window.PETEXPRESS_API_URL || 'http://localhost:8082';

const orderNumberField = document.getElementById('orderNumber');
const orderInfoField = document.getElementById('orderInfo');
const alertMessageField = document.getElementById('alertMessage');

function getLoggedUser() {
  try {
    const raw = localStorage.getItem(LOGGED_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    console.error('Erro ao ler usuario logado:', error);
    return null;
  }
}

function getPendingPedidoId() {
  return localStorage.getItem('pendingPedidoId');
}

function getAuthHeaders() {
  const loggedUser = getLoggedUser();
  return loggedUser?.token ? { Authorization: `Bearer ${loggedUser.token}` } : {};
}

function getLastOrder() {
  try {
    const raw = localStorage.getItem('lastOrder');
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    return null;
  }
}

function clearCheckoutState() {
  localStorage.removeItem(CART_KEY);
  localStorage.removeItem('pendingPedidoId');
}

function formatDate(dateString) {
  const date = new Date(dateString);
  if (Number.isNaN(date.getTime())) {
    return dateString || '-';
  }
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function showMessage(message) {
  if (!alertMessageField) return;
  alertMessageField.textContent = message;
}

function renderOrder(order) {
  if (orderNumberField) {
    orderNumberField.textContent = order?.numeroPedido || 'N/A';
  }

  if (orderInfoField && order?.data) {
    orderInfoField.textContent = `Pedido confirmado em ${formatDate(order.data)}.`;
  }
}

async function approvePendingOrder(pedidoId) {
  const response = await fetch(`${API_BASE_URL}/api/pedidos/${pedidoId}/aprovar`, {
    method: 'PUT',
    headers: getAuthHeaders()
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Falha ao confirmar pedido: ${response.status} ${errorText}`);
  }

  return response.json();
}

async function initializePage() {
  const loggedUser = getLoggedUser();
  const pedidoId = getPendingPedidoId();

  if (!loggedUser) {
    showMessage('Nao foi possivel confirmar o pedido porque voce nao esta logado.');
    renderOrder(getLastOrder());
    return;
  }

  if (!pedidoId) {
    showMessage('Pedido ja confirmado ou nao encontrado nesta sessao.');
    renderOrder(getLastOrder());
    return;
  }

  try {
    const order = await approvePendingOrder(pedidoId);
    clearCheckoutState();
    renderOrder(order);
    showMessage('Pedido confirmado com sucesso.');
  } catch (error) {
    console.error('Erro ao confirmar pedido:', error);
    showMessage('Pagamento recebido, mas nao foi possivel atualizar o pedido automaticamente. Acesse Meus Pedidos ou tente fazer login novamente.');
    renderOrder(getLastOrder());
  }
}

document.addEventListener('DOMContentLoaded', initializePage);

