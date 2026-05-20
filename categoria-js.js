const CART_KEY = 'carrinho';
const API_BASE_URL = window.PETEXPRESS_API_URL || 'http://localhost:8082';
const categoryConfig = window.PETEXPRESS_CATEGORY || {};
const productGrid = document.getElementById('productGrid');
const productCount = document.getElementById('productCount');

function getCart() {
  try {
    const stored = localStorage.getItem(CART_KEY);
    return stored ? JSON.parse(stored) : [];
  } catch (error) {
    console.error('Erro ao carregar carrinho:', error);
    return [];
  }
}

function saveCart(cart) {
  localStorage.setItem(CART_KEY, JSON.stringify(cart));
}

function showAlert() {
  const alertBox = document.getElementById('alert');
  if (!alertBox) return;
  alertBox.classList.remove('hidden');
  alertBox.classList.add('show');

  setTimeout(() => {
    alertBox.classList.remove('show');
    alertBox.classList.add('hidden');
  }, 2500);
}

function formatPrice(value) {
  return `R$ ${Number(value || 0).toFixed(2).replace('.', ',')}`;
}

function getProductId(produto) {
  return produto.id ?? produto.codigo ?? produto.nome;
}

function addToCart(button) {
  if (!button) return;
  const card = button.closest('.product-card');
  if (!card) return;

  const produto = {
    id: card.dataset.id,
    nome: card.querySelector('h2')?.textContent.trim() || 'Produto',
    preco: Number(card.dataset.price || 0),
    imagem: card.querySelector('img')?.getAttribute('src') || 'img/c1.png',
    quantidade: 1
  };

  const cart = getCart();
  const existing = cart.find((item) => String(item.id) === String(produto.id));

  if (existing) {
    existing.quantidade = Number(existing.quantidade || 1) + 1;
  } else {
    cart.push(produto);
  }

  saveCart(cart);
  showAlert();
}

function normalizeProduct(produto) {
  return {
    id: getProductId(produto),
    nome: produto.nome || 'Produto PetExpress',
    tipoProduto: produto.tipoProduto || produto.marca || 'Marca parceira',
    tipoAnimal: produto.tipoAnimal || categoryConfig.tipoAnimal || categoryConfig.animalLabel || 'Pet',
    preco: Number(produto.preco || 0),
    imagem: produto.imagem || 'img/c1.png',
    tag: produto.tag || categoryConfig.productTag || 'Produto pet'
  };
}

function uniqueProducts(list) {
  return list.filter((produto, index, all) =>
    all.findIndex((item) => String(item.nome).toLowerCase() === String(produto.nome).toLowerCase()) === index
  );
}

function renderProducts(list) {
  if (!productGrid) return;
  const products = uniqueProducts(list.map(normalizeProduct));
  productGrid.innerHTML = '';
  if (productCount) productCount.textContent = String(products.length);

  if (!products.length) {
    productGrid.innerHTML = '<p class="empty-products">Nenhum produto encontrado.</p>';
    return;
  }

  products.forEach((produto) => {
    const card = document.createElement('div');
    card.className = 'product-card';
    card.dataset.id = produto.id;
    card.dataset.price = String(produto.preco);
    card.innerHTML = `
      <div class="product-media">
        <span class="discount-badge">Oferta</span>
        <img src="${produto.imagem}" alt="${produto.nome}">
      </div>
      <div class="product-info">
        <span class="product-brand">${produto.tipoProduto}</span>
        <h2>${produto.nome}</h2>
        <div class="product-tags">
          <span>${produto.tipoAnimal}</span>
          <span>${produto.tag}</span>
        </div>
        <span class="price-label">À vista</span>
        <p class="price">${formatPrice(produto.preco)}</p>
        <p class="installments">Entrega rápida ou retirada conforme disponibilidade</p>
      </div>
      <button onclick="addToCart(this)">Comprar</button>
    `;
    productGrid.appendChild(card);
  });
}

async function loadProducts() {
  if (!productGrid) return;
  productGrid.innerHTML = '<p class="empty-products">Carregando produtos...</p>';

  try {
    const staticProducts = Array.isArray(categoryConfig.products) ? categoryConfig.products : [];

    if (!categoryConfig.tipoAnimal && categoryConfig.mode !== 'all') {
      renderProducts(staticProducts);
      return;
    }

    const endpoint = categoryConfig.mode === 'all'
      ? `${API_BASE_URL}/produtos`
      : `${API_BASE_URL}/api/produtos/categoria/${categoryConfig.tipoAnimal}`;

    const response = await fetch(endpoint);
    if (!response.ok) throw new Error('Erro ao buscar produtos');
    const produtos = await response.json();
    renderProducts([...produtos, ...staticProducts]);
  } catch (error) {
    console.error('Erro ao carregar produtos:', error);
    if (Array.isArray(categoryConfig.products) && categoryConfig.products.length) {
      renderProducts(categoryConfig.products);
      return;
    }

    productGrid.innerHTML = '<p class="empty-products">Não foi possível carregar os produtos.</p>';
  }
}

document.addEventListener('DOMContentLoaded', loadProducts);
