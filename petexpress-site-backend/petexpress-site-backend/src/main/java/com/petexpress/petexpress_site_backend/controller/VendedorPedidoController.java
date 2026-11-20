package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.dto.PedidoResponse;
import com.petexpress.petexpress_site_backend.model.ExpedicaoStatus;
import com.petexpress.petexpress_site_backend.service.PedidosLoja;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendedor/pedidos")
public class VendedorPedidoController {
    private final PedidosLoja pedidos;

    public VendedorPedidoController(PedidosLoja pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return pedidos.listar();
    }

    @PutMapping("/{id}/expedicao")
    public PedidoResponse expedir(@PathVariable long id, @RequestBody Atualizacao atualizacao) {
        return pedidos.expedir(id, atualizacao.expedicao(), atualizacao.rastreio());
    }

    public record Atualizacao(ExpedicaoStatus expedicao, String rastreio) {}
}
