const form = document.getElementById('form-cadastro');
const mensagemAviso = document.getElementById('mensagem-aviso');
const API_BASE_URL = window.PETEXPRESS_API_URL || 'http://localhost:8082';

function getAuthHeaders() {
  try {
    const loggedUser = JSON.parse(localStorage.getItem('usuarioLogado') || 'null');
    return loggedUser?.token ? { Authorization: `Bearer ${loggedUser.token}` } : {};
  } catch (error) {
    return {};
  }
}

form.addEventListener('submit', (e) => {
  e.preventDefault();

  const nome = form['nome'].value.trim();
  const tipoProduto = form['tipo-produto'].value;
  const tipoAnimal = form['tipo-animal'].value;
  const preco = form['preco'].value.trim();
  const codigoBarras = form['codigo-barras'].value.trim();

  if (!nome || !tipoProduto || !tipoAnimal || !preco || !codigoBarras) {
    alert('Por favor, preencha todos os campos obrigatorios.');
    return;
  }

  if (Number.isNaN(Number(preco)) || Number(preco) < 0) {
    alert('Informe um valor valido para o preco.');
    return;
  }

  const produto = {
    nome,
    tipoProduto,
    tipoAnimal,
    preco: Number(preco),
    codigoBarras,
    descricao: form['descricao'].value.trim(),
    imagem: form['imagem'].value.trim()
  };

  fetch(`${API_BASE_URL}/produtos`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
    body: JSON.stringify(produto)
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`Erro HTTP ${response.status}`);
      }
      return response.json();
    })
    .then((data) => {
      mensagemAviso.textContent = `Produto "${data.nome || nome}" cadastrado com sucesso.`;
      mensagemAviso.classList.add('show');
      form.reset();
      setTimeout(() => mensagemAviso.classList.remove('show'), 4000);
    })
    .catch((error) => {
      console.error('Erro ao conectar com o servidor:', error);
      alert('Erro ao conectar com o servidor. Verifique a conexao ou o console para mais detalhes.');
    });
});

