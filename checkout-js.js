const { escapar } = window.PETEXPRESS_CATALOGO;
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

const uberStatusPill = document.getElementById('uberStatusPill');
const uberFeeEl = document.getElementById('uberFee');
const uberEtaEl = document.getElementById('uberEta');

let currentDeliveryFee = 0;
let currentDeliveryMethod = null;
let currentDeliveryCode = null;
let currentUberDelivery = null;
let checkoutInProgress = false;
let cupomAplicado = null;
let descontoCupom = 0;

function itensDoCheckout(cart) {
  return cart.map(item => ({
    produtoId: item.catalogoId || (/^\d+$/.test(String(item.id)) ? Number(item.id) : null),
    variacaoCodigo: item.variacaoCodigo,
    nome: item.nome,
    quantidade: Number(item.quantidade || 1),
    preco: Number(item.preco)
  }));
}

document.getElementById('coupon-apply').addEventListener('click', async () => {
  const button = document.getElementById('coupon-apply');
  const message = document.getElementById('coupon-message');
  const user = getLoggedUser();
  if (!user?.token) {
    message.textContent = 'Entre na sua conta para aplicar o cupom.';
    return;
  }
  const codigo = document.getElementById('coupon-code').value.trim();
  if (!codigo) { message.textContent = 'Digite o código do cupom.'; return; }
  button.disabled = true;
  try {
    const response = await fetch(API_BASE_URL + '/api/pagamento/orcamento', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + user.token },
      body: JSON.stringify({ itens: itensDoCheckout(getCart()), cupom: codigo })
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || 'Não foi possível aplicar o cupom.');
    cupomAplicado = data.cupom;
    descontoCupom = Number(data.desconto);
    document.getElementById('coupon-code').readOnly = true;
    document.getElementById('coupon-remove').hidden = false;
    message.textContent = 'Cupom ' + data.cupom + ' aplicado.';
    renderOrderSummary();
  } catch (error) {
    message.textContent = error.message;
  } finally {
    button.disabled = false;
  }
});

document.getElementById('coupon-remove').addEventListener('click', () => {
  cupomAplicado = null;
  descontoCupom = 0;
  document.getElementById('coupon-code').readOnly = false;
  document.getElementById('coupon-remove').hidden = true;
  document.getElementById('coupon-message').textContent = '';
  renderOrderSummary();
});

console.log('checkout-js carregado');
console.log('checkoutForm encontrado', checkoutForm);
console.log('botão finalizar encontrado', botaoFinalizar);

function setCheckoutButtonState(loading) {
  if (!botaoFinalizar) return;
  botaoFinalizar.disabled = loading;
  botaoFinalizar.textContent = loading ? 'Redirecionando...' : 'Finalizar compra';
}

async function createPaymentPreference(cart, loggedUser) {
  const fields = checkoutForm.elements;
  const endereco = [
    getCheckoutAddressLine(),
    fields.complement.value.trim(),
    fields.cep.value.trim() ? 'CEP ' + fields.cep.value.trim() : ''
  ].filter(Boolean).join(' · ');
  const payload = {
    formaEntrega: fields.delivery.value,
    cotacaoId: currentUberDelivery?.id,
    cliente: fields.fullName.value.trim() || [loggedUser.nome, loggedUser.sobrenome].filter(Boolean).join(' '),
    enderecoEntrega: endereco,
    cupom: cupomAplicado,
    itens: itensDoCheckout(cart)
  };
  const signature = JSON.stringify([loggedUser.id, payload]);
  const stored = JSON.parse(sessionStorage.getItem('petexpressCheckout') || 'null');
  payload.chave = stored?.signature === signature ? stored.chave : crypto.randomUUID();
  sessionStorage.setItem('petexpressCheckout', JSON.stringify({ signature, chave: payload.chave }));

  const response = await fetch(`${API_BASE_URL}/api/pagamento/criar-preferencia`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + loggedUser.token },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    if ([410, 502].includes(response.status)) sessionStorage.removeItem('petexpressCheckout');
    const data = await response.json().catch(() => ({}));
    throw new Error(data.message || 'Não foi possível iniciar o pagamento.');
  }
  const data = await response.json();
  sessionStorage.setItem('petexpressPedidoCheckout', JSON.stringify({ pedido: data.pedidoId, carrinho: JSON.stringify(cart) }));
  return data;
}

async function handleCheckoutSubmit(event) {
  if (event) event.preventDefault();
  if (checkoutInProgress) return;

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

  checkoutInProgress = true;
  setCheckoutButtonState(true);

  try {
    const data = await createPaymentPreference(cart, loggedUser);

    if (!data || !data.init_point) {
      throw new Error('Resposta inválida do servidor. Não foi possível encontrar init_point.');
    }

    window.location.href = data.init_point;
  } catch (error) {
    console.error('Erro ao finalizar pagamento:', error);
    showMessage(error.message || "Erro ao processar o pagamento.", false);
    checkoutInProgress = false;
    setCheckoutButtonState(false);
  }
}

