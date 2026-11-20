(function () {
  window.PETEXPRESS_API_URL =
    window.PETEXPRESS_API_URL ||
    (window.location.protocol === 'file:' ? 'http://localhost:8082' : window.location.origin);
})();

window.PETEXPRESS_CATALOGO = {
  escapar(valor) {
    const caracteres = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
    return String(valor ?? '').replace(/[&<>"']/g, caractere => caracteres[caractere]);
  },
  link(id, pagina = 'todos.html') {
    return pagina + '#produto-' + encodeURIComponent(id);
  }
};
