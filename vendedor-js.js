const endpoint = '/produtos';
const form = byId('form');
const dialog = byId('dialog');
const variants = byId('variants');
const statusLabels = { PUBLICADO: 'Publicado', RASCUNHO: 'Rascunho', PAUSADO: 'Pausado' };
const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
let items = [];
let current = null;
let saving = false;

function normalize(text) {
  return text.toLocaleLowerCase('pt-BR').normalize('NFD').replace(/\p{Diacritic}/gu, '');
}

function render() {
  const search = normalize(byId('search').value.trim());
  const status = byId('filter').value;
  const filtered = items.filter(item =>
    normalize(item.nome || '').includes(search) && (!status || item.situacao === status)
  );
  byId('empty').hidden = filtered.length > 0;
  byId('rows').innerHTML = filtered.map(item => `
    <tr>
      <td>
        <div class="seller-item">
          <img src="${escapar(item.imagem || 'img/lg.png')}" alt="" loading="lazy">
          <div><strong>${escapar(item.nome)}</strong><small>${escapar(item.tipoProduto)}</small></div>
        </div>
      </td>
      <td>${item.preco == null ? '—' : money.format(item.preco)}</td>
      <td><span class="seller-status ${item.situacao}">${statusLabels[item.situacao]}</span></td>
      <td><button type="button" class="seller-secondary" data-id="${item.id}" aria-label="Editar ${escapar(item.nome)}">Editar</button></td>
    </tr>
  `).join('');
}

function addVariant(variant = {}) {
  const row = byId('variant-template').content.firstElementChild.cloneNode(true);
  row.dataset.code = variant.codigo || '';
  row.querySelector('[name=embalagem]').value = variant.embalagem || '';
  row.querySelector('[name=preco]').value = variant.preco ?? '';
  const estoque = row.querySelector('[name=estoque]');
  estoque.value = variant.estoque ?? '';
  estoque.readOnly = !!variant.codigo;
  if (variant.estoque > 0) {
    const remover = row.querySelector('[data-remove]');
    remover.disabled = true;
    remover.dataset.comEstoque = 'true';
    remover.title = 'Acerte o saldo na aba Estoque para remover esta embalagem.';
  }
  row.querySelector('[data-remove]').addEventListener('click', () => { row.remove(); atualizarTamanhos(); });
  row.querySelector('[name=embalagem]').addEventListener('input', atualizarTamanhos);
  row.querySelector('[name=preco]').addEventListener('input', atualizarPrecoPrincipal);
  variants.append(row);
  atualizarTamanhos();
  return row;
}

function edit(item = null) {
  current = item;
  form.reset();
  variants.replaceChildren();
  for (const name of ['nome', 'tipoProduto', 'tipoAnimal', 'categoria', 'descricao']) {
    form.elements[name].value = item?.[name] || '';
  }
  form.elements.fotos.value = (item?.fotos?.length ? item.fotos : [item?.imagem || '']).join('\n');
  const legacyPrice = item?.variacoes.length === 0 && item.preco != null;
  byId('legacy-price').hidden = !legacyPrice;
  form.elements.precoAvulso.disabled = !legacyPrice;
  form.elements.precoAvulso.value = legacyPrice ? item.preco : '';
  item?.variacoes.forEach(addVariant);
  if (!item) addVariant({estoque:0});

  byId('form-title').textContent = item ? 'Editar Produto' : 'Cadastro de Produtos';

  form.elements.produtoAtivo.checked = !item || item.situacao === 'PUBLICADO';
  byId('draft').hidden = !!item && item.situacao !== 'RASCUNHO';
  byId('publish').textContent = 'Salvar produto';
  byId('form-error').hidden = true;
  atualizarTamanhos();
  atualizarPrecoPrincipal();
  atualizarFoto();
  byId('photo-editor').hidden = true;
  byId('product-extra').open = !item;
  byId('catalog').hidden = true;
  dialog.hidden = false;
  form.elements.nome.focus();
  window.scrollTo(0, 0);
}

function setSaving(active) {
  saving = active;
  Array.from(form.elements).forEach(control => {
    control.disabled = active || control.dataset.comEstoque === 'true' || (control.name === 'precoAvulso' && byId('legacy-price').hidden);
  });
}

