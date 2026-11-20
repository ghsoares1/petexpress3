const form = byId('report-form');
const moeda = new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'});
const hoje = new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
form.elements.inicio.value = hoje.slice(0,8) + '01';
form.elements.fim.value = hoje;
form.elements.inicio.max = hoje;
form.elements.fim.max = hoje;

function tendencia(valor) {
  if (valor == null) return '<small>Sem base no período anterior</small>';
  return '<small><span class="seller-trend ' + (valor < 0 ? 'down' : '') + '">' + (valor < 0 ? '↓ ' : '↑ ') + Math.abs(valor).toLocaleString('pt-BR') + '%</span> em relação ao período anterior</small>';
}
async function carregar() {
  const params = new URLSearchParams({inicio:form.elements.inicio.value,fim:form.elements.fim.value});
  const relatorio = await request('/relatorios?' + params);
  byId('metrics').innerHTML = [
    ['Total de vendas',moeda.format(relatorio.faturamento),relatorio.variacaoVendas],
    ['Total de pedidos',relatorio.pedidosAprovados,relatorio.variacaoPedidos],
    ['Ticket médio',moeda.format(relatorio.ticketMedio),relatorio.variacaoTicket]
  ].map(([nome,valor,variacao]) => `<article class="auth-card"><p>${nome}</p><strong>${valor}</strong>${tendencia(variacao)}</article>`).join('');
  const categorias = relatorio.vendasPorCategoria;
  const unidades = categorias.reduce((total,item) => total + item.unidades,0);
  byId('category-chart').innerHTML = categorias.length
    ? '<div class="seller-bars" role="img" aria-label="Participação por categoria em unidades vendidas">' + categorias.map(item => {
      const percentual = 100 * item.unidades / unidades;
      return `<div class="seller-bar-column"><span class="seller-bar-value">${percentual.toFixed(0)}%</span><div class="seller-bar" style="height:${percentual}%"></div><span class="seller-bar-name">${escapar(item.nome)}</span></div>`;
    }).join('') + '</div>'
    : '<p class="seller-empty">Nenhuma venda aprovada no período.</p>';
  const formas = relatorio.formasPagamento;
  const total = formas.reduce((n,item) => n + item.unidades,0);
  const cores = ['#ff8717','#ffc329','#8a80ac','#8da4af','#c7d3da'];
  let inicio = 0;
  const faixas = formas.map((item,index) => {
    const fim = inicio + item.unidades / total * 100;
    const faixa = cores[index % cores.length] + ' ' + inicio + '% ' + fim + '%';
    inicio = fim;
    return faixa;
  });
  byId('payment-chart').innerHTML = total ? `<div class="seller-donut-layout"><div class="seller-donut" style="background:conic-gradient(${faixas.join(',')})" role="img" aria-label="Formas de pagamento de ${total} pedidos"><div class="seller-donut-hole"><strong>${total}</strong><span>pedidos</span></div></div><ul class="seller-legend">${formas.map((item,index) => `<li><i style="background:${cores[index % cores.length]}"></i><span>${escapar(item.nome)}</span><strong>${(100*item.unidades/total).toFixed(0)}%</strong></li>`).join('')}</ul></div>` : '<p class="seller-empty">Nenhum pagamento aprovado no período.</p>';
  byId('report-detail').innerHTML = `<span>Receita de produtos <strong>${moeda.format(relatorio.receitaProdutos)}</strong></span><span>Frete recebido <strong>${moeda.format(relatorio.frete)}</strong></span><span>Descontos concedidos <strong>${moeda.format(relatorio.descontos)}</strong></span>`;
  for (const [id,itens] of [['top-products',relatorio.produtosMaisVendidos],['top-sizes',relatorio.tamanhosMaisVendidos]]) {
    byId(id).innerHTML = itens.length ? itens.map(item => `<li><span>${escapar(item.nome)}</span><strong>${item.unidades} un.</strong></li>`).join('') : '<li>Nenhuma venda no período.</li>';
  }
}
form.addEventListener('submit',async event => {
  event.preventDefault();
  const button = form.querySelector('button');
  button.disabled = true;
  try {await carregar();byId('message').textContent = '';}
  catch(error) {byId('message').textContent = error.message;}
  finally {button.disabled = false;}
});
init(carregar);
