const { pagamento, expedicao } = window.PETEXPRESS_PEDIDOS;
const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const dialog = byId('order-dialog');
const form = byId('order-form');
let pedidos = [];
let pedidoAtual;
let proximaEtapa;
let salvando = false;

function textoBusca(texto) {
  return texto.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '');
}

function data(valor) {
  const parsed = new Date(valor);
  return valor && !Number.isNaN(parsed.getTime()) ? dataHora.format(parsed) : '—';
}

function listar() {
  const busca = textoBusca(byId('order-search').value.trim());
  const filtroPagamento = byId('payment-filter').value;
  const filtroExpedicao = byId('dispatch-filter').value;
  const visiveis = pedidos.filter(pedido =>
    textoBusca([pedido.numeroPedido, pedido.cliente, pedido.usuarioEmail].join(' ')).includes(busca)
    && (!filtroPagamento || pedido.status === filtroPagamento)
    && (!filtroExpedicao || pedido.expedicao === filtroExpedicao));
  byId('order-empty').hidden = visiveis.length > 0;
  byId('order-rows').innerHTML = visiveis.map(pedido => `
    <tr>
      <td><strong>${escapar(pedido.numeroPedido)}</strong><small class="seller-order-date">${data(pedido.data)}</small></td>
      <td>${escapar(pedido.cliente || pedido.usuarioEmail)}</td>
      <td>${moeda.format(pedido.total)}</td>
      <td><span class="seller-status ${pedido.status === 'PAGAMENTO_APROVADO' ? 'PUBLICADO' : 'PAUSADO'}">${pagamento[pedido.status] || 'Aguardando pagamento'}</span></td>
      <td>${expedicao[pedido.expedicao] || 'Aguardando separação'}</td>
      <td><button class="seller-secondary" type="button" data-id="${pedido.id}" aria-label="Ver pedido ${escapar(pedido.numeroPedido)}">Ver pedido</button></td>
    </tr>`).join('');
}

function detalhes(pedido) {
  pedidoAtual = pedido;
  byId('order-title').textContent = 'Pedido ' + pedido.numeroPedido;
  byId('order-date').textContent = data(pedido.data);
  byId('order-details').innerHTML = `
    <dl class="seller-order-meta">
      <div><dt>Cliente</dt><dd>${escapar(pedido.cliente || pedido.usuarioEmail)}</dd></div>
      <div><dt>Forma de entrega</dt><dd>${escapar(pedido.formaEntrega || '—')}</dd></div>
      ${pedido.enderecoEntrega ? `<div><dt>Endereço</dt><dd>${escapar(pedido.enderecoEntrega)}</dd></div>` : ''}
      <div><dt>Pagamento</dt><dd>${pagamento[pedido.status] || 'Aguardando pagamento'}</dd></div>
      <div><dt>Expedição</dt><dd>${expedicao[pedido.expedicao] || 'Aguardando separação'}</dd></div>
      ${pedido.rastreio ? `<div><dt>Rastreio</dt><dd>${escapar(pedido.rastreio)}</dd></div>` : ''}
    </dl>
    <ul class="seller-history-rows">
      ${pedido.itens.map(item => `<li>${escapar(item.nome)} <strong>× ${item.quantidade}</strong> · ${moeda.format(item.preco)} por un.</li>`).join('')}
    </ul>
    ${pedido.taxaEntrega > 0 ? `<p>Entrega · ${moeda.format(pedido.taxaEntrega)}</p>` : ''}
    ${pedido.descontoCupom > 0 ? `<p>Cupom ${escapar(pedido.cupom)} · − ${moeda.format(pedido.descontoCupom)}</p>` : ''}
    ${pedido.cupom ? `<p>Cupom ${escapar(pedido.cupom)} · − ${moeda.format(pedido.descontoCupom)}</p>` : ''}
    <p><strong>Total · ${moeda.format(pedido.total)}</strong></p>`;

  const porRetirada = pedido.formaEntrega === 'Retirar na loja';
  const etapas = {
    AGUARDANDO: ['EM_SEPARACAO', 'Iniciar separação'],
    EM_SEPARACAO: porRetirada ? ['PRONTO_PARA_RETIRADA', 'Marcar pronto para retirada'] : ['ENVIADO', 'Marcar como enviado'],
    ENVIADO: ['CONCLUIDO', 'Concluir entrega'],
    PRONTO_PARA_RETIRADA: ['CONCLUIDO', 'Confirmar retirada']
  };
  const uber = pedido.formaEntrega === 'Entrega via Uber';
  const proxima = pedido.status === 'PAGAMENTO_APROVADO' && !(uber && pedido.expedicao !== 'AGUARDANDO') ? etapas[pedido.expedicao] : null;
  proximaEtapa = proxima?.[0];
  byId('order-next').hidden = !proxima;
  byId('order-next').textContent = proxima?.[1] || '';
  byId('tracking').hidden = proximaEtapa !== 'ENVIADO';
  form.elements.rastreio.value = pedido.rastreio || '';
  byId('order-error').hidden = true;
  if (uber && pedido.status === 'PAGAMENTO_APROVADO') {
    const entrega = document.createElement('section');
    entrega.className = 'seller-section-heading';
    entrega.id = 'seller-order-delivery';
    entrega.innerHTML = '<h3>Entrega Uber Direct</h3><p id="seller-delivery-message" role="status"></p><div class="seller-actions"><button type="button" class="seller-secondary" id="seller-delivery-refresh">Atualizar acompanhamento</button>'
      + (pedido.expedicao === 'EM_SEPARACAO' ? '<button type="button" class="seller-secondary" id="seller-delivery-quote">Consultar valor da entrega</button><button type="button" class="btn-submit" id="seller-delivery-request" hidden></button>' : '') + '</div>';
    byId('order-details').append(entrega);
    byId('delivery-refresh').addEventListener('click', () => atualizarEntrega(pedido.id));
    if (pedido.expedicao === 'EM_SEPARACAO') prepararSolicitacao(pedido);
    atualizarEntrega(pedido.id);
  }
  if (!dialog.open) dialog.showModal();
}