form.addEventListener('submit', async event => {
  event.preventDefault();
  if (saving) return;
  const fields = new FormData(form);
  const data = Object.fromEntries(['nome', 'tipoProduto', 'tipoAnimal', 'categoria', 'descricao']
    .map(name => [name, fields.get(name)]));
  data.fotos = fields.get('fotos').split(/\r?\n/).map(foto => foto.trim()).filter(Boolean);
  data.situacao = event.submitter?.value === 'RASCUNHO' ? 'RASCUNHO' : (form.elements.produtoAtivo.checked ? 'PUBLICADO' : 'PAUSADO');
  data.precoAvulso = !byId('legacy-price').hidden ? Number(form.elements.precoPrincipal.value) : null;
  data.variacoes = [...variants.children].filter(row => row.querySelector("[name=embalagem]").value.trim() || row.querySelector("[name=preco]").value).map(row => ({
    codigo: row.dataset.code,
    embalagem: row.querySelector('[name=embalagem]').value,
    preco: Number(row.querySelector('[name=preco]').value),
    estoque: Number(row.querySelector('[name=estoque]').value)
  }));
  byId('form-error').hidden = true;
  setSaving(true);
  try {
    const saved = await request(endpoint + (current ? '/' + current.id : ''), {
      method: current ? 'PUT' : 'POST',
      body: JSON.stringify(data)
    });
    items = [saved, ...items.filter(item => item.id !== saved.id)];
    byId('search').value = '';
    byId('filter').value = '';
    render();
    dialog.hidden = true;
    byId("catalog").hidden = false;
    byId('message').textContent = {
      PUBLICADO: 'Alterações publicadas no catálogo.',
      RASCUNHO: 'Rascunho salvo.',
      PAUSADO: 'Anúncio pausado.'
    }[saved.situacao];
  } catch (error) {
    byId('form-error').textContent = error.message;
    byId('form-error').hidden = false;
  } finally {
    setSaving(false);
  }
});

byId('search').addEventListener('input', render);
byId('filter').addEventListener('change', render);
byId('new').addEventListener('click', () => edit());
byId('close').addEventListener('click', fecharFormulario);
byId('cancel').addEventListener('click', fecharFormulario);

byId('add-variant').addEventListener('click', () => addVariant().querySelector('input').focus());
byId('rows').addEventListener('click', event => {
  const button = event.target.closest('[data-id]');
  if (button) edit(items.find(item => String(item.id) === button.dataset.id));
});


function fecharFormulario() {
  if (saving) return;
  dialog.hidden = true;
  byId('catalog').hidden = false;
}
function atualizarTamanhos() {
  const select = byId('primary-size');
  const anterior = select.value;
  select.replaceChildren();
  if (!variants.children.length) {
    select.add(new Option('Tamanho único', ''));
  } else {
    [...variants.children].forEach((row, index) => {
      select.add(new Option(row.querySelector('[name=embalagem]').value || 'Novo tamanho', String(index)));
    });
    if (variants.children.length === 1 && !variants.children[0].querySelector('[name=embalagem]').value) {
      select.options[0].textContent = 'Selecione um tamanho';
      for (const peso of ['1 kg', '3 kg', '10 kg', '15 kg', '20 kg']) select.add(new Option(peso, 'tamanho:' + peso));
    }
    if ([...select.options].some(option => option.value === anterior)) select.value = anterior;
  }
  atualizarPrecoPrincipal();
}
function atualizarPrecoPrincipal() {
  const row = variants.children[Number(byId('primary-size').value)];
  form.elements.precoPrincipal.value = row ? row.querySelector('[name=preco]').value : form.elements.precoAvulso.value;
}
byId('primary-size').addEventListener('change', () => {
  const valor = byId('primary-size').value;
  if (valor.startsWith('tamanho:')) {
    variants.children[0].querySelector('[name=embalagem]').value = valor.slice(8);
    atualizarTamanhos();
    byId('primary-size').value = '0';
  }
  atualizarPrecoPrincipal();
});
form.elements.precoPrincipal.addEventListener('input', () => {
  const row = variants.children[Number(byId('primary-size').value)];
  if (row) row.querySelector('[name=preco]').value = form.elements.precoPrincipal.value;
  else form.elements.precoAvulso.value = form.elements.precoPrincipal.value;
});
function atualizarFoto() {
  const url = form.elements.fotos.value.split(/\r?\n/)[0].trim();
  const image = byId('photo');
  image.hidden = !url;
  byId('photo-empty').hidden = !!url;
  if (url) image.src = url;
}
form.elements.fotos.addEventListener('input', atualizarFoto);
byId('change-photo').addEventListener('click', () => {
  byId('photo-editor').hidden = !byId('photo-editor').hidden;
  if (!byId('photo-editor').hidden) form.elements.fotos.focus();
});

init(async () => {
  items = await request(endpoint);
  render();
});
byId('primary-size').addEventListener('change', atualizarPrecoPrincipal);
form.elements.precoPrincipal.addEventListener('input', () => {
  const row = variants.children[byId('primary-size').value];
  if (row) row.querySelector('[name=preco]').value = form.elements.precoPrincipal.value;
  else form.elements.precoAvulso.value = form.elements.precoPrincipal.value;
});