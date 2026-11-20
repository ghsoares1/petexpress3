package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.dto.CheckoutRequest;
import com.petexpress.petexpress_site_backend.service.PromocoesService;
import com.petexpress.petexpress_site_backend.service.JwtService.AuthUser;
import com.petexpress.petexpress_site_backend.service.PagamentoPedidos;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@RestController
@RequestMapping("/api/pagamento")
public class PagamentoController {
    private final PagamentoPedidos pagamentos;

    private final PromocoesService promocoes;

    public PagamentoController(PagamentoPedidos pagamentos, PromocoesService promocoes) {
        this.promocoes = promocoes;
        this.pagamentos = pagamentos;
    }

    @PostMapping("/orcamento")
    public PromocoesService.Orcamento orcar(@RequestBody PromocoesService.OrcamentoRequest request) {
        return promocoes.orcar(request);
    }

    @PostMapping("/criar-preferencia")
    public PagamentoPedidos.Inicio iniciar(@RequestBody CheckoutRequest request, @AuthenticationPrincipal AuthUser cliente) {
        return pagamentos.iniciar(request, cliente.id());
    }

    @PostMapping("/pedidos/{id}/atualizar")
    public ResponseEntity<Void> atualizar(@PathVariable long id, @RequestBody Atualizacao atualizacao,
                                          @AuthenticationPrincipal AuthUser cliente) {
        pagamentos.atualizarDoCliente(id, cliente.id(), atualizacao.paymentId());
        return ResponseEntity.noContent().build();
    }

    public record Atualizacao(String paymentId) {}

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody(required = false) Map<String, Object> payload,
                                       @RequestParam(name = "type", required = false) String tipo,
                                       @RequestParam(name = "data.id", required = false) String id) {
        if (payload != null && "payment".equals(payload.get("type"))
                && payload.get("data") instanceof Map<?, ?> dados) {
            id = String.valueOf(dados.get("id"));
            tipo = "payment";
        }
        if ("payment".equals(tipo)) {
            try {
                pagamentos.notificar(id);
            } catch (HttpClientErrorException.NotFound ignored) {
                // Identificador inexistente na conta Mercado Pago da loja.
            }
        }
        return ResponseEntity.ok().build();
    }
}
