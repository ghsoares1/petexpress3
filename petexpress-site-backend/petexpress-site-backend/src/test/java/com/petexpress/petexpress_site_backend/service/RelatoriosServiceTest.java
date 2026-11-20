package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.model.*;
import com.petexpress.petexpress_site_backend.repository.PedidoRepository;
import com.petexpress.petexpress_site_backend.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RelatoriosServiceTest {
    @Test
    void usaValoresDaCompraEComparaPeriodosDeMesmaDuracao() {
        var pedidos = mock(PedidoRepository.class);
        var produtos = mock(ProdutoRepository.class);
        Pedido anterior = pedido("2025-01-01T12:00:00", 100.0, 0.0, "pix");
        Pedido atual = pedido("2025-01-02T12:00:00", 150.0, 10.0, "credit_card");
        atual.setDescontoCupom(10.0);
        ItemPedido item = new ItemPedido();
        item.setNome("Ração · 3 kg");
        item.setVariacaoCodigo("tamanho-3");
        item.setCategoria("Rações");
        item.setQuantidade(2);
        item.setPrecoOriginal(100.0);
        item.setPrecoUnitario(75.0);
        atual.addItem(item);
        Pedido pendente = pedido("2025-01-02T13:00:00", 999.0, 0.0, null);
        pendente.setStatus(PedidoStatus.PENDENTE);
        when(pedidos.findAllByOrderByDataCriacaoDesc()).thenReturn(List.of(atual, pendente, anterior));
        when(produtos.findAllByOrderByIdDesc()).thenReturn(List.of());
        var resultado = new RelatoriosService(pedidos, produtos).consultar(
                LocalDate.parse("2025-01-02"), LocalDate.parse("2025-01-02"));
        assertEquals(0, resultado.faturamento().compareTo(new BigDecimal("150")));
        assertEquals(0, resultado.receitaProdutos().compareTo(new BigDecimal("140")));
        assertEquals(0, resultado.descontos().compareTo(new BigDecimal("60")));
        assertEquals(1, resultado.pedidosAprovados());
        assertEquals(0, resultado.variacaoVendas().compareTo(new BigDecimal("50")));
        assertEquals("Rações", resultado.vendasPorCategoria().get(0).nome());
        assertEquals(2, resultado.vendasPorCategoria().get(0).unidades());
        assertEquals("Cartão de crédito", resultado.formasPagamento().get(0).nome());
        assertEquals("Ração", resultado.produtosMaisVendidos().get(0).nome());
        verify(produtos, never()).findById(anyLong());
    }

    private Pedido pedido(String data, double total, double frete, String forma) {
        Pedido pedido = new Pedido();
        pedido.setStatus(PedidoStatus.PAGAMENTO_APROVADO);
        pedido.setExpedicao(ExpedicaoStatus.AGUARDANDO);
        pedido.setPagoEm(LocalDateTime.parse(data));
        pedido.setDataCriacao(LocalDateTime.parse(data));
        pedido.setTotal(total);
        pedido.setTaxaEntrega(frete);
        pedido.setFormaPagamento(forma);
        return pedido;
    }
}
