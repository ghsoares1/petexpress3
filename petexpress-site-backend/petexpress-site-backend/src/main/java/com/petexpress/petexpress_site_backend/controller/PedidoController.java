package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.dto.PedidoRequest;
import com.petexpress.petexpress_site_backend.dto.PedidoResponse;
import com.petexpress.petexpress_site_backend.service.JwtService;
import com.petexpress.petexpress_site_backend.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@RequestBody PedidoRequest request, Authentication authentication) {
        JwtService.AuthUser authUser = (JwtService.AuthUser) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criar(request, authUser.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id, Authentication authentication) {
        JwtService.AuthUser authUser = (JwtService.AuthUser) authentication.getPrincipal();
        return ResponseEntity.ok(pedidoService.buscarPorId(id, authUser.id()));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<PedidoResponse>> listarPorUsuario(@PathVariable Long usuarioId, Authentication authentication) {
        JwtService.AuthUser authUser = (JwtService.AuthUser) authentication.getPrincipal();
        if (!authUser.id().equals(usuarioId)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        return ResponseEntity.ok(pedidoService.listarPorUsuario(usuarioId));
    }

    @PutMapping("/{id}/aprovar")
    public ResponseEntity<PedidoResponse> aprovar(@PathVariable Long id, Authentication authentication) {
        JwtService.AuthUser authUser = (JwtService.AuthUser) authentication.getPrincipal();
        return ResponseEntity.ok(pedidoService.aprovar(id, authUser.id()));
    }
}
