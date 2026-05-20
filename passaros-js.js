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
  if (!alertBox) return;
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

