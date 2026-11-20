const form = byId('report-form');
const moeda = new Intl.NumberFormat('pt-BR', {style:'currency',currency:'BRL'});
const hoje = new Intl.DateTimeFormat('en-CA', {timeZone:'America/Sao_Paulo', year:'numeric', month:'2-digit', day:'2-digit'}).format(new Date());
form.elements.inicio.value = hoje.slice(0,8) + '01';
form.elements.fim.value = hoje;
form.elements.inicio.max = hoje;
form.elements.fim.max = hoje;

function ranking(id, itens) {
  byId(id).innerHTML = itens.length ? itens.map(item => `<li><span>${escapar(item.nome)}</span><strong>${item.unidades} un.</strong></li>`).join('') : '<li>Nenhuma venda aprovada neste período.</li>';
}
async function carregar() {
  const params = new URLSearchParams({inicio:form.elements.inicio.value, fim:form.elements.fim.value});
  const relatorio = await request('/relatorios?' + params);
  const metricas = [
    ['Faturamento aprovado', moeda.format(relatorio.faturamento)],
    ['Pedidos aprovados', relatorio.pedidosAprovados],
    ['Ticket médio', moeda.format(relatorio.ticketMedio)],
    ['Receita de produtos', moeda.format(relatorio.receitaProdutos)],
    ['Frete recebido', moeda.format(relatorio.frete)],
    ['Descontos concedidos', moeda.format(relatorio.descontos)]
  ];
  byId('metrics').innerHTML = metricas.map(([label,value]) => `<article class="auth-card"><p>${label}</p><strong>${value}</strong></article>`).join('');
  byId('attention').innerHTML = `<p class="seller-attention-line"><a href="vendedor-pedidos.html">${relatorio.aguardandoSeparacao} pedido(s) pago(s) aguardando separação</a></p>
    ${relatorio.alertas.length ? `<ul class="seller-history-rows">${relatorio.alertas.map(item => `<li><strong>${escapar(item.produto)} · ${escapar(item.tamanho)}</strong><p>${item.disponivel === 0 ? 'Esgotado' : 'Estoque baixo'} · ${item.disponivel} disponíveis · mínimo ${item.minimo}</p></li>`).join('')}</ul><a href="vendedor-estoque.html">Gerenciar estoque</a>` : '<p>Nenhuma embalagem esgotada ou com estoque baixo.</p>'}`;
  ranking('top-products', relatorio.produtosMaisVendidos);
  ranking('top-sizes', relatorio.tamanhosMaisVendidos);
  byId('sales-days').innerHTML = relatorio.vendasPorDia.length ? `<table><thead><tr><th>Data</th><th>Faturamento aprovado</th></tr></thead><tbody>${relatorio.vendasPorDia.map(dia => `<tr><td>${dia.data.split('-').reverse().join('/')}</td><td>${moeda.format(dia.faturamento)}</td></tr>`).join('')}</tbody></table>` : '<p>Nenhuma venda aprovada neste período.</p>';
}
form.addEventListener('submit', async event => {
  event.preventDefault();
  const button = form.querySelector('button');
  button.disabled = true;
  try { await carregar(); byId('message').textContent = 'Relatório atualizado.'; }
  catch(error) { byId('message').textContent = error.message; }
  finally { button.disabled = false; }
});
init(carregar);
