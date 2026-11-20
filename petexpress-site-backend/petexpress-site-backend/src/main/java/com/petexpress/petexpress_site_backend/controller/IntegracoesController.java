package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.service.EntregasService;
import com.petexpress.petexpress_site_backend.service.PagamentoGateway;
import org.springframework.web.bind.annotation.*;

@RestController
public class IntegracoesController {
    private final PagamentoGateway pagamento;
    private final EntregasService entregas;
    public IntegracoesController(PagamentoGateway pagamento, EntregasService entregas) {
        this.pagamento = pagamento;
        this.entregas = entregas;
    }

    @GetMapping("/api/integracoes/status")
    public Status status() {
        return new Status(pagamento.configurado(), pagamento.retornoPublico(), entregas.configurado());
    }

    @GetMapping("/api/vendedor/integracoes/mercadopago")
    public PagamentoGateway.Diagnostico diagnosticar() {
        return pagamento.diagnosticar();
    }

    public record Status(boolean pagamentoConfigurado, boolean retornoPublico, boolean entregaConfigurada) {}
}
