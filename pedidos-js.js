const { escapar } = window.PETEXPRESS_CATALOGO;
const LOGGED_USER_KEY = 'usuarioLogado';
const API_BASE_URL = window.PETEXPRESS_API_URL || 'http://localhost:8082';

const ordersContainer = document.getElementById('ordersContainer');

function getLoggedUser() {
  try {
    const raw = localStorage.getItem(LOGGED_USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    console.error('Erro ao ler usuario logado:', error);
    return null;
  }
}

function getAuthHeaders() {
  const loggedUser = getLoggedUser();
  return loggedUser?.token ? { Authorization: `Bearer ${loggedUser.token}` } : {};
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

function formatPrice(value) {
  const amount = Number(value || 0);
  return `R$ ${amount.toFixed(2).replace('.', ',')}`;
}

function redirectToLogin() {
  window.location.href = 'login.html';
}

function renderEmptyMessage() {
  if (!ordersContainer) return;
  ordersContainer.innerHTML = '<div class="empty-message">Voce ainda nao fez nenhum pedido.</div>';
}

function renderErrorMessage(message) {
  if (!ordersContainer) return;
  ordersContainer.innerHTML = `<div class="empty-message">${message}</div>`;
}

function renderOrders(orders) {
  if (!ordersContainer) return;
  ordersContainer.innerHTML = '';

  orders.forEach((order) => {
    const itemsHtml = (order.itens || []).map((item) => {
      const itemName = item.nome || item.name || 'Produto';
      const itemQty = item.quantidade || item.quantity || 1;
      return `<div class="item-row"><span>${escapar(itemName)}</span><strong>Qtd: ${escapar(itemQty)}</strong></div>`;
    }).join('');

    const card = document.createElement('div');
    card.className = 'order-card';
    card.innerHTML = `
      <div class="order-card-header">
        <div>
          <strong>Pedido ${escapar(order.numeroPedido)}</strong>
          <span>${escapar(formatDate(order.data))}</span>
        </div>
        <div class="order-status">${PETEXPRESS_PEDIDOS.pagamento[order.status] || 'Aguardando pagamento'}</div>
      </div>
      <div class="order-content">
        <div class="order-summary">
          <span>Total</span>
          <strong>${formatPrice(order.total)}</strong>
        </div>
        ${order.descontoCupom > 0 ? `<p>Cupom ${escapar(order.cupom)} · − ${formatPrice(order.descontoCupom)}</p>` : ''}
        <p><strong>Expedição</strong> · ${PETEXPRESS_PEDIDOS.expedicao[order.expedicao] || 'Aguardando'}</p>
        ${order.rastreio ? `<p><strong>Rastreio</strong> · ${escapar(order.rastreio)}</p>` : ''}
        ${order.cupom ? `<p>Cupom ${escapar(order.cupom)} · desconto de ${formatPrice(order.descontoCupom)}</p>` : ''}
        <div class="order-items-list">
          ${itemsHtml}
        </div>
      </div>
    `;

    if (order.formaEntrega === 'Entrega via Uber') {
      const area = document.createElement('div');
      area.className = 'order-content';
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'btn-primary';
      button.textContent = 'Acompanhar entrega';
      const status = document.createElement('p');
      status.setAttribute('role','status');
      button.addEventListener('click', async () => {
        button.disabled = true;
        try {
          const response = await fetch(API_BASE_URL + '/api/pedidos/' + order.id + '/entrega', {headers:getAuthHeaders()});
          const entrega = await response.json();
          if (!response.ok) throw new Error(entrega.message || 'Não foi possível consultar a entrega.');
          const labels = { pending:'Buscando entregador',pickup:'Entregador a caminho da loja',pickup_complete:'Retirada realizada',
            dropoff:'Entrega a caminho',delivered:'Entregue',canceled:'Entrega cancelada',returned:'Devolvida',
            SOLICITANDO:'Solicitação em andamento',AGUARDANDO_CONFIRMACAO:'Aguardando confirmação da Uber' };
          status.textContent = entrega.status ? (labels[entrega.status] || entrega.status) : 'A loja ainda está preparando a entrega.';
          if (entrega.trackingUrl && new URL(entrega.trackingUrl).protocol === 'https:') {
            const link = document.createElement('a');
            link.href = entrega.trackingUrl;
            link.target = '_blank';
            link.rel = 'noopener noreferrer';
            link.textContent = ' Abrir acompanhamento Uber Direct';
            status.append(link);
          }
        } catch(error) { status.textContent = error.message; }
        finally {button.disabled = false;}
      });
      area.append(button,status);
      card.append(area);
    }
    ordersContainer.appendChild(card);
  });
}

async function fetchOrders(userId) {
  const response = await fetch(`${API_BASE_URL}/api/pedidos/usuario/${userId}`, {
    headers: getAuthHeaders()
  });
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Nao foi possivel carregar pedidos: ${response.status} ${errorText}`);
  }
  return response.json();
}

async function initializePage() {
  const loggedUser = getLoggedUser();
  if (!loggedUser || !loggedUser.id) {
    redirectToLogin();
    return;
  }

  try {
    const orders = await fetchOrders(loggedUser.id);
    if (!orders.length) {
      renderEmptyMessage();
      return;
    }
    renderOrders(orders);
  } catch (error) {
    console.error('Erro ao carregar pedidos:', error);
    renderErrorMessage('Nao foi possivel carregar seus pedidos agora.');
  }
}

document.addEventListener('DOMContentLoaded', initializePage);

