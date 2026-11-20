async function acompanharPagamento() {
  const heading = document.getElementById('paymentStatus');
  const params = new URLSearchParams(location.search);
  const id = params.get('pedido');
  const paymentId = params.get('payment_id') || params.get('collection_id');
  const sessao = JSON.parse(localStorage.getItem('usuarioLogado') || 'null');
  if (!sessao?.token || !/^\d+$/.test(id || '')) {
    heading.textContent = 'Acompanhe o status em Meus pedidos';
    return;
  }

  const headers = { Authorization: 'Bearer ' + sessao.token };
  const api = window.PETEXPRESS_API_URL;
  try {
    if (/^\d+$/.test(paymentId || '')) {
      await fetch(api + '/api/pagamento/pedidos/' + id + '/atualizar', {
        method: 'POST',
        headers: { ...headers, 'Content-Type': 'application/json' },
        body: JSON.stringify({ paymentId })
      }).catch(() => {});
    }
    const response = await fetch(api + '/api/pedidos/' + id, { headers, cache: 'no-store' });
    if (!response.ok) throw new Error('Pedido indisponível');
    const pedido = await response.json();
    heading.textContent = window.PETEXPRESS_PEDIDOS.pagamento[pedido.status] || 'Aguardando confirmação';
    document.getElementById('orderNumber').textContent = pedido.numeroPedido;
    if (pedido.status === 'PAGAMENTO_APROVADO') {
      const checkout = JSON.parse(sessionStorage.getItem('petexpressPedidoCheckout') || 'null');
      if (String(checkout?.pedido) === id && checkout.carrinho === localStorage.getItem('carrinho')) {
        localStorage.removeItem('carrinho');
        sessionStorage.removeItem('petexpressCheckout');
        sessionStorage.removeItem('petexpressPedidoCheckout');
      }
    }
  } catch {
    heading.textContent = 'Consulte a atualização em Meus pedidos';
  }
}

acompanharPagamento();
