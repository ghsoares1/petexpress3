const cartKey = 'carrinho';
const userKey = 'usuarioCadastrado';
const loggedKey = 'usuarioLogado';
const API_BASE_URL = window.PETEXPRESS_API_URL || 'http://localhost:8082';
const orderList = document.querySelector('.order-list');
const summaryValue = document.querySelector('.summary-value');
const checkoutForm = document.getElementById('checkoutForm');
const cardDetails = document.getElementById('cardDetails');
const messageBox = document.getElementById('messageBox');
const accountNotice = document.getElementById('accountNotice');
const savedAddressBlock = document.getElementById('savedAddressBlock');
const manualAddressFields = document.getElementById('manualAddressFields');
const addressExisting = document.getElementById('addressExisting');
const addressNew = document.getElementById('addressNew');
const authModal = document.getElementById('authModal');
const closeModalBtn = document.getElementById('closeModalBtn');
const cardMethodsSection = document.getElementById('cardMethodsSection');
const savedCardArea = document.getElementById('savedCardArea');
const savedCardsList = document.getElementById('savedCardsList');
const cardChoiceSaved = document.getElementById('cardChoiceSaved');
const cardChoiceNew = document.getElementById('cardChoiceNew');
const botaoFinalizar =
  document.getElementById('btn-finalizar') ||
  document.querySelector('.confirm-btn') ||
  checkoutForm?.querySelector('button[type="submit"]');
const deliveryFeeRow = document.getElementById('deliveryFeeRow');
const deliveryFeeValue = document.querySelector('.delivery-fee-value');
const uberInfo = document.getElementById('uberInfo');
const uberCodeEl = document.getElementById('uberCode');
const uberStatusPill = document.getElementById('uberStatusPill');
const uberFeeEl = document.getElementById('uberFee');
const uberEtaEl = document.getElementById('uberEta');
const uberDistanceEl = document.getElementById('uberDistance');
let currentDeliveryFee = 0;
let currentDeliveryMethod = null;
let currentDeliveryCode = null;
let currentUberDelivery = null;

console.log('checkout-js carregado');
console.log('checkoutForm encontrado', checkoutForm);
console.log('botão finalizar encontrado', botaoFinalizar);

function setCheckoutButtonState(loading) {
  if (!botaoFinalizar) return;
  botaoFinalizar.disabled = loading;
  botaoFinalizar.textContent = loading ? 'Redirecionando...' : 'Finalizar compra';
}

