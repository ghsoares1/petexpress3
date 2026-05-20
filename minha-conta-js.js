const savedUserKey = 'usuarioCadastrado';
const loggedUserKey = 'usuarioLogado';
const API_BASE_URL = window.PETEXPRESS_API_URL || 'http://localhost:8082';

const accountForm = document.getElementById('accountForm');
const logoutBtn = document.getElementById('logoutBtn');
const accountFeedback = document.getElementById('accountFeedback');

function getSavedUser() {
  try {
    const raw = localStorage.getItem(savedUserKey);
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    return null;
  }
}

function getLoggedUser() {
  try {
    const raw = localStorage.getItem(loggedUserKey);
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    return null;
  }
}

function getAuthHeaders() {
  const loggedUser = getLoggedUser();
  return loggedUser?.token ? { Authorization: `Bearer ${loggedUser.token}` } : {};
}

function redirectToLogin() {
  window.location.href = 'login.html';
}

function populateAccountDetails(user) {
  if (!user) return;

  document.getElementById('nome').value = user.nome || '';
  document.getElementById('sobrenome').value = user.sobrenome || '';
  document.getElementById('email').value = user.email || '';
  document.getElementById('cpf').value = user.cpf || '';
  document.getElementById('endereco').value = user.endereco || '';
  document.getElementById('complemento').value = user.complemento || '';
  document.getElementById('bairro').value = user.bairro || '';
  document.getElementById('cep').value = user.cep || '';
}

async function readError(response) {
  try {
    const data = await response.json();
    return data.message || data.error || 'Nao foi possivel salvar os dados.';
  } catch (error) {
    return 'Nao foi possivel salvar os dados.';
  }
}

async function fetchCurrentUser(loggedUser) {
  if (!loggedUser?.id) {
    return getSavedUser();
  }

  const response = await fetch(`${API_BASE_URL}/api/usuarios/${loggedUser.id}`, {
    headers: getAuthHeaders()
  });
  if (!response.ok) {
    throw new Error(await readError(response));
  }

  const user = await response.json();
  localStorage.setItem(savedUserKey, JSON.stringify(user));
  return user;
}

async function saveUserData(event) {
  event.preventDefault();
  const loggedUser = getLoggedUser();
  const nome = document.getElementById('nome').value.trim();
  const sobrenome = document.getElementById('sobrenome').value.trim();
  const email = document.getElementById('email').value.trim();
  const cpf = document.getElementById('cpf').value.trim();
  const endereco = document.getElementById('endereco').value.trim();
  const complemento = document.getElementById('complemento').value.trim();
  const bairro = document.getElementById('bairro').value.trim();
  const cep = document.getElementById('cep').value.trim();

  if (!nome || !sobrenome || !email || !cpf || !endereco || !bairro || !cep) {
    accountFeedback.textContent = 'Preencha todos os campos obrigatorios antes de salvar.';
    accountFeedback.style.color = '#d12f24';
    return;
  }

  const updatedUser = {
    nome,
    sobrenome,
    email,
    cpf,
    endereco,
    complemento,
    bairro,
    cep
  };

  try {
    let savedUser = updatedUser;

    if (loggedUser?.id) {
      const response = await fetch(`${API_BASE_URL}/api/usuarios/${loggedUser.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(updatedUser)
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      savedUser = await response.json();
    }

    localStorage.setItem(savedUserKey, JSON.stringify(savedUser));

    const updatedLogged = {
      ...loggedUser,
      id: savedUser.id || loggedUser?.id,
      nome: savedUser.nome,
      sobrenome: savedUser.sobrenome,
      email: savedUser.email,
      token: loggedUser?.token
    };
    localStorage.setItem(loggedUserKey, JSON.stringify(updatedLogged));

    accountFeedback.textContent = 'Dados salvos com sucesso.';
    accountFeedback.style.color = '#1f7a33';
  } catch (error) {
    accountFeedback.textContent = error.message || 'Nao foi possivel salvar os dados.';
    accountFeedback.style.color = '#d12f24';
  }
}

function logout() {
  localStorage.removeItem(loggedUserKey);
  window.location.href = 'index.html';
}

async function initializePage() {
  const loggedUser = getLoggedUser();
  if (!loggedUser) {
    redirectToLogin();
    return;
  }

  try {
    const user = await fetchCurrentUser(loggedUser);
    populateAccountDetails(user);
  } catch (error) {
    const fallbackUser = getSavedUser();
    populateAccountDetails(fallbackUser);
    if (accountFeedback) {
      accountFeedback.textContent = error.message || 'Nao foi possivel carregar os dados atualizados.';
      accountFeedback.style.color = '#d12f24';
    }
  }
}

accountForm.addEventListener('submit', saveUserData);
logoutBtn.addEventListener('click', logout);

initializePage();

