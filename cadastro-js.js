const API_BASE_URL = window.PETEXPRESS_API_URL || 'http://localhost:8082';

document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('form-cadastro');
  const modal = document.getElementById('signupSuccessModal');
  const goToLoginBtn = document.getElementById('goToLoginBtn');
  const submitButton = form?.querySelector('button[type="submit"]');

  function showModal() {
    modal.classList.remove('hidden');
  }

  function redirectToLogin() {
    window.location.href = 'login.html';
  }

  function setLoading(loading) {
    if (!submitButton) return;
    submitButton.disabled = loading;
    submitButton.textContent = loading ? 'Cadastrando...' : 'Cadastrar';
  }

  async function readError(response) {
    const fallback = 'Nao foi possivel concluir a operacao.';
    const contentType = response.headers.get('Content-Type') || '';

    if (contentType.includes('application/json')) {
      try {
        const data = await response.json();
        return data.message || data.error || fallback;
      } catch (error) {
        return fallback;
      }
    }

    try {
      const text = await response.text();
      return text ? text : fallback;
    } catch (error) {
      return fallback;
    }
  }

  if (!form) {
    return;
  }

  form.addEventListener('submit', async function(event) {
    event.preventDefault();

    const nome = document.getElementById('nome').value.trim();
    const sobrenome = document.getElementById('sobrenome').value.trim();
    const cpf = document.getElementById('cpf').value.trim();
    const email = document.getElementById('email').value.trim();
    const senha = document.getElementById('senha').value;
    const confirmar = document.getElementById('confirmar').value;
    const endereco = document.getElementById('endereco').value.trim();
    const complemento = document.getElementById('complemento').value.trim();
    const bairro = document.getElementById('bairro').value.trim();
    const cep = document.getElementById('cep').value.trim();

    if (!nome || !sobrenome || !cpf || !email || !senha || !confirmar || !endereco || !bairro || !cep) {
      alert('Preencha todos os campos obrigatorios antes de continuar.');
      return;
    }

    if (senha !== confirmar) {
      alert('As senhas nao coincidem!');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/usuarios/cadastrar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          nome,
          sobrenome,
          cpf,
          email,
          senha,
          endereco,
          complemento,
          bairro,
          cep
        })
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      localStorage.removeItem('usuarioCadastrado');
      localStorage.removeItem('usuarioLogado');
      showModal();
      setTimeout(redirectToLogin, 3000);
    } catch (error) {
      alert(error.message || 'Erro ao cadastrar usuario.');
    } finally {
      setLoading(false);
    }
  });

  if (goToLoginBtn) {
    goToLoginBtn.addEventListener('click', redirectToLogin);
  }
});