async function createPaymentPreference(cart) {
  const payload = cart.map((item) => ({
    title: item.nome || item.name || 'Produto',
    quantity: Number(item.quantidade || item.quantity || 1),
    price: Number(item.preco || item.price || 0),
  }));

  if (currentDeliveryMethod === 'Entrega via Uber' && currentDeliveryFee > 0) {
    payload.push({
      title: 'Taxa de entrega Uber',
      quantity: 1,
      price: Number(currentDeliveryFee),
    });
  }

  const response = await fetch(`${API_BASE_URL}/api/pagamento/criar-preferencia`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Falha ao criar preferência de pagamento: ${response.status} ${errorText}`);
  }

  return response.json();
}

async function handleCheckoutSubmit(event) {
  if (event) event.preventDefault();
  console.log('clique em finalizar detectado');

  const cart = getCart();
  if (!cart.length) {
    showMessage('Seu carrinho está vazio. Adicione produtos antes de finalizar.', false);
    return;
  }

  const loggedUser = getLoggedUser();
  if (!loggedUser) {
    openModal();
    return;
  }

  if (!validateForm()) {
    return;
  }

  setCheckoutButtonState(true);

  try {
    const data = await createPaymentPreference(cart);

    if (!data || !data.init_point) {
      throw new Error('Resposta inválida do servidor. Não foi possível encontrar init_point.');
    }

    window.location.href = data.init_point;
  } catch (error) {
    console.error('Erro ao finalizar pagamento:', error);
    alert('Não foi possível finalizar o pagamento no momento. Por favor, tente novamente mais tarde.');
    showMessage('Erro ao processar o pagamento. Veja o console para mais detalhes.', false);
    setCheckoutButtonState(false);
  }
}

if (botaoFinalizar) {
  botaoFinalizar.addEventListener('click', (event) => {
    console.log('clique em finalizar detectado');
    handleCheckoutSubmit(event);
  });
}

checkoutForm.addEventListener('submit', handleCheckoutSubmit);



function formatPrice(value) {
  const amount = Number(value || 0);
  return `R$ ${amount.toFixed(2).replace('.', ',')}`;
}

function getCart() {
  try {
    const stored = localStorage.getItem(cartKey);
    return stored ? JSON.parse(stored) : [];
  } catch (error) {
    console.error('Erro ao ler carrinho:', error);
    return [];
  }
}

function getSavedUser() {
  try {
    const raw = localStorage.getItem(userKey);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch (error) {
    console.error('Erro ao ler usuário salvo:', error);
    return null;
  }
}

function getLoggedUser() {
  try {
    const raw = localStorage.getItem(loggedKey);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch (error) {
    console.error('Erro ao ler usuário logado:', error);
    return null;
  }
}

function getSafeValue(user, ...keys) {
  for (const key of keys) {
    if (user[key]) return user[key];
  }
  return '';
}

function renderOrderSummary() {
  const cart = getCart();
  orderList.innerHTML = '';

  if (!cart.length) {
    orderList.innerHTML = '<p class="empty-order">Seu carrinho está vazio. Volte à página inicial para adicionar produtos.</p>';
    summaryValue.textContent = formatPrice(0);
    return;
  }

  let total = 0;

  cart.forEach((item) => {
    const itemTotal = item.preco * item.quantidade;
    total += itemTotal;

    const orderItem = document.createElement('div');
    orderItem.className = 'order-item';
    orderItem.innerHTML = `
      <div class="item-info">
        <span class="item-name">${item.nome}</span>
        <span>Qtd: ${item.quantidade}</span>
      </div>
      <strong>${formatPrice(itemTotal)}</strong>
    `;

    orderList.appendChild(orderItem);
  });

  const totalWithDelivery = total + Number(currentDeliveryFee || 0);

  if (deliveryFeeRow) {
    deliveryFeeRow.classList.toggle('hidden', !currentDeliveryFee);
  }
  if (deliveryFeeValue) {
    deliveryFeeValue.textContent = formatPrice(currentDeliveryFee);
  }

  summaryValue.textContent = formatPrice(totalWithDelivery);
}

function generateUberCode() {
  return `UBER-${Math.floor(10000 + Math.random() * 90000)}`;
}

function getCheckoutAddressLine() {
  const choice = checkoutForm?.addressChoice?.value;

  if (choice === 'existing') {
    const user = getSavedUser();
    const street = getSafeValue(user || {}, 'address', 'endereco', 'endereço', 'Address');
    const number = getSafeValue(user || {}, 'number', 'numero', 'Número', 'Number');
    const neighborhood = getSafeValue(user || {}, 'neighborhood', 'bairro', 'Neighborhood');
    if (street) {
      return `${street}${number ? `, ${number}` : ''}${neighborhood ? ` - ${neighborhood}` : ''}`;
    }
  }

  const street = checkoutForm?.address?.value?.trim();
  const number = checkoutForm?.number?.value?.trim();
  const neighborhood = checkoutForm?.neighborhood?.value?.trim();
  if (street) {
    return `${street}${number ? `, ${number}` : ''}${neighborhood ? ` - ${neighborhood}` : ''}`;
  }

  return 'Endereço do checkout';
}

function createUberDeliverySimulation() {
  const address = getCheckoutAddressLine();
  const distance = Number((2.2 + Math.random() * 5.6).toFixed(1));
  const eta = Math.round(16 + distance * 4 + Math.random() * 8);
  const fee = Number((7.9 + distance * 2.2).toFixed(2));

  return {
    id: generateUberCode(),
    status: 'Cotação criada',
    fee,
    eta,
    distance,
    pickup: 'PetExpress - Ipiranga',
    dropoff: address,
    createdAt: new Date().toISOString(),
  };
}

function renderUberSimulation() {
  if (!currentUberDelivery) return;

  currentDeliveryFee = Number(currentUberDelivery.fee || 0);
  currentDeliveryCode = currentUberDelivery.id;

  if (uberCodeEl) uberCodeEl.textContent = currentUberDelivery.id;
  if (uberFeeEl) uberFeeEl.textContent = formatPrice(currentDeliveryFee);
  if (uberEtaEl) uberEtaEl.textContent = `${currentUberDelivery.eta} min`;
  if (uberDistanceEl) {
    uberDistanceEl.textContent = `${currentUberDelivery.distance.toFixed(1).replace('.', ',')} km`;
  }
  if (uberStatusPill) uberStatusPill.textContent = 'Disponível';

  try {
    localStorage.setItem('uberDeliverySimulation', JSON.stringify(currentUberDelivery));
  } catch (error) {
    console.warn('Não foi possível salvar a cotação Uber:', error);
  }

  renderOrderSummary();
}

function resetUberSimulation() {
  currentUberDelivery = null;
  currentDeliveryFee = 0;
  currentDeliveryCode = null;
  try {
    localStorage.removeItem('uberDeliverySimulation');
  } catch (error) {
    console.warn('Não foi possível limpar a cotação Uber:', error);
  }
}

function updateDeliverySelection() {
  const selected = document.querySelector('input[name="delivery"]:checked')?.value || null;
  currentDeliveryMethod = selected;

  if (selected === 'Entrega via Uber') {
    currentUberDelivery = createUberDeliverySimulation();
    uberInfo?.classList.remove('hidden');
    uberInfo?.setAttribute('aria-hidden', 'false');
    renderUberSimulation();
    return;
  }

  resetUberSimulation();
  uberInfo?.classList.add('hidden');
  uberInfo?.setAttribute('aria-hidden', 'true');
  renderOrderSummary();
}

function fillSavedAddress(user) {
  if (!user) return;

  checkoutForm.fullName.value = getSafeValue(user, 'fullName', 'nome', 'name', 'Nome');
  checkoutForm.phone.value = getSafeValue(user, 'phone', 'telefone', 'Phone');
  checkoutForm.cep.value = getSafeValue(user, 'cep', 'CEP');
  checkoutForm.address.value = getSafeValue(user, 'address', 'endereco', 'endereço', 'Address');
  checkoutForm.number.value = getSafeValue(user, 'number', 'numero', 'Número', 'Number');
  checkoutForm.complement.value = getSafeValue(user, 'complement', 'complemento', 'Complemento');
  checkoutForm.neighborhood.value = getSafeValue(user, 'neighborhood', 'bairro', 'Neighborhood');

  const street = getSafeValue(user, 'address', 'endereco', 'endereço', 'Address');
  const number = getSafeValue(user, 'number', 'numero', 'Número', 'Number');
  const complement = getSafeValue(user, 'complement', 'complemento', 'Complemento');
  const neighborhood = getSafeValue(user, 'neighborhood', 'bairro', 'Neighborhood');
  const cep = getSafeValue(user, 'cep', 'CEP');

  const addressLine = street ? `${street}${number ? `, nº ${number}` : ''}` : 'Endereço não informado';
  const neighborhoodLine = neighborhood ? ` - ${neighborhood}` : '';

  savedAddressBlock.innerHTML = `
    <div class="saved-address-header">
      <div>
        <strong>Endereço de entrega</strong>
        <p class="saved-address-caption">Usando endereço cadastrado</p>
      </div>
      <button type="button" class="link-button" id="alterarEnderecoBtn">Alterar endereço</button>
    </div>
    <div class="saved-address-line saved-address-main">
      <span class="saved-address-icon">📍</span>
      <span>${addressLine}${neighborhoodLine}</span>
    </div>
    <div class="saved-address-line saved-address-small">
      <span>CEP</span>
      <span>${cep}</span>
    </div>
    ${complement ? `<div class="saved-address-line saved-address-small"><span>Complemento</span><span>${complement}</span></div>` : ''}
  `;
}

function updateAddressView() {
  const loggedUser = getLoggedUser();
  const user = getSavedUser();
  const choice = checkoutForm.addressChoice.value;

  if (choice === 'existing' && loggedUser && user) {
    savedAddressBlock.classList.remove('hidden');
    manualAddressFields.classList.add('hidden');
    fillSavedAddress(user);
  } else {
    savedAddressBlock.classList.add('hidden');
    manualAddressFields.classList.remove('hidden');
  }
}

function showMessage(text, success = true) {
  messageBox.textContent = text;
  messageBox.classList.add('show');
  messageBox.style.background = success ? '#eafaf1' : '#fdecea';
  messageBox.style.color = success ? '#155724' : '#721c24';
  messageBox.style.borderColor = success ? '#c3e6cb' : '#f5c6cb';
}

function validateForm() {
  const loggedUser = getLoggedUser();
  const user = getSavedUser();

  const addressChoice = checkoutForm.addressChoice.value;

  if (addressChoice === 'existing') {
    if (!loggedUser || !user) {
      showMessage('Para usar o endereço cadastrado, você precisa estar logado.', false);
      return false;
    }
  }

  if (addressChoice === 'new') {
    const requiredFields = [
      'fullName',
      'phone',
      'cep',
      'address',
      'number',
      'neighborhood'
    ];

    for (const fieldName of requiredFields) {
      const field = checkoutForm[fieldName];
      if (!field.value.trim()) {
        field.focus();
        showMessage('Por favor, preencha os campos do endereço.', false);
        return false;
      }
    }
  }

  if (!checkoutForm.delivery.value) {
    showMessage('Escolha uma forma de entrega.', false);
    return false;
  }

  return true;
}

function openModal() {
  authModal.classList.remove('hidden');
}

function closeModal() {
  authModal.classList.add('hidden');
}

Array.from(checkoutForm.addressChoice).forEach((radio) => {
  radio.addEventListener('change', () => {
    updateAddressView();
    if (currentDeliveryMethod === 'Entrega via Uber') {
      currentUberDelivery = createUberDeliverySimulation();
      renderUberSimulation();
    }
  });
});

document.querySelectorAll('#deliveryOptions input[name="delivery"]').forEach((radio) => {
  radio.addEventListener('change', updateDeliverySelection);
});

['address', 'number', 'neighborhood'].forEach((fieldName) => {
  checkoutForm?.[fieldName]?.addEventListener('input', () => {
    if (currentDeliveryMethod !== 'Entrega via Uber') return;
    currentUberDelivery = createUberDeliverySimulation();
    renderUberSimulation();
  });
});

savedAddressBlock?.addEventListener('click', (event) => {
  if (event.target.closest('#alterarEnderecoBtn')) {
    if (addressNew) addressNew.checked = true;
    if (addressExisting) addressExisting.checked = false;
    updateAddressView();
  }
});

closeModalBtn.addEventListener('click', closeModal);

authModal.addEventListener('click', (event) => {
  if (event.target === authModal) {
    closeModal();
  }
});

function updateUserState() {
  const loggedUser = getLoggedUser();
  const savedUser = getSavedUser();

  if (loggedUser) {
    accountNotice.classList.add('hidden');
    if (savedUser) {
      addressExisting.disabled = false;
      if (!addressExisting.checked && !addressNew.checked) {
        addressExisting.checked = true;
        addressNew.checked = false;
      }
    } else {
      addressExisting.disabled = true;
      addressExisting.checked = false;
      addressNew.checked = true;
    }
  } else {
    accountNotice.classList.remove('hidden');
    addressExisting.disabled = true;
    addressExisting.checked = false;
    addressNew.checked = true;
  }
}

function initializePage() {
  updateUserState();
  updateAddressView();
  updateDeliverySelection();
  renderOrderSummary();
}

initializePage();
