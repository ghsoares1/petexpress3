const stockForm = byId('stock-form');
const stockDialog = byId('stock-dialog');
const historyDialog = byId('history-dialog');
const endpoint = '/estoque';
const stockStates = { BAIXO: 'Estoque baixo', ESGOTADO: 'Esgotado', DISPONIVEL: 'Disponível' };
const movementLabels = {
  RESERVA: 'Reserva para pedido', LIBERACAO: 'Reserva liberada', VENDA: 'Venda confirmada',
  SALDO_INICIAL: 'Saldo inicial', ENTRADA: 'Entrada', SAIDA: 'Saída', AJUSTE: 'Ajuste de contagem'
};
const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
let saldos = [];
let selected;
let mode;
let saving = false;
let historyActive;

function stockStatus(item) {
  if (item.estoque === 0) return 'ESGOTADO';
  return item.estoque <= item.estoqueMinimo ? 'BAIXO' : 'DISPONIVEL';
}

function normalized(text) {
  return text.toLocaleLowerCase('pt-BR').normalize('NFD').replace(/\p{Diacritic}/gu, '');
}

function renderStock() {
  const query = normalized(byId('stock-search').value.trim());
  const filter = byId('stock-filter').value;
  const filtered = saldos.filter(item => normalized(item.produto + ' ' + item.embalagem).includes(query)
    && (!filter || stockStatus(item) === filter));
  const empty = byId('stock-empty');
  empty.hidden = filtered.length > 0;
  empty.textContent = saldos.length ? 'Nenhuma embalagem corresponde aos filtros.'
    : 'Nenhuma embalagem cadastrada. Adicione embalagens na aba Produtos.';
  byId('stock-rows').innerHTML = filtered.map(item => {
    const status = stockStatus(item);
    const label = escapar(item.produto + ' · ' + item.embalagem);
    return `
      <tr data-codigo="${escapar(item.codigo)}">
        <td><strong>${escapar(item.produto)}</strong></td>
        <td class="seller-stock-size">${escapar(item.embalagem)}</td>
        <td>${item.estoque}</td>
        <td>${item.reservado}</td>
        <td>${item.estoqueMinimo}</td>
        <td><span class="seller-status ${status}">${stockStates[status]}</span></td>
        <td><div class="seller-stock-buttons">
          <button class="seller-secondary" type="button" data-action="movement" aria-label="Movimentar ${label}">Movimentar</button>
          <button class="seller-secondary" type="button" data-action="minimum" aria-label="Definir mínimo de ${label}">Mínimo</button>
          <button class="seller-secondary" type="button" data-action="history" aria-label="Histórico de ${label}">Histórico</button>
        </div></td>
      </tr>`;
  }).join('');
}

async function loadStock() {
  saldos = await request(endpoint);
  renderStock();
}

function showBalance() {
  byId('stock-balance').textContent = 'Saldo atual: ' + selected.estoque + ' un.';
}

function preview() {
  const tipo = stockForm.elements.tipo.value;
  const field = stockForm.elements.quantidade;
  field.min = tipo === 'AJUSTE' ? '0' : '1';
  byId('stock-quantity-label').textContent = tipo === 'AJUSTE' ? 'Saldo contado' : 'Quantidade de unidades';
  const quantidade = field.valueAsNumber;
  const saldo = { ENTRADA: selected.estoque + quantidade, SAIDA: selected.estoque - quantidade, AJUSTE: quantidade }[tipo];
  byId('stock-preview').textContent = !field.value ? '' : saldo < 0
    ? 'A saída ultrapassa o saldo disponível.'
    : 'Saldo após a movimentação: ' + saldo + ' un.';
}

function setSaving(active) {
  saving = active;
  const movement = mode === 'movement';
  for (const control of stockForm.elements) {
    control.disabled = active || (control.name === 'minimo' && movement)
      || (['tipo', 'quantidade', 'motivo'].includes(control.name) && !movement);
  }
}

function showForm(item, action) {
  selected = item;
  mode = action;
  stockForm.reset();
  const movement = action === 'movement';
  byId('stock-title').textContent = movement ? 'Registrar movimentação' : 'Definir estoque mínimo';
  byId('stock-product').textContent = item.produto + ' · ' + item.embalagem;
  byId('stock-error').hidden = true;
  byId('stock-movement').hidden = !movement;
  byId('stock-minimum').hidden = movement;
  stockForm.elements.minimo.value = item.estoqueMinimo;
  showBalance();
  preview();
  setSaving(false);
  stockDialog.showModal();
}

stockForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (saving) return;
  const movement = mode === 'movement';
  const data = movement ? {
    tipo: stockForm.elements.tipo.value,
    quantidade: stockForm.elements.quantidade.valueAsNumber,
    motivo: stockForm.elements.motivo.value.trim(),
    saldoEsperado: selected.estoque
  } : { quantidade: stockForm.elements.minimo.valueAsNumber };
  byId('stock-error').hidden = true;
  setSaving(true);
  try {
    const updated = await request(endpoint + '/' + selected.produtoId + '/' + encodeURIComponent(selected.codigo)
      + (movement ? '/movimentacoes' : '/minimo'), { method: movement ? 'POST' : 'PUT', body: JSON.stringify(data) });
    saldos = saldos.map(item => item.codigo === updated.codigo ? updated : item);
    stockDialog.close();
    renderStock();
    byId('message').textContent = movement
      ? 'Movimentação registrada. Saldo atual: ' + updated.estoque + ' un.'
      : 'Estoque mínimo atualizado.';
  } catch (error) {
    if (error.status === 409) {
      await loadStock().catch(() => {});
      const refreshed = saldos.find(item => item.codigo === selected.codigo);
      if (refreshed) {
        selected = refreshed;
        showBalance();
        preview();
      }
    }
    byId('stock-error').textContent = error.message;
    byId('stock-error').hidden = false;
  } finally {
    setSaving(false);
  }
});

async function loadHistory(state) {
  const more = byId('history-more');
  const message = byId('history-message');
  const rows = byId('history-rows');
  const params = new URLSearchParams();
  if (state.codigo) params.set('codigo', state.codigo);
  if (state.next) params.set('antesDe', state.next);
  more.disabled = true;
  message.textContent = 'Carregando movimentações…';
  try {
    const data = await request(endpoint + '/movimentacoes?' + params);
    if (historyActive !== state) return;
    rows.insertAdjacentHTML('beforeend', data.movimentacoes.map(item => `
      <li>
        <div class="seller-history-line">
          <strong>${escapar(movementLabels[item.tipo] || item.tipo)} · ${escapar(item.produto)} · ${escapar(item.embalagem)}</strong>
          <time datetime="${escapar(item.registradoEm)}">${dateFormat.format(new Date(item.registradoEm))}</time>
        </div>
        <p>${escapar(item.motivo)}</p>
        <p>Saldo de <strong>${item.saldoAnterior}</strong> para <strong>${item.saldoAtual} un.</strong> · ${escapar(item.responsavel)}</p>
      </li>`).join(''));
    state.next = data.proximaPagina;
    more.hidden = !state.next;
    message.textContent = rows.children.length ? '' : 'Nenhuma movimentação registrada.';
  } catch (error) {
    if (historyActive !== state) return;
    message.textContent = error.message;
    more.hidden = false;
  } finally {
    if (historyActive === state) more.disabled = false;
  }
}

function showHistory(item) {
  historyActive = { codigo: item?.codigo, next: null };
  byId('history-product').textContent = item ? item.produto + ' · ' + item.embalagem : 'Todas as embalagens';
  byId('history-rows').replaceChildren();
  byId('history-more').hidden = true;
  historyDialog.showModal();
  loadHistory(historyActive);
}

stockForm.elements.tipo.addEventListener('change', preview);
stockForm.elements.quantidade.addEventListener('input', preview);
byId('stock-close').addEventListener('click', () => stockDialog.close());
stockDialog.addEventListener('cancel', event => { if (saving) event.preventDefault(); });
byId('stock-search').addEventListener('input', renderStock);
byId('stock-filter').addEventListener('change', renderStock);
byId('history-close').addEventListener('click', () => historyDialog.close());
historyDialog.addEventListener('close', () => { historyActive = null; });
byId('history-all').addEventListener('click', () => showHistory());
byId('history-more').addEventListener('click', () => loadHistory(historyActive));
byId('stock-rows').addEventListener('click', event => {
  const button = event.target.closest('[data-action]');
  if (!button) return;
  const item = saldos.find(saldo => saldo.codigo === button.closest('tr').dataset.codigo);
  if (button.dataset.action === 'history') showHistory(item);
  else showForm(item, button.dataset.action);
});

init(loadStock);
