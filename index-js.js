const maisVendidosContainer = document.getElementById("mais-vendidos-container");
const inputBusca = document.getElementById("busca");
const sugestoes = document.getElementById("sugestoes-busca");
const formBusca = document.getElementById("form-busca");
const API_BASE_URL = window.PETEXPRESS_API_URL || "http://localhost:8082";
const CART_KEY = 'carrinho';
const cartCount = document.getElementById('cartCount');
const menuToggle = document.getElementById('menuToggle');
const sideMenu = document.getElementById('sideMenu');
const sideMenuBackdrop = document.getElementById('sideMenuBackdrop');
const sideMenuClose = document.getElementById('sideMenuClose');
const sideAccountLink = document.getElementById('sideAccountLink');

let produtos = [];
let toastTimer = null;

function initHeroCarousel() {
  const slides = Array.from(document.querySelectorAll('.hero-slide'));
  const dots = Array.from(document.querySelectorAll('[data-hero-dot]'));
  const controls = document.querySelectorAll('[data-hero-control]');
  const carousel = document.getElementById('heroCarousel');

  if (!slides.length) return;

  let currentIndex = 0;
  let timer = null;
  let touchStartX = 0;
  let touchStartY = 0;

  function showSlide(index) {
    currentIndex = (index + slides.length) % slides.length;

    slides.forEach((slide, slideIndex) => {
      slide.classList.toggle('active', slideIndex === currentIndex);
    });

    dots.forEach((dot, dotIndex) => {
      dot.classList.toggle('active', dotIndex === currentIndex);
    });
  }

  function startAutoPlay() {
    stopAutoPlay();
    timer = setInterval(() => showSlide(currentIndex + 1), 5000);
  }

  function stopAutoPlay() {
    if (timer) clearInterval(timer);
  }

  controls.forEach((control) => {
    control.addEventListener('click', () => {
      const direction = control.dataset.heroControl === 'next' ? 1 : -1;
      showSlide(currentIndex + direction);
      startAutoPlay();
    });
  });

  dots.forEach((dot) => {
    dot.addEventListener('click', () => {
      showSlide(Number(dot.dataset.heroDot || 0));
      startAutoPlay();
    });
  });

  carousel?.addEventListener('mouseenter', stopAutoPlay);
  carousel?.addEventListener('mouseleave', startAutoPlay);
  carousel?.addEventListener('touchstart', (event) => {
    const touch = event.touches[0];
    touchStartX = touch.clientX;
    touchStartY = touch.clientY;
    stopAutoPlay();
  }, { passive: true });

  carousel?.addEventListener('touchend', (event) => {
    const touch = event.changedTouches[0];
    const deltaX = touch.clientX - touchStartX;
    const deltaY = touch.clientY - touchStartY;

    if (Math.abs(deltaX) > 45 && Math.abs(deltaX) > Math.abs(deltaY)) {
      showSlide(currentIndex + (deltaX < 0 ? 1 : -1));
    }

    startAutoPlay();
  }, { passive: true });
  showSlide(0);
  startAutoPlay();
}

function obterPaginaPorTipo(tipoAnimal) {
  const tipo = String(tipoAnimal || "").toLowerCase();

  if (tipo.includes("gato")) return "gatos.html";
  if (tipo.includes("cachorro") || tipo.includes("cao") || tipo.includes("cão")) return "caes.html";
  if (tipo.includes("pássaro") || tipo.includes("passaro") || tipo.includes("ave") || tipo.includes("aves")) return "passaros.html";
  if (tipo.includes("peixe") || tipo.includes("peixes") || tipo.includes("aquário") || tipo.includes("aquario")) return "peixes.html";

  return "produto.html";
}

function formatarPreco(valor) {
  return `R$ ${Number(valor || 0).toFixed(2).replace(".", ",")}`;
}

function getProductId(produto) {
  return produto.id ?? produto._id ?? produto.codigo ?? produto.nome;
}

function getCart() {
  try {
    const stored = localStorage.getItem(CART_KEY);
    return stored ? JSON.parse(stored) : [];
  } catch (error) {
    console.error('Erro ao ler carrinho:', error);
    return [];
  }
}

function saveCart(cart) {
  localStorage.setItem(CART_KEY, JSON.stringify(cart));
  updateCartCount();
}

function updateCartCount() {
  const total = getCart().reduce((sum, item) => sum + Number(item.quantidade || item.quantity || 1), 0);
  if (cartCount) cartCount.textContent = String(total);
}

function showHomeToast(message) {
  let toast = document.querySelector('.home-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.className = 'home-toast';
    document.body.appendChild(toast);
  }

  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 2200);
}

function addToCart(produto) {
  const cart = getCart();
  const id = getProductId(produto);
  const existing = cart.find((item) => String(item.id) === String(id));

  if (existing) {
    existing.quantidade = Number(existing.quantidade || 1) + 1;
  } else {
    cart.push({
      id,
      nome: produto.nome,
      preco: Number(produto.preco || 0),
      imagem: produto.imagem || 'img/c1.png',
      quantidade: 1
    });
  }

  saveCart(cart);
  showHomeToast('Produto adicionado ao carrinho.');
}

