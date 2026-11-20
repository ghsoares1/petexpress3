package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.dto.CheckoutRequest;
import com.petexpress.petexpress_site_backend.service.PagamentoGateway.Pagamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class PagamentoPedidos {
    private static final Logger LOG = LoggerFactory.getLogger(PagamentoPedidos.class);
    private static final Set<String> EM_PROCESSAMENTO = Set.of("pending", "in_process", "authorized", "in_mediation");
    private final PedidosLoja pedidos;
    private final PagamentoGateway gateway;

    public PagamentoPedidos(PedidosLoja pedidos, PagamentoGateway gateway) {
        this.pedidos = pedidos;
        this.gateway = gateway;
    }

    public Inicio iniciar(CheckoutRequest request, Long usuarioId) {
        if (!gateway.configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Pagamento indisponível neste ambiente.");
        }
        gateway.exigirRetornoPublico();
        var checkout = pedidos.iniciar(request, usuarioId);
        if (!checkout.novo()) return new Inicio(checkout.id(), checkout.url());
        PagamentoGateway.Preferencia preferencia;
        try {
            preferencia = gateway.criar(checkout);
        } catch (RuntimeException ex) {
            LOG.warn("Falha ao criar preferência do pedido {}: {}", checkout.id(), ex.getClass().getSimpleName());
            pedidos.cancelarSemPreferencia(checkout.referencia());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível abrir o pagamento. Tente novamente.");
        }
        pedidos.vincular(checkout.referencia(), preferencia.id(), preferencia.url());
        return new Inicio(checkout.id(), preferencia.url());
    }

    public void notificar(String id) {
        if (gateway.configurado() && id != null && id.matches("[0-9]{1,30}")) {
            pedidos.confirmarPagamento(gateway.buscar(id));
        }
    }

    public void atualizarDoCliente(long pedidoId, Long usuarioId, String paymentId) {
        String referencia = pedidos.referenciaDoCliente(pedidoId, usuarioId);
        if (referencia == null || !gateway.configurado()) return;
        if (paymentId != null && paymentId.matches("[0-9]{1,30}")) {
            Pagamento pagamento = gateway.buscar(paymentId);
            if (referencia.equals(pagamento.referencia())) pedidos.confirmarPagamento(pagamento);
        }
    }

    @Scheduled(fixedDelayString = "${app.reserva-verificacao-ms:60000}", initialDelayString = "${app.reserva-verificacao-ms:60000}")
    public void reconciliarReservas() {
        if (!gateway.configurado()) return;
        for (String referencia : pedidos.reservasVencidas()) {
            try {
                List<Pagamento> pagamentos = gateway.buscarPorReferencia(referencia);
                pagamentos.forEach(pedidos::confirmarPagamento);
                if (pagamentos.stream().noneMatch(pagamento -> EM_PROCESSAMENTO.contains(pagamento.status()))) {
                    boolean recusado = pagamentos.stream().anyMatch(pagamento -> "rejected".equals(pagamento.status()));
                    pedidos.expirar(referencia, recusado);
                }
            } catch (RuntimeException ex) {
                LOG.warn("Não foi possível reconciliar reserva {}: {}", referencia, ex.getClass().getSimpleName());
            }
        }
    }

    public record Inicio(long pedidoId, String init_point) {}
}
