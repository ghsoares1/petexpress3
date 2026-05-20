(function () {
  const localHosts = ['localhost', '127.0.0.1', ''];
  const localApiUrl = 'http://localhost:8082';

  // Depois de criar o backend no Render, troque esta URL se o Render gerar outro nome.
  const productionApiUrl = 'https://petexpress-site-backend.onrender.com';

  window.PETEXPRESS_API_URL =
    window.PETEXPRESS_API_URL ||
    (localHosts.includes(window.location.hostname) ? localApiUrl : productionApiUrl);
})();
