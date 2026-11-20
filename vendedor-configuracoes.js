const form = byId('settings-form');
const profileForm = byId('profile-form');
const storeFeedback = byId('store-feedback');
const profileFeedback = byId('profile-feedback');

function selecionarAba(aba) {
  const selecionada = ['loja','conta','integracoes'].includes(aba) ? aba : 'loja';
  for (const nome of ['loja','conta','integracoes']) {
    byId('settings-' + nome).hidden = nome !== selecionada;
    byId('tab-' + nome).toggleAttribute('aria-current', nome === selecionada);
    if (nome === selecionada) byId('tab-' + nome).setAttribute('aria-current','page');
  }
}
for (const aba of ['loja','conta','integracoes']) {
  byId('tab-' + aba).addEventListener('click', event => {
    event.preventDefault();
    history.pushState(null,'','?aba=' + aba);
    selecionarAba(aba);
  });
}
window.addEventListener('popstate', () => selecionarAba(new URLSearchParams(location.search).get('aba')));

function atualizarLoja(dados) {
  byId('store-preview-name').textContent = dados.nome;
  byId('store-monogram').textContent = dados.nome.slice(0,1).toLocaleUpperCase('pt-BR');
}
function atualizarPerfil(perfil) {
  byId('profile-fullname').textContent = [perfil.nome,perfil.sobrenome].filter(Boolean).join(' ');
  byId('profile-email').textContent = perfil.email;
  byId('profile-monogram').textContent = [perfil.nome?.[0],perfil.sobrenome?.[0]].filter(Boolean).join('').toLocaleUpperCase('pt-BR');
}
function feedback(elemento, texto, erro = false) {
  elemento.textContent = texto;
  elemento.classList.toggle('seller-feedback-error', erro);
}
async function carregar() {
  const dados = await request('/configuracoes');
  for (const campo of ['nome','telefone','endereco']) form.elements[campo].value = dados[campo];
  for (const campo of ['nome','sobrenome','email','cpf','endereco','bairro','cep','complemento']) {
    profileForm.elements[campo].value = sellerProfile[campo] || '';
  }
  const status = await requestApi('/api/integracoes/status');
  byId('payment-integration').textContent = !status.pagamentoConfigurado ? 'Credencial ainda não configurada.'
    : status.retornoPublico ? 'Credencial e URL de retorno configuradas.' : 'Configure a URL HTTPS pública para retorno e notificações.';
  byId('delivery-integration').textContent = status.entregaConfigurada ? 'Credenciais e ponto de coleta configurados.' : 'Aguardando conta de produção e dados do ponto de coleta.';
  atualizarLoja(dados);
  atualizarPerfil(sellerProfile);
  selecionarAba(new URLSearchParams(location.search).get('aba'));
  if ([...profileForm.querySelectorAll('details input[required]')].some(input => !input.value)) {
    profileForm.querySelector('details').open = true;
  }
}
form.elements.nome.addEventListener('input', () => atualizarLoja({nome:form.elements.nome.value}));
form.addEventListener('submit', async event => {
  event.preventDefault();
  const button = form.querySelector('button[type=submit]');
  button.disabled = true;
  feedback(storeFeedback,'Salvando…');
  try {
    const loja = await request('/configuracoes',{method:'PUT',body:JSON.stringify(Object.fromEntries(new FormData(form)))});
    byId('shop-name').textContent = loja.nome;
    atualizarLoja(loja);
    feedback(storeFeedback,'Alterações salvas.');
  } catch(error) { feedback(storeFeedback,error.message,true); }
  finally {button.disabled = false;}
});
profileForm.addEventListener('invalid', () => {
  profileForm.querySelector('details').open = true;
},true);
profileForm.addEventListener('submit', async event => {
  event.preventDefault();
  const button = profileForm.querySelector('button[type=submit]');
  button.disabled = true;
  feedback(profileFeedback,'Salvando…');
  try {
    const perfil = await requestApi('/api/usuarios/' + readSession().id, {
      method:'PUT',body:JSON.stringify(Object.fromEntries(new FormData(profileForm)))
    });
    sellerProfile = perfil;
    const sessao = readSession();
    localStorage.setItem('usuarioCadastrado',JSON.stringify(perfil));
    localStorage.setItem('usuarioLogado',JSON.stringify({
      ...sessao,nome:perfil.nome,sobrenome:perfil.sobrenome,email:perfil.email
    }));
    atualizarIdentidade(perfil);
    atualizarPerfil(perfil);
    feedback(profileFeedback,'Perfil atualizado.');
  } catch(error) {feedback(profileFeedback,error.message,true);}
  finally {button.disabled = false;}
});
byId('profile-logout').addEventListener('click',sairDaConta);
byId('payment-test').addEventListener('click', async () => {
  const button = byId('payment-test');
  const message = byId('payment-diagnostic');
  button.disabled = true;
  message.textContent = 'Verificando a conta no Mercado Pago…';
  try {
    const result = await request('/integracoes/mercadopago');
    message.textContent = result.mensagem;
  } catch(error) {message.textContent = error.message;}
  finally {button.disabled = false;}
});
init(carregar);
