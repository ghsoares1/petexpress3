const CART_KEY = 'carrinho';

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
  alertBox.classList.remove('hidden');
  alertBox.classList.add('show');

  setTimeout(() => {
    alertBox.classList.remove('show');
    alertBox.classList.add('hidden');
  }, 2500);
}

function parsePrice(priceText) {
  const cleaned = priceText.replace(/\D/g, '');
  return Number(cleaned.slice(0, -2) + '.' + cleaned.slice(-2));
}

function addToCart(button) {
  if (!button) return;
  const card = button.closest('.product-card');
  if (!card) return;

  const nome = card.querySelector('h2')?.textContent.trim() || 'Produto';
  const priceText = card.querySelector('.price')?.textContent || 'R$ 0,00';
  const preco = parsePrice(priceText);
  const imagem = card.querySelector('img')?.src || card.querySelector('img')?.getAttribute('src') || 'img/c1.png';
  const id = nome;

  const cart = getCart();
  const existing = cart.find((item) => String(item.id) === String(id));

  if (existing) {
    existing.quantidade += 1;
  } else {
    cart.push({
      id,
      nome,
      preco,
      imagem,
      quantidade: 1,
    });
  }

  saveCart(cart);
  showAlert();
}

const API_BASE_URL = window.PETEXPRESS_API_URL || 'http://localhost:8082';
const productCount = document.getElementById('productCount');

function getProductId(produto) {
  return produto.id ?? produto.codigo ?? produto.nome;
}

function formatPrice(value) {
  return `R$ ${Number(value || 0).toFixed(2).replace('.', ',')}`;
}

async function carregarProdutos(tipoAnimal) {
  const container = document.querySelector('#productGrid');
  if (!container) return;

  try {
    const response = await fetch(`${API_BASE_URL}/api/produtos/categoria/${tipoAnimal}`);
    if (!response.ok) throw new Error('Erro ao buscar produtos');
    
    const produtosRecebidos = await response.json();
    const produtos = produtosRecebidos.filter((produto, index, lista) =>
      lista.findIndex((item) => item.nome === produto.nome) === index
    );
    
    container.innerHTML = '';
    if (productCount) productCount.textContent = String(produtos.length);
    
    produtos.forEach(produto => {
      const card = document.createElement('div');
      card.className = 'product-card';
      card.innerHTML = `
        <div class="product-media">
          <span class="discount-badge">Oferta</span>
          <img src="${produto.imagem || 'img/1.jpg'}" alt="${produto.nome}">
        </div>
        <div class="product-info">
          <span class="product-brand">${produto.tipoProduto || 'Marca parceira'}</span>
          <h2>${produto.nome}</h2>
          <div class="product-tags">
            <span>Cães</span>
            <span>Ração seca</span>
          </div>
          <span class="price-label">À vista</span>
          <p class="price">${formatPrice(produto.preco)}</p>
          <p class="installments">Entrega rápida ou retirada conforme disponibilidade</p>
        </div>
        <button onclick="addToCart(this)">Comprar</button>
      `;
      card.dataset.id = getProductId(produto);
      container.appendChild(card);
    });
  } catch (error) {
    console.error('Erro ao carregar produtos dinamicos:', error);
    container.innerHTML = '<p style="text-align:center; width:100%; padding:24px;">Não foi possível carregar os produtos.</p>';
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const grid = document.querySelector('#productGrid');
  if (grid) {
    grid.innerHTML = '<p style="text-align:center; width:100%; padding:20px;">Carregando produtos...</p>';
  }
  carregarProdutos('CACHORRO');
});