function renderizarProdutos(lista) {
  if (!maisVendidosContainer) return;
  maisVendidosContainer.innerHTML = "";

  if (lista.length === 0) {
    maisVendidosContainer.innerHTML = "<p>Nenhum produto encontrado.</p>";
    return;
  }

  lista.forEach((produto) => {
    const card = document.createElement("div");
    card.className = "card";

    card.innerHTML = `
      <div class="product-media">
        <span class="product-ribbon">Destaque</span>
        <img src="${produto.imagem || "img/c1.jpg"}" alt="${produto.nome}">
      </div>
      <div class="info">
        <h3>${produto.nome}</h3>
        <span class="badge">
          ${produto.tipoAnimal.charAt(0).toUpperCase() + produto.tipoAnimal.slice(1).toLowerCase()}
        </span>
        <p class="price">${formatarPreco(produto.preco)}</p>
        <div class="card-actions">
          <button class="view-product" type="button" data-action="view" data-id="${getProductId(produto)}">Comprar</button>
          <button class="add-cart-btn" type="button" data-action="add" data-id="${getProductId(produto)}" aria-label="Adicionar ${produto.nome} ao carrinho">
            <i class="fas fa-cart-plus"></i>
          </button>
        </div>
      </div>
    `;

    maisVendidosContainer.appendChild(card);
  });

  maisVendidosContainer.querySelectorAll('button[data-action]').forEach((button) => {
    button.addEventListener('click', (event) => {
      event.stopPropagation();
      const produto = produtos.find((item) => String(getProductId(item)) === String(button.dataset.id));
      if (!produto) return;

      if (button.dataset.action === 'add') {
        addToCart(produto);
        return;
      }

      window.location.href = obterPaginaPorTipo(produto.tipoAnimal);
    });
  });
}

async function carregarProdutos() {
  try {
    const resposta = await fetch(`${API_BASE_URL}/produtos`);

    if (!resposta.ok) {
      throw new Error("Erro ao buscar produtos.");
    }

    produtos = await resposta.json();
    const produtosUnicos = produtos.filter((produto, index, lista) =>
      lista.findIndex((item) => item.nome === produto.nome) === index
    );
    const maisVendidos = produtosUnicos.slice(0, 4);
    renderizarProdutos(maisVendidos);
  } catch (erro) {
    console.error("Erro:", erro);
    if (maisVendidosContainer) {
      maisVendidosContainer.innerHTML = "<p>Não foi possível carregar os produtos.</p>";
    }
  }
}

inputBusca.addEventListener("input", function () {
  const termo = this.value.trim().toLowerCase();
  sugestoes.innerHTML = "";

  if (!termo) {
    sugestoes.style.display = "none";
    return;
  }

  const resultados = produtos.filter((produto) =>
    produto.nome.toLowerCase().includes(termo)
  );

  if (resultados.length === 0) {
    sugestoes.style.display = "none";
    return;
  }

  resultados.forEach((produto) => {
    const li = document.createElement("li");

    li.innerHTML = `
      <img src="${produto.imagem || "img/c1.jpg"}" alt="${produto.nome}">
      <div class="info">
        <span class="nome">${produto.nome}</span>
        <span class="preco">${formatarPreco(produto.preco)}</span>
      </div>
    `;

    li.addEventListener("click", () => {
      window.location.href = obterPaginaPorTipo(produto.tipoAnimal);
    });

    sugestoes.appendChild(li);
  });

  sugestoes.style.display = "block";
});

formBusca.addEventListener("submit", function (e) {
  e.preventDefault();

  const termo = inputBusca.value.trim().toLowerCase();

  const resultado = produtos.find((produto) =>
    produto.nome.toLowerCase().includes(termo)
  );

  if (resultado) {
    window.location.href = obterPaginaPorTipo(resultado.tipoAnimal);
  }
});

document.addEventListener("click", function (e) {
  if (!e.target.closest("#box-busca-container")) {
    sugestoes.style.display = "none";
  }
});

function updateAccountLink() {
  const accountLink = document.getElementById('accountLink');
  const logged = localStorage.getItem('usuarioLogado');
  if (accountLink) {
    accountLink.href = logged ? 'minha-conta.html' : 'login.html';
  }
  if (sideAccountLink) {
    sideAccountLink.href = logged ? 'minha-conta.html' : 'login.html';
    sideAccountLink.textContent = logged ? 'Acessar minha conta' : 'Entre ou cadastre-se';
  }
}

function openSideMenu() {
  if (!sideMenu || !sideMenuBackdrop || !menuToggle) return;
  sideMenu.classList.add('open');
  sideMenu.setAttribute('aria-hidden', 'false');
  sideMenuBackdrop.hidden = false;
  menuToggle.setAttribute('aria-expanded', 'true');
  document.body.classList.add('side-menu-open');
}