async function carregar() {
  const lista = await request('/pedidos');
  pedidos = location.pathname.endsWith('/vendedor-vendas.html')
    ? lista.filter(pedido => pedido.status === 'PAGAMENTO_APROVADO') : lista;
  listar();
}

for (const [id, labels] of [['payment-filter', pagamento], ['dispatch-filter', expedicao]]) {
  for (const [value, label] of Object.entries(labels)) byId(id).add(new Option(label, value));
  byId(id).addEventListener('change', listar);
}
byId('order-search').addEventListener('input', listar);
byId('reload').addEventListener('click', async () => {
  byId('reload').disabled = true;
  try {
    await carregar();
    byId('message').textContent = 'Lista atualizada.';
  } catch (error) {
    byId('message').textContent = error.message;
  } finally {
    byId('reload').disabled = false;
  }
});
byId('order-rows').addEventListener('click', event => {
  const id = event.target.closest('[data-id]')?.dataset.id;
  if (id) detalhes(pedidos.find(pedido => String(pedido.id) === id));
});
byId('order-close').addEventListener('click', () => dialog.close());
dialog.addEventListener('cancel', event => { if (salvando) event.preventDefault(); });
form.addEventListener('submit', async event => {
  event.preventDefault();
  if (!proximaEtapa || salvando) return;
  salvando = true;
  byId('order-next').disabled = true;
  byId('order-close').disabled = true;
  try {
    const atualizado = await request('/pedidos/' + pedidoAtual.id + '/expedicao', {
      method: 'PUT',
      body: JSON.stringify({ expedicao: proximaEtapa, rastreio: form.elements.rastreio.value.trim() })
    });
    pedidos = pedidos.map(pedido => pedido.id === atualizado.id ? atualizado : pedido);
    listar();
    detalhes(atualizado);
    byId('message').textContent = 'Expedição atualizada.';
  } catch (error) {
    byId('order-error').textContent = error.message;
    byId('order-error').hidden = false;
  } finally {
    salvando = false;
    byId('order-next').disabled = false;
    byId('order-close').disabled = false;
  }
});

const statusEntrega = {
  pending:'Buscando entregador', pickup:'Entregador a caminho da loja', pickup_complete:'Retirada realizada',
  dropoff:'A caminho do destinatário', delivered:'Entregue', canceled:'Cancelada', returned:'Devolvida',
  SOLICITANDO:'Solicitação em andamento', AGUARDANDO_CONFIRMACAO:'Aguardando confirmação da Uber'
};
async function atualizarEntrega(id) {
  const message = byId('delivery-message');
  if (!message) return;
  try {
    const entrega = await request('/pedidos/' + id + '/entrega');
    if (pedidoAtual.id !== id || !byId('delivery-message')) return;
    message.textContent = entrega.status ? (statusEntrega[entrega.status] || entrega.status) : 'Entregador ainda não solicitado.';
    if (entrega.trackingUrl && new URL(entrega.trackingUrl).protocol === 'https:') {
      const link = document.createElement('a');
      link.href = entrega.trackingUrl;
      link.target = '_blank';
      link.rel = 'noopener noreferrer';
      link.textContent = ' Acompanhar entrega';
      message.append(link);
    }
  } catch(error) {message.textContent = error.message;}
}
function prepararSolicitacao(pedido) {
  let quote;
  const button = byId('delivery-quote');
  const confirmar = byId('delivery-request');
  const message = byId('delivery-message');
  button.addEventListener('click', async () => {
    button.disabled = true;
    try {
      quote = await request('/pedidos/' + pedido.id + '/entrega/cotacao',{method:'POST'});
      message.textContent = 'Cobrado do cliente: ' + moeda.format(pedido.taxaEntrega) + '. Cotação atual da Uber: ' + moeda.format(quote.valor) + '.';
      confirmar.textContent = 'Solicitar entregador por ' + moeda.format(quote.valor);
      confirmar.hidden = false;
    } catch(error) {message.textContent = error.message;}
    finally {button.disabled = false;}
  });
  confirmar.addEventListener('click', async () => {
    if (!quote || !confirm('Confirmar a solicitação real à Uber Direct pelo valor de ' + moeda.format(quote.valor) + '?')) return;
    confirmar.disabled = true;
    button.disabled = true;
    try {
      await request('/pedidos/' + pedido.id + '/entrega',{method:'POST',body:JSON.stringify({cotacaoId:quote.id,valorConfirmado:quote.valor})});
      confirmar.hidden = true;
      button.hidden = true;
      await atualizarEntrega(pedido.id);
    } catch(error) {message.textContent = error.message;}
    finally {confirmar.disabled = false;button.disabled = false;}
  });
}
init(carregar);
