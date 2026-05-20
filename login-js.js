const API_BASE_URL = window.PETEXPRESS_API_URL || 'http://localhost:8082';

function validateForm() {
  var email = document.getElementById("email").value;
  var senha = document.getElementById("senha").value;

  if (email.trim() === "") {
    alert("Por favor, insira seu e-mail.");
    return false;
  }

  if (senha.trim() === "") {
    alert("Por favor, insira sua senha.");
    return false;
  }

  return true;
}

document.addEventListener('DOMContentLoaded', function () {
  var loginForm = document.getElementById('login-form');
  var welcomeModal = document.getElementById('loginSuccessModal');
  var welcomeTitle = document.getElementById('welcomeTitle');
  var welcomeMessage = document.getElementById('welcomeMessage');
  var submitButton = loginForm?.querySelector('button[type="submit"]');

  function getRedirectTarget() {
    var params = new URLSearchParams(window.location.search);
    if (params.get('redirect') === 'checkout') {
      return 'checkout.html';
    }

    var referrer = document.referrer || '';
    if (referrer.indexOf('checkout.html') !== -1) {
      return 'checkout.html';
    }

    return 'index.html';
  }

  function showWelcomeModal(name) {
    welcomeTitle.textContent = 'Bem-vindo, ' + name + '!';
    welcomeMessage.textContent = 'Login realizado com sucesso. Redirecionando...';
    welcomeModal.classList.remove('hidden');
  }

  function setLoading(loading) {
    if (!submitButton) return;
    submitButton.disabled = loading;
    submitButton.textContent = loading ? 'Entrando...' : 'Entrar';
  }

  async function readError(response) {
    try {
      const data = await response.json();
      return data.message || data.error || 'E-mail ou senha incorretos.';
    } catch (error) {
      return 'E-mail ou senha incorretos.';
    }
  }

  if (!loginForm) {
    return;
  }

  loginForm.addEventListener('submit', async function (event) {
    event.preventDefault();

    if (!validateForm()) {
      return;
    }

    var email = document.getElementById('email').value.trim().toLowerCase();
    var senha = document.getElementById('senha').value;

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, senha })
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const data = await response.json();
      const usuario = data.usuario;

      var usuarioLogado = {
        id: usuario.id,
        nome: usuario.nome,
        sobrenome: usuario.sobrenome,
        email: usuario.email,
        token: data.token,
        loginAt: new Date().toISOString()
      };

      localStorage.setItem('usuarioLogado', JSON.stringify(usuarioLogado));
      localStorage.setItem('usuarioCadastrado', JSON.stringify(usuario));

      showWelcomeModal(usuario.nome);

      setTimeout(function () {
        window.location.href = getRedirectTarget();
      }, 2000);
    } catch (error) {
      alert(error.message || 'E-mail ou senha incorretos. Verifique seus dados e tente novamente.');
    } finally {
      setLoading(false);
    }
  });
});

