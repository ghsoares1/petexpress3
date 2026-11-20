(() => {
  const icons = {"home":"<path d=\"m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z\"/>","product":"<rect x=\"4\" y=\"5\" width=\"16\" height=\"16\" rx=\"2\"/><path d=\"M8 5V3h8v2M4 10h16M10 14h4\"/>","stock":"<path d=\"m3 7 9-4 9 4v14H3zM3 11h18M9 16h6M9 3v4h6V3\"/>","orders":"<path d=\"M3 3h3l2 12h11l2-9H6M9 20h.01M18 20h.01\"/>","sales":"<rect x=\"4\" y=\"4\" width=\"16\" height=\"16\" rx=\"2\"/><path d=\"M8 16v-4M12 16V8M16 16v-6\"/>","report":"<rect x=\"4\" y=\"4\" width=\"16\" height=\"16\" rx=\"2\"/><path d=\"M8 8h8M8 12h3M8 16h3M15 12v4\"/>","gear":"<path d=\"m9 3-1 3-3 1 1 3-2 2 2 2-1 3 3 1 1 3h6l1-3 3-1-1-3 2-2-2-2 1-3-3-1-1-3z\"/><circle cx=\"12\" cy=\"12\" r=\"3\"/>","promo":"<path d=\"M3 3h9l9 9-9 9-9-9z\"/><circle cx=\"7\" cy=\"7\" r=\"1\"/>","logout":"<path d=\"M9 3H4v18h5M9 12h12m-4-4 4 4-4 4\"/>","shop":"<path d=\"m4 3-2 6h20l-2-6zM4 9v12h16V9M9 21v-7h6v7\"/>"};
  icons.paw = '<ellipse cx="6" cy="8" rx="2.4" ry="3.3" fill="currentColor" stroke="none"/><ellipse cx="11" cy="5.5" rx="2.3" ry="3" fill="currentColor" stroke="none"/><ellipse cx="17" cy="7" rx="2.3" ry="3.2" fill="currentColor" stroke="none"/><ellipse cx="20" cy="12" rx="2" ry="2.8" fill="currentColor" stroke="none"/><path d="M6 17c0-3 4-7 7-7s6 5 6 8-4 2-6 2-7 1-7-3" fill="currentColor" stroke="none"/>';
  const icon = name => '<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">' + icons[name] + '</svg>';
  const paginas = [
    ['vendedor-resumo.html','Início','home'],
    ['vendedor.html','Produtos','product'],
    ['vendedor-estoque.html','Estoque','stock'],
    ['vendedor-pedidos.html','Pedidos','orders'],
    ['vendedor-vendas.html','Vendas','sales'],
    ['vendedor-relatorios.html','Relatórios','report'],
    ['vendedor-promocoes.html','Promoções','promo'],
    ['vendedor-configuracoes.html','Configurações','gear']
  ];
  const header = document.querySelector('.seller-header');
  header.outerHTML = `
    <aside class="seller-sidebar">
      <a class="seller-wordmark" href="vendedor-resumo.html" aria-label="PetExpress Início">
        <span class="seller-paw" aria-hidden="true">${icon('paw')}</span><span>PetExpress<small>Seu pet é a nossa paixão</small></span>
      </a>
      <nav id="seller-nav" aria-label="Painel do vendedor" hidden>
        ${paginas.map(([url,nome,icone]) => `<a href="${url}" ${url === location.pathname.split('/').pop() ? 'aria-current="page"' : ''}>${icon(icone)}<span>${nome}</span></a>`).join('')}
      </nav>
      <button id="seller-logout" class="seller-sidebar-logout" type="button">${icon('logout')}<span>Sair</span></button>
    </aside>
    <header class="seller-topbar">
      <button id="seller-menu" class="seller-menu-button" type="button" aria-label="Abrir menu" aria-expanded="false">☰</button>
      <div class="seller-topbar-group">
        <div class="seller-dropdown">
          <button class="seller-shop seller-dropdown-trigger" id="seller-shop-toggle" type="button" aria-expanded="false" aria-controls="seller-shop-dropdown">
            <span class="seller-shop-symbol">${icon('shop')}</span>
            <span class="seller-topbar-copy"><small>Sua loja</small><strong id="seller-shop-name">PetExpress</strong></span>
            <span class="seller-chevron" aria-hidden="true">⌄</span>
          </button>
          <div class="seller-dropdown-panel" id="seller-shop-dropdown" hidden>
            <a href="index.html">${icon('shop')}Visitar a loja</a>
            <a href="vendedor-configuracoes.html">${icon('gear')}Dados da loja</a>
          </div>
        </div>
        <span class="seller-topbar-divider" aria-hidden="true"></span>
        <div class="seller-dropdown">
          <button class="seller-user seller-dropdown-trigger" id="seller-account-toggle" type="button" aria-expanded="false" aria-controls="seller-account-dropdown">
            <span class="seller-avatar" id="seller-account-avatar">—</span>
            <span class="seller-topbar-copy"><strong id="seller-account-name">Minha conta</strong><small>Conta do vendedor</small></span>
            <span class="seller-chevron" aria-hidden="true">⌄</span>
          </button>
          <div class="seller-dropdown-panel seller-account-panel" id="seller-account-dropdown" hidden>
            <div class="seller-dropdown-identity"><strong id="seller-account-fullname">Minha conta</strong><span id="seller-account-email"></span></div>
            <a href="vendedor-configuracoes.html?aba=conta">${icon('home')}Meu perfil</a>
            <a href="vendedor-configuracoes.html">${icon('gear')}Configurações da loja</a>
            <button type="button" id="seller-account-logout">${icon('logout')}Sair da conta</button>
          </div>
        </div>
      </div>
    </header>`;
  const menu = document.getElementById('seller-menu');
  menu.addEventListener('click', () => {
    const aberto = document.body.classList.toggle('seller-menu-open');
    menu.setAttribute('aria-expanded', String(aberto));
  });

  const dropdowns = ['shop','account'].map(name => ({
    trigger: document.getElementById('seller-' + name + '-toggle'),
    panel: document.getElementById('seller-' + name + '-dropdown')
  }));
  function fecharMenus() {
    dropdowns.forEach(({trigger,panel}) => {
      trigger.setAttribute('aria-expanded','false');
      panel.hidden = true;
    });
  }
  dropdowns.forEach(({trigger,panel}) => {
    trigger.addEventListener('click', () => {
      const abrir = panel.hidden;
      fecharMenus();
      panel.hidden = !abrir;
      trigger.setAttribute('aria-expanded',String(abrir));
    });
  });
  document.addEventListener('click', event => {
    if (!event.target.closest('.seller-dropdown')) fecharMenus();
  });
  document.addEventListener('keydown', event => {
    if (event.key !== 'Escape') return;
    const aberto = dropdowns.find(({panel}) => !panel.hidden);
    fecharMenus();
    aberto?.trigger.focus();
  });
})();
