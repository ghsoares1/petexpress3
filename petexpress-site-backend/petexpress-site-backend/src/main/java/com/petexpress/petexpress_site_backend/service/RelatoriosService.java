package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.model.*;
import com.petexpress.petexpress_site_backend.repository.PedidoRepository;
import com.petexpress.petexpress_site_backend.repository.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class RelatoriosService {
    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;

    public RelatoriosService(PedidoRepository pedidos, ProdutoRepository produtos) {
        this.pedidos = pedidos;
        this.produtos = produtos;
    }

    @Transactional(readOnly = true)
    public Relatorio consultar(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio) || fim.isAfter(LocalDate.now(ZoneId.of("America/Sao_Paulo")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um período válido até hoje.");
        }
        List<Pedido> todos = pedidos.findAllByOrderByDataCriacaoDesc();
        List<Pedido> pagos = todos.stream().filter(p -> p.getStatus() == PedidoStatus.PAGAMENTO_APROVADO)
                .filter(p -> {
                    LocalDate dia = (p.getPagoEm() == null ? p.getDataCriacao() : p.getPagoEm()).toLocalDate();
                    return !dia.isBefore(inicio) && !dia.isAfter(fim);
                }).toList();
        BigDecimal faturamento = BigDecimal.ZERO;
        BigDecimal frete = BigDecimal.ZERO;
        BigDecimal descontos = BigDecimal.ZERO;
        Map<String, Long> categorias = new HashMap<>();
        Map<String, Long> formas = new HashMap<>();
        Map<String, Long> tamanhos = new HashMap<>();
        Map<String, Long> porProduto = new HashMap<>();
        Map<LocalDate, BigDecimal> porDia = new TreeMap<>();
        for (Pedido pedido : pagos) {
            formas.merge(pedido.getFormaPagamento() == null ? "Não informado" : switch(pedido.getFormaPagamento()) {
                case "pix" -> "Pix";
                case "credit_card" -> "Cartão de crédito";
                case "debit_card" -> "Cartão de débito";
                case "ticket" -> "Boleto";
                default -> "Outros";
            }, 1L, Long::sum);
            faturamento = faturamento.add(valor(pedido.getTotal()));
            frete = frete.add(valor(pedido.getTaxaEntrega()));
            descontos = descontos.add(valor(pedido.getDescontoCupom()));
            LocalDate dia = (pedido.getPagoEm() == null ? pedido.getDataCriacao() : pedido.getPagoEm()).toLocalDate();
            porDia.merge(dia, valor(pedido.getTotal()), BigDecimal::add);
            for (ItemPedido item : pedido.getItens()) {
                String categoria = item.getCategoria();
                if (categoria == null && item.getProdutoId() != null && item.getProdutoId().matches("[0-9]+")) {
                    categoria = produtos.findById(Long.valueOf(item.getProdutoId())).map(Produto::getCategoria).orElse("Outros");
                }
                categorias.merge(categoria == null || categoria.isBlank() ? "Outros" : categoria,
                        item.getQuantidade().longValue(), Long::sum);
                tamanhos.merge(item.getNome(), item.getQuantidade().longValue(), Long::sum);
                String nome = item.getNome();
                int separador = nome.lastIndexOf(" · ");
                if (item.getVariacaoCodigo() != null && separador >= 0) nome = nome.substring(0, separador);
                porProduto.merge(nome, item.getQuantidade().longValue(), Long::sum);
                if (item.getPrecoOriginal() != null) {
                    descontos = descontos.add(valor(item.getPrecoOriginal()).subtract(valor(item.getPrecoUnitario()))
                            .multiply(BigDecimal.valueOf(item.getQuantidade())));
                }
            }
        }
        List<Alerta> alertas = produtos.findAllByOrderByIdDesc().stream()
                .flatMap(p -> p.getVariacoes().stream()
                        .filter(v -> v.getEstoque() == 0 || v.getEstoqueMinimo() > 0 && v.getEstoque() <= v.getEstoqueMinimo())
                        .map(v -> new Alerta(p.getNome(), v.getEmbalagem(), v.getEstoque(), v.getEstoqueMinimo()))).toList();
        long separacao = todos.stream().filter(p -> p.getStatus() == PedidoStatus.PAGAMENTO_APROVADO
                && p.getExpedicao() == ExpedicaoStatus.AGUARDANDO).count();
        BigDecimal ticket = pagos.isEmpty() ? BigDecimal.ZERO : faturamento.divide(BigDecimal.valueOf(pagos.size()), 2, RoundingMode.HALF_UP);
        long duracao = java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) + 1;
        LocalDate anteriorInicio = inicio.minusDays(duracao);
        LocalDate anteriorFim = inicio.minusDays(1);
        List<Pedido> anteriores = todos.stream().filter(p -> p.getStatus() == PedidoStatus.PAGAMENTO_APROVADO)
                .filter(p -> {
                    LocalDate dia = (p.getPagoEm() == null ? p.getDataCriacao() : p.getPagoEm()).toLocalDate();
                    return !dia.isBefore(anteriorInicio) && !dia.isAfter(anteriorFim);
                }).toList();
        BigDecimal totalAnterior = anteriores.stream().map(p -> valor(p.getTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticketAnterior = anteriores.isEmpty() ? BigDecimal.ZERO : totalAnterior.divide(BigDecimal.valueOf(anteriores.size()), 2, RoundingMode.HALF_UP);
        return new Relatorio(inicio, fim, faturamento, frete, faturamento.subtract(frete), descontos, pagos.size(), ticket,
                separacao, alertas, ranking(porProduto), ranking(tamanhos),
                porDia.entrySet().stream().map(e -> new Dia(e.getKey(), e.getValue())).toList(), ranking(categorias), ranking(formas),
                variacao(faturamento,totalAnterior), variacao(BigDecimal.valueOf(pagos.size()),BigDecimal.valueOf(anteriores.size())), variacao(ticket,ticketAnterior));
    }

    private BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        return anterior.signum() == 0 ? null : atual.subtract(anterior).multiply(new BigDecimal("100"))
                .divide(anterior, 1, RoundingMode.HALF_UP);
    }

    private List<Ranking> ranking(Map<String, Long> totais) {
        return totais.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey())).limit(10).map(e -> new Ranking(e.getKey(), e.getValue())).toList();
    }
    private BigDecimal valor(Double numero) { return numero == null ? BigDecimal.ZERO : BigDecimal.valueOf(numero); }
    public record Alerta(String produto, String tamanho, int disponivel, int minimo) {}
    public record Ranking(String nome, long unidades) {}
    public record Dia(LocalDate data, BigDecimal faturamento) {}
    public record Relatorio(LocalDate inicio, LocalDate fim, BigDecimal faturamento, BigDecimal frete,
                            BigDecimal receitaProdutos, BigDecimal descontos, int pedidosAprovados, BigDecimal ticketMedio,
                            long aguardandoSeparacao, List<Alerta> alertas, List<Ranking> produtosMaisVendidos,
                            List<Ranking> tamanhosMaisVendidos, List<Dia> vendasPorDia, List<Ranking> vendasPorCategoria,
                            List<Ranking> formasPagamento, BigDecimal variacaoVendas, BigDecimal variacaoPedidos, BigDecimal variacaoTicket) {}
}