if (botaoFinalizar) {
  botaoFinalizar.addEventListener('click', (event) => {
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
        <span class="item-name">${escapar(item.nome)}</span>
        <span>Qtd: ${escapar(item.quantidade)}</span>
      </div>
      <strong>${formatPrice(itemTotal)}</strong>
    `;

    orderList.appendChild(orderItem);
  });

  const totalWithDelivery = total + Number(currentDeliveryFee || 0) - descontoCupom;
  document.getElementById('coupon-discount-row').hidden = !descontoCupom;
  document.getElementById('coupon-discount').textContent = '− ' + formatPrice(descontoCupom);

  if (deliveryFeeRow) {
    deliveryFeeRow.classList.toggle('hidden', !currentDeliveryFee);
  }
  if (deliveryFeeValue) {
    deliveryFeeValue.textContent = formatPrice(currentDeliveryFee);
  }

  summaryValue.textContent = formatPrice(totalWithDelivery);
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


let quoteVersion = 0;

function invalidarCotacao() {
  quoteVersion++;
  currentUberDelivery = null;
  currentDeliveryFee = 0;
  currentDeliveryCode = null;
  document.getElementById('uberQuoteResult').classList.add('hidden');
  document.getElementById('uberQuoteMessage').textContent = '';
  uberStatusPill.textContent = 'Consulte o frete';
  renderOrderSummary();
}

function updateDeliverySelection() {
  currentDeliveryMethod = checkoutForm.elements.delivery.value;
  invalidarCotacao();
  const uber = currentDeliveryMethod === 'Entrega via Uber';
  uberInfo.classList.toggle('hidden', !uber);
  uberInfo.setAttribute('aria-hidden', String(!uber));
  if (uber) {
    checkoutForm.elements.uberNome.value = checkoutForm.elements.fullName.value || getLoggedUser()?.nome || '';
    checkoutForm.elements.uberEndereco.value = getCheckoutAddressLine();
    checkoutForm.elements.uberCep.value = checkoutForm.elements.cep.value;
    checkoutForm.elements.uberTelefone.value = checkoutForm.elements.phone.value;
  }
}

document.getElementById('uberQuoteButton').addEventListener('click', async () => {
  const message = document.getElementById('uberQuoteMessage');
  const usuario = getLoggedUser();
  if (!usuario?.token) { openModal(); return; }
  let telefone = checkoutForm.elements.uberTelefone.value.replace(/[^0-9]/g, '');
  if (telefone.length === 10 || telefone.length === 11) telefone = '55' + telefone;
  const destino = {
    nome: checkoutForm.elements.uberNome.value.trim(),
    telefone: '+' + telefone,
    endereco: {
      rua: checkoutForm.elements.uberEndereco.value.trim(),
      cidade: checkoutForm.elements.uberCidade.value.trim(),
      estado: checkoutForm.elements.uberUF.value.trim().toUpperCase(),
      cep: checkoutForm.elements.uberCep.value.trim()
    }
  };
  const version = quoteVersion;
  const button = document.getElementById('uberQuoteButton');
  button.disabled = true;
  message.textContent = 'Consultando a Uber Direct…';
  try {
    const response = await fetch(API_BASE_URL + '/api/entregas/cotacoes', {
      method: 'POST',
      headers: {'Content-Type':'application/json', Authorization:'Bearer ' + usuario.token},
      body: JSON.stringify(destino)
    });
    const quote = await response.json();
    if (!response.ok) throw new Error(quote.message || 'Não foi possível consultar o frete.');
    if (version !== quoteVersion) return;
    currentUberDelivery = quote;
    currentDeliveryFee = Number(quote.valor);
    currentDeliveryCode = quote.id;
    uberFeeEl.textContent = formatPrice(quote.valor);
    uberEtaEl.textContent = quote.minutos == null ? 'A confirmar' : quote.minutos + ' min';
    document.getElementById('uberExpires').textContent = new Date(quote.expiraEm).toLocaleTimeString('pt-BR',{hour:'2-digit',minute:'2-digit'});
    document.getElementById('uberQuoteResult').classList.remove('hidden');
    uberStatusPill.textContent = 'Cotação recebida';
    message.textContent = 'Valor informado pela Uber Direct.';
    renderOrderSummary();
  } catch(error) { if (version === quoteVersion) message.textContent = error.message; }
  finally {button.disabled = false;}
});

async function verificarIntegracoes() {
  const radio = document.querySelector('input[value="Entrega via Uber"]');
  const aviso = document.getElementById('uberAvailability');
  try {
    const response = await fetch(API_BASE_URL + '/api/integracoes/status');
    if (!response.ok) throw new Error();
    const status = await response.json();
    radio.disabled = !status.entregaConfigurada;
    aviso.textContent = status.entregaConfigurada ? 'Consulte a disponibilidade Uber Direct para seu endereço.' : 'Uber Direct indisponível: a loja ainda precisa configurar a conta de entregas.';
    if (!status.pagamentoConfigurado) showMessage('O pagamento ainda precisa ser configurado pela loja.', false);
  } catch {
    aviso.textContent = 'Não foi possível verificar o serviço de entrega.';
  }
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
      <span>${escapar(addressLine)}${escapar(neighborhoodLine)}</span>
    </div>
    <div class="saved-address-line saved-address-small">
      <span>CEP</span>
      <span>${escapar(cep)}</span>
    </div>
    ${complement ? `<div class="saved-address-line saved-address-small"><span>Complemento</span><span>${escapar(complement)}</span></div>` : ''}
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

  if (checkoutForm.elements.delivery.value === 'Entrega via Uber'
      && (!currentUberDelivery || new Date(currentUberDelivery.expiraEm) <= new Date())) {
    showMessage('Consulte o frete Uber Direct antes de finalizar a compra.', false);
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
    invalidarCotacao();
  });
});

document.querySelectorAll('#deliveryOptions input[name="delivery"]').forEach((radio) => {
  radio.addEventListener('change', updateDeliverySelection);
});

['address','number','neighborhood','cep','fullName','phone','uberNome','uberEndereco','uberCidade','uberUF','uberCep','uberTelefone'].forEach(fieldName => {
  checkoutForm.elements[fieldName]?.addEventListener('input', invalidarCotacao);
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
verificarIntegracoes();
