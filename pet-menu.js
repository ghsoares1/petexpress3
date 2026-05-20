function carregarMenu() {
    const menuContainer = document.getElementById('pet-menu-container');

    if (!menuContainer) {
        return;
    }

    const menuHTML = `
    <button id="btn-abrir-menu" class="menu-burger" type="button" aria-label="Abrir menu">
      <span></span>
      <span></span>
      <span></span>
    </button>
    <nav id="menu-lateral" class="menu-escondido" aria-hidden="true">
      <div class="menu-cabecalho">
        <h2>Menu</h2>
        <button id="btn-fechar-menu" type="button" class="btn-fechar" aria-label="Fechar menu">&times;</button>
      </div>
      <ul class="menu-lista">
        <li><a href="index.html">Home</a></li>
        <li><a href="cadastro.html">Cadastro</a></li>
        <li><a href="caes.html">Cães</a></li>
        <li><a href="gatos.html">Gatos</a></li>
        <li><a href="sobre.html">Sobre</a></li>
        <li><a href="contato.html">Contato</a></li>
      </ul>
    </nav>
    `;

    menuContainer.innerHTML = menuHTML;

    const btnAbrir = document.getElementById('btn-abrir-menu');
    const btnFechar = document.getElementById('btn-fechar-menu');
    const menu = document.getElementById('menu-lateral');

    if (btnAbrir && menu) {
        btnAbrir.addEventListener('click', () => toggleMenu(menu, btnAbrir));
    }

    if (btnFechar && menu) {
        btnFechar.addEventListener('click', () => toggleMenu(menu, btnAbrir));
    }
}

function toggleMenu(menu, btnAbrir) {
    if (!menu) {
        return;
    }

    const opening = !menu.classList.contains('menu-visivel');
    menu.classList.toggle('menu-visivel', opening);
    menu.classList.toggle('menu-escondido', !opening);
    menu.setAttribute('aria-hidden', !opening);

    if (btnAbrir) {
        btnAbrir.style.display = opening ? 'none' : 'flex';
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', carregarMenu);
} else {
    carregarMenu();
}