function closeSideMenu() {
  if (!sideMenu || !sideMenuBackdrop || !menuToggle) return;
  sideMenu.classList.remove('open');
  sideMenu.setAttribute('aria-hidden', 'true');
  sideMenuBackdrop.hidden = true;
  menuToggle.setAttribute('aria-expanded', 'false');
  document.body.classList.remove('side-menu-open');
}

menuToggle?.addEventListener('click', openSideMenu);
sideMenuClose?.addEventListener('click', closeSideMenu);
sideMenuBackdrop?.addEventListener('click', closeSideMenu);
sideMenu?.querySelectorAll('a').forEach((link) => {
  link.addEventListener('click', () => {
    if (link.getAttribute('href') !== '#') closeSideMenu();
  });
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') closeSideMenu();
});

updateAccountLink();
updateCartCount();
initHeroCarousel();
carregarProdutos();

/* PetExpress Chatbot (frontend only) */
(function () {
  const toggle = document.getElementById('pet-chat-toggle');
  const box = document.getElementById('pet-chat-box');
  const closeBtn = document.getElementById('pet-chat-close');
  const body = document.getElementById('pet-chat-body');
  const quicks = document.querySelectorAll('.pet-quick');
  const input = document.getElementById('pet-chat-input');
  const send = document.getElementById('pet-chat-send');

  if (!toggle || !box || !body) return;

  const responses = {
    entrega: 'Nossas entregas dependem da sua região. Você pode ver prazos e valores na página do produto ou no checkout. Trabalhamos com parceiros confiáveis e despacho em até 48h úteis.',
    retirada: 'Sim — em muitos produtos você pode escolher a opção "Retirada na loja" durante o checkout. Verifique disponibilidade na página do produto ou no momento da compra.',
    pagamento: 'Sim. Aceitamos meios de pagamento com criptografia segura (SSL) e parceiros de pagamento reconhecidos. Seus dados financeiros não ficam armazenados neste site.',
    rastreio: 'Após o envio, você receberá um código de rastreamento por e-mail e poderá acompanhar pelo link do transportador ou na área "Meus Pedidos" quando estiver logado.',
    aves: 'Sim! O PetExpress também oferece produtos para pássaros e peixes. Você pode navegar nas categorias Pássaros e Peixes na home para conferir rações e acessórios.'
  };

  function openChat() {
    box.style.display = 'flex';
    box.setAttribute('aria-hidden', 'false');
    toggle.setAttribute('aria-expanded', 'true');
    setTimeout(() => input && input.focus(), 200);
  }

  function closeChat() {
    box.style.display = 'none';
    box.setAttribute('aria-hidden', 'true');
    toggle.setAttribute('aria-expanded', 'false');
  }

  function scrollToBottom() {
    body.scrollTop = body.scrollHeight;
  }

  function addMessage(text, who = 'bot') {
    const el = document.createElement('div');
    el.className = 'pet-chat-message ' + (who === 'user' ? 'user' : 'bot');
    el.textContent = text;
    body.appendChild(el);
    scrollToBottom();
  }

  toggle.addEventListener('click', function () {
    if (box.style.display === 'flex') closeChat(); else openChat();
  });

  closeBtn && closeBtn.addEventListener('click', closeChat);

  quicks.forEach((btn) => {
    btn.addEventListener('click', () => {
      const key = btn.getAttribute('data-key');
      addMessage(btn.textContent, 'user');
      const resp = responses[key] || 'Desculpe, não tenho essa resposta agora.';
      setTimeout(() => addMessage(resp, 'bot'), 350);
    });
  });

  send.addEventListener('click', () => {
    const text = (input.value || '').trim();
    if (!text) {
      addMessage('Por favor, selecione uma opção rápida ou digite sua dúvida.', 'bot');
      return;
    }
    addMessage(text, 'user');
    input.value = '';

    const lower = text.toLowerCase();
    const key = Object.keys(responses).find(k =>
      lower.includes(k) ||
      (k === 'entrega' && lower.includes('entrega')) ||
      (k === 'aves' && (lower.includes('pássaro') || lower.includes('passaro') || lower.includes('peixe') || lower.includes('peixes')))
    );

    const resp = responses[key] || 'Desculpe, não tenho essa resposta agora.';
    setTimeout(() => addMessage(resp, 'bot'), 350);
  });

  // ensure the widget does not interfere with existing site elements
  // start closed
  closeChat();
  // expose helper to open chat from other pages (footer link)
  window.openPetChat = function() {
    // if widget exists on this page, open it
    if (box && toggle) openChat();
  };
  window.closePetChat = function() {
    if (box && toggle) closeChat();
  };

  document.getElementById('open-chat-link')?.addEventListener('click', function(event) {
    event.preventDefault();
    openChat();
  });

  document.getElementById('service-chat-link')?.addEventListener('click', function(event) {
    event.preventDefault();
    openChat();
  });

  document.getElementById('sideChatLink')?.addEventListener('click', function(event) {
    event.preventDefault();
    closeSideMenu();
    openChat();
  });
})();

