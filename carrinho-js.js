const CART_KEY = 'carrinho';
const cartItemsElement = document.querySelector('.cart-items');
const totalValueElement = document.querySelector('.total-value');
const emptyStateElement = document.querySelector('.empty-cart');
const recommendationsElement = document.querySelector('.recommendations');
const recommendationsSection = document.querySelector('.recommendations-section');
const checkoutBtn = document.querySelector('.checkout-btn');
const API_BASE_URL = window.PETEXPRESS_API_URL || 'http://localhost:8082';
let cart = [];
let products = [];

function formatPrice(value) {
  const amount = Number(value || 0);
  return `R$ ${amount.toFixed(2).replace('.', ',')}`;
}

function getItemImage(item) {
  return item.imagem || item.image || item.img || item.urlImagem || item.foto || 'img/c1.png';
}

function normalizeCartItem(item) {
  return {
    ...item,
    imagem: getItemImage(item),
  };
}

function getProductId(product) {
  return product.id ?? product._id ?? product.codigo ?? product.nome;
}

function loadCart() {
  try {
    const stored = localStorage.getItem(CART_KEY);
    cart = stored ? JSON.parse(stored).map(normalizeCartItem) : [];
  } catch (error) {
    console.error('Erro ao carregar carrinho:', error);
    cart = [];
  }
}

function saveCart() {
  localStorage.setItem(CART_KEY, JSON.stringify(cart));
}

function updateTotal() {
  const total = cart.reduce((sum, item) => sum + item.preco * item.quantidade, 0);
  totalValueElement.textContent = formatPrice(total);
  checkoutBtn.disabled = cart.length === 0;
}

function renderCartItems() {
  cartItemsElement.innerHTML = '';

  if (!cart.length) {
    emptyStateElement.classList.remove('hidden');
    updateTotal();
    return;
  }

  emptyStateElement.classList.add('hidden');

  cart.forEach((item) => {
    const imageUrl = getItemImage(item);
    const card = document.createElement('div');
    card.className = 'cart-item';
    card.innerHTML = `
      <div class="cart-item-image">
        <img src="${imageUrl}" alt="${item.nome}" onerror="this.src='img/c1.png'">
      </div>
      <div class="item-details">
        <span class="item-name">${item.nome}</span>
        <span class="item-price">${formatPrice(item.preco)}</span>
        <div class="item-controls">
          <button class="qty-btn" data-action="decrease" data-id="${item.id}">-</button>
          <span class="item-quantity">${item.quantidade}</span>
          <button class="qty-btn" data-action="increase" data-id="${item.id}">+</button>
        </div>
      </div>
    `;

    cartItemsElement.appendChild(card);
  });

  cartItemsElement.querySelectorAll('.qty-btn').forEach((button) => {
    button.addEventListener('click', () => {
      const id = button.dataset.id;
      const action = button.dataset.action;
      updateQuantity(id, action === 'increase' ? 1 : -1);
    });
  });

  updateTotal();
}

function updateQuantity(id, delta) {
  const index = cart.findIndex((item) => String(item.id) === String(id));
  if (index === -1) return;

  const nextQuantity = cart[index].quantidade + delta;
  if (nextQuantity <= 0) {
    cart.splice(index, 1);
  } else {
    cart[index].quantidade = nextQuantity;
  }

  saveCart();
  renderCartItems();
}

function renderRecommendations() {
  recommendationsElement.innerHTML = '';

  if (!products.length) {
    recommendationsSection.classList.add('hidden');
    return;
  }

  recommendationsSection.classList.remove('hidden');
  const popular = products.slice(0, 6);

  popular.forEach((product) => {
    const productId = getProductId(product);
    const card = document.createElement('div');
    card.className = 'recommendation-card';
    card.innerHTML = `
      <img src="${product.imagem || 'img/c1.png'}" alt="${product.nome}">
      <div class="recommendation-copy">
        <span class="recommendation-name">${product.nome}</span>
        <span class="recommendation-price">${formatPrice(product.preco)}</span>
      </div>
      <button class="add-btn" data-id="${productId}">Adicionar</button>
    `;

    recommendationsElement.appendChild(card);
  });

  recommendationsElement.querySelectorAll('.add-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      const id = btn.dataset.id;
      const product = products.find((item) => String(getProductId(item)) === String(id));
      if (product) {
        addToCart(product);
      }
    });
  });
}

function addToCart(product) {
  const id = getProductId(product);
  const existing = cart.find((item) => String(item.id) === String(id));

  if (existing) {
    existing.quantidade += 1;
  } else {
    cart.push({
      id,
      nome: product.nome,
      preco: Number(product.preco || product.valor || 0),
      imagem: product.imagem || product.image || product.img || product.urlImagem || product.foto || 'img/c1.png',
      quantidade: 1,
    });
  }

  saveCart();
  renderCartItems();
}

async function fetchProducts() {
  try {
    const response = await fetch(`${API_BASE_URL}/produtos`);
    if (!response.ok) throw new Error('Erro na API');
    products = await response.json();
    renderRecommendations();
  } catch (error) {
    console.error('Não foi possível carregar recomendações:', error);
    recommendationsSection.classList.add('hidden');
  }
}

checkoutBtn.addEventListener('click', () => {
  if (!cart.length) return;
  window.location.href = 'checkout.html';
});

loadCart();
renderCartItems();
fetchProducts();

