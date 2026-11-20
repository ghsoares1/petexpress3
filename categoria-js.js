const { escapar, link } = window.PETEXPRESS_CATALOGO;
const categoryConfig = window.PETEXPRESS_CATEGORY;
const productGrid = document.getElementById('productGrid');
const productCount = document.getElementById('productCount');
const alertBox = document.getElementById('alert');
let alertTimer;

function showAlert(message) {
  alertBox.textContent = message;
  alertBox.setAttribute('role', 'status');
  alertBox.classList.remove('hidden');
  alertBox.classList.add('show');
  clearTimeout(alertTimer);
  alertTimer = setTimeout(() => {
    alertBox.classList.remove('show');
    alertBox.classList.add('hidden');
  }, 2500);
}

function formatPrice(value) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

function addToCart(produto, variacao) {
  let cart;
  try {
    cart = JSON.parse(localStorage.getItem('carrinho')) || [];
  } catch {
    cart = [];
  }
  const id = variacao ? produto.id + ':' + variacao.codigo : String(produto.id);
  const existing = cart.find(item => String(item.id) === id);
  const quantidade = Number(existing?.quantidade || 0) + 1;
  if (variacao && quantidade > variacao.estoque) {
    showAlert('Você já adicionou todo o estoque disponível desta embalagem.');
    return;
  }
  const item = {
    id,
    catalogoId: produto.id,
    variacaoCodigo: variacao?.codigo,
    nome: variacao ? produto.nome + ' · ' + variacao.embalagem : produto.nome,
    preco: variacao?.preco ?? produto.preco,
    imagem: produto.imagem || 'img/c1.png',
    quantidade
  };
  if (existing) Object.assign(existing, item);
  else cart.push(item);
  localStorage.setItem('carrinho', JSON.stringify(cart));
  showAlert('Produto adicionado ao carrinho!');
}

function renderProduct(produto) {
  const card = document.createElement('div');
  card.className = 'product-card';
  card.id = 'produto-' + produto.id;
  const variacoes = produto.variacoes || [];
  const fotos = produto.fotos || [];
  card.innerHTML = `
    <div class="product-media">
      <img src="${escapar(produto.imagem || 'img/c1.png')}" alt="${escapar(produto.nome)}" loading="lazy">
    </div>
    <div class="product-info">
      <span class="product-brand">${escapar(produto.tipoProduto || 'Marca parceira')}</span>
      <h2>${escapar(produto.nome)}</h2>
      <div class="product-tags">
        <span>${escapar(produto.categoria || categoryConfig.productTag)}</span>
      </div>
      ${produto.descricao ? `<p>${escapar(produto.descricao)}</p>` : ''}
      ${fotos.length > 1 ? `<div class="product-photos" aria-label="Fotos de ${escapar(produto.nome)}">
        ${fotos.map((foto, index) => `<button type="button" data-foto="${index}" aria-label="Ver foto ${index + 1}"><img src="${escapar(foto)}" alt=""></button>`).join('')}
      </div>` : ''}
      ${variacoes.length ? `<label for="embalagem-${produto.id}" class="product-option">Embalagem</label>
        <select id="embalagem-${produto.id}" class="product-variant">
          ${variacoes.map((v, index) => `<option value="${index}" ${v.estoque ? '' : 'disabled'}>${escapar(v.embalagem)}${v.estoque ? '' : ' · Esgotada'}</option>`).join('')}
        </select>` : ''}
      <span class="price-label">À vista</span>
      <p class="price" aria-live="polite"></p>
    </div>
    <button class="product-buy" type="button">Comprar</button>
  `;
  const select = card.querySelector('select');
  const button = card.querySelector('.product-buy');
  const price = card.querySelector('.price');
  function updatePrice() {
    const variacao = variacoes[select?.value];
    const disponivel = !variacoes.length || variacao?.estoque > 0;
    button.disabled = !disponivel;
    button.textContent = disponivel ? 'Comprar' : 'Esgotado';
    const atual = variacao?.preco ?? produto.preco;
    const original = variacao?.precoOriginal ?? produto.precoOriginal;
    price.innerHTML = disponivel
      ? (original > atual ? '<del class="original-price">' + formatPrice(original) + '</del> ' : '') + formatPrice(atual)
      : 'Indisponível';
  }
  select?.addEventListener('change', updatePrice);
  button.addEventListener('click', () => addToCart(produto, variacoes[select?.value]));
  card.querySelectorAll('[data-foto]').forEach(buttonFoto => {
    buttonFoto.addEventListener('click', () => {
      card.querySelector('.product-media img').src = fotos[buttonFoto.dataset.foto];
    });
  });
  updatePrice();
  return card;
}

async function loadProducts() {
  productGrid.innerHTML = '<p class="empty-products">Carregando produtos...</p>';
  try {
    const endpoint = categoryConfig.mode === 'all'
      ? '/produtos'
      : '/api/produtos/categoria/' + categoryConfig.tipoAnimal;
    const response = await fetch(window.PETEXPRESS_API_URL + endpoint, { cache: 'no-store' });
    if (!response.ok) throw new Error('Erro ao buscar produtos');
    const produtos = await response.json();
    productCount.textContent = String(produtos.length);
    if (!produtos.length) {
      productGrid.innerHTML = '<p class="empty-products">Nenhum produto encontrado.</p>';
      return;
    }
    productGrid.replaceChildren(...produtos.map(renderProduct));
    document.getElementById(location.hash.slice(1))?.scrollIntoView({ block: 'center' });
  } catch (error) {
    console.error(error);
    productGrid.innerHTML = '<p class="empty-products">Não foi possível carregar os produtos.</p>';
  }
}

document.addEventListener('DOMContentLoaded', loadProducts);
