const form = byId('promo-form');
const dialog = byId('promo-dialog');
const moeda = new Intl.NumberFormat('pt-BR', {style:'currency', currency:'BRL'});
const datas = new Intl.DateTimeFormat('pt-BR', {dateStyle:'short', timeStyle:'short'});
let ofertas = [], cupons = [], produtos = [], atual, tipoAtual, salvando = false;

function situacao(registro) {
  if (!registro.ativo) return 'Pausada';
  if (new Date(registro.inicio) > new Date()) return 'Agendada';
  if (new Date(registro.fim) <= new Date()) return 'Encerrada';
  if (registro.usos >= registro.limite) return 'Limite atingido';
  return 'Ativa';
}
function periodo(item) {
  return datas.format(new Date(item.inicio)) + ' a ' + datas.format(new Date(item.fim));
}
function listar() {
  byId('offer-empty').hidden = ofertas.length > 0;
  byId('coupon-empty').hidden = cupons.length > 0;
  byId('offer-rows').innerHTML = ofertas.map(item => {
    const produto = produtos.find(p => p.id === item.produtoId);
    const tamanho = produto?.variacoes.find(v => v.codigo === item.variacaoCodigo)?.embalagem;
    return `<tr><td>${escapar(item.nome)}</td><td>${escapar(produto?.nome || 'Produto indisponível')}<small class="seller-order-date">${escapar(tamanho || (item.variacaoCodigo ? 'Tamanho removido' : 'Todos os tamanhos'))}</small></td><td>${item.percentual}%</td><td>${periodo(item)}</td><td>${situacao(item)}</td><td><button type="button" class="seller-secondary" data-type="ofertas" data-id="${item.id}">Editar oferta</button></td></tr>`;
  }).join('');
  byId('coupon-rows').innerHTML = cupons.map(item => `<tr><td><strong>${escapar(item.codigo)}</strong></td><td>${item.tipo === 'PERCENTUAL' ? item.valor + '%' : moeda.format(item.valor)}</td><td>${moeda.format(item.minimo)}</td><td>${item.usos} / ${item.limite}</td><td>${periodo(item)}</td><td>${situacao(item)}</td><td><button type="button" class="seller-secondary" data-type="cupons" data-id="${item.id}">Editar cupom</button></td></tr>`).join('');
}
function preencherTamanhos(codigo = '') {
  const produto = produtos.find(p => String(p.id) === form.elements.produtoId.value);
  const select = form.elements.variacaoCodigo;
  select.replaceChildren(new Option('Todos os tamanhos', ''));
  produto?.variacoes.forEach(v => select.add(new Option(v.embalagem, v.codigo)));
  select.value = codigo;
}
function dataLocal(valor) {
  const data = new Date(valor);
  return new Date(data.getTime() - data.getTimezoneOffset()*60000).toISOString().slice(0,16);
}
function editar(tipo, item = null) {
  atual = item; tipoAtual = tipo; form.reset();
  const oferta = tipo === 'ofertas';
  byId('promo-title').textContent = item ? (oferta ? 'Editar oferta' : 'Editar cupom') : (oferta ? 'Nova oferta' : 'Novo cupom');
  byId('offer-fields').hidden = !oferta;
  byId('coupon-fields').hidden = oferta;
  byId('offer-fields').querySelectorAll('input,select').forEach(el => el.disabled = !oferta);
  byId('coupon-fields').querySelectorAll('input,select').forEach(el => el.disabled = oferta);
  form.elements.produtoId.replaceChildren(new Option('Selecione', ''));
  produtos.forEach(p => form.elements.produtoId.add(new Option(p.nome, p.id)));
  for (const name of ['nome','produtoId','percentual','codigo','tipo','valor','minimo','limite']) {
    if (item?.[name] != null) form.elements[name].value = item[name];
  }
  preencherTamanhos(item?.variacaoCodigo || '');
  form.elements.inicio.value = dataLocal(item?.inicio || new Date());
  form.elements.fim.value = dataLocal(item?.fim || new Date(Date.now() + 7*86400000));
  form.elements.ativo.checked = item?.ativo ?? true;
  if (!item) { form.elements.minimo.value = '0'; form.elements.limite.value = '100'; }
  byId('promo-error').hidden = true;
  dialog.showModal();
}
async function carregar() {
  [ofertas, cupons, produtos] = await Promise.all([
    request('/promocoes/ofertas'), request('/promocoes/cupons'), request('/produtos')
  ]);
  listar();
}
form.elements.produtoId.addEventListener('change', () => preencherTamanhos());
byId('new-offer').addEventListener('click', () => editar('ofertas'));
byId('new-coupon').addEventListener('click', () => editar('cupons'));
byId('promo-close').addEventListener('click', () => dialog.close());
dialog.addEventListener('cancel', e => {if (salvando) e.preventDefault();});
byId('catalog').addEventListener('click', e => {
  const button = e.target.closest('[data-type]');
  if (button) editar(button.dataset.type, (button.dataset.type === 'ofertas' ? ofertas : cupons).find(item => String(item.id) === button.dataset.id));
});
form.addEventListener('submit', async e => {
  e.preventDefault();
  if (salvando) return;
  const campos = new FormData(form);
  const dados = Object.fromEntries(campos);
  dados.ativo = form.elements.ativo.checked;
  dados.inicio = new Date(dados.inicio).toISOString();
  dados.fim = new Date(dados.fim).toISOString();
  for (const campo of ['produtoId','percentual','valor','minimo','limite']) {
    if (campo in dados) dados[campo] = Number(dados[campo]);
  }
  if (dados.variacaoCodigo === '') dados.variacaoCodigo = null;
  salvando = true;
  const controles = [...form.elements].filter(el => !el.disabled);
  controles.forEach(el => el.disabled = true);
  try {
    await request('/promocoes/' + tipoAtual + (atual ? '/' + atual.id : ''), {method:atual ? 'PUT' : 'POST', body:JSON.stringify(dados)});
    dialog.close();
    await carregar();
    byId('message').textContent = 'Promoção salva.';
  } catch(error) {
    byId('promo-error').textContent = error.message;
    byId('promo-error').hidden = false;
  } finally {
    salvando = false;
    controles.forEach(el => el.disabled = false);
  }
});
init(carregar);
