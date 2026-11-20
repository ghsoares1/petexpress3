package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.service.EntregasService;
import com.petexpress.petexpress_site_backend.service.EntregasService.*;
import com.petexpress.petexpress_site_backend.service.JwtService.AuthUser;
import com.petexpress.petexpress_site_backend.service.PedidoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class EntregasController {
    private final EntregasService entregas;
    private final PedidoService pedidos;

    public EntregasController(EntregasService entregas,PedidoService pedidos) {
        this.entregas = entregas;
        this.pedidos = pedidos;
    }
    @GetMapping("/api/entregas/status")
    public Map<String,Boolean> status() { return Map.of("configurado",entregas.configurado()); }

    @PostMapping("/api/entregas/cotacoes")
    public Cotacao cotar(@RequestBody Destino destino,@AuthenticationPrincipal AuthUser usuario) {
        return entregas.cotar(destino,usuario.id());
    }
    @PostMapping("/api/vendedor/pedidos/{id}/entrega/cotacao")
    public Cotacao recotar(@PathVariable long id) { return entregas.recotarPedido(id); }

    @PostMapping("/api/vendedor/pedidos/{id}/entrega")
    public Resumo solicitar(@PathVariable long id,@RequestBody Confirmacao confirmacao) {
        return entregas.solicitar(id,confirmacao);
    }
    @GetMapping("/api/vendedor/pedidos/{id}/entrega")
    public Resumo consultar(@PathVariable long id) { return entregas.consultar(id); }

    @GetMapping("/api/pedidos/{id}/entrega")
    public Resumo cliente(@PathVariable long id,@AuthenticationPrincipal AuthUser usuario) {
        pedidos.buscarPorId(id,usuario.id());
        return entregas.consultar(id);
    }
}
