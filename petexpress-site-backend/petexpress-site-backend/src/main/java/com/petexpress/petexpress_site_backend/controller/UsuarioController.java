package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.dto.UsuarioCadastroRequest;
import com.petexpress.petexpress_site_backend.dto.UsuarioResponse;
import com.petexpress.petexpress_site_backend.service.JwtService;
import com.petexpress.petexpress_site_backend.service.UsuarioService;
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

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody UsuarioCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.cadastrar(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id, Authentication authentication) {
        validarMesmoUsuario(id, authentication);
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id, @RequestBody UsuarioCadastroRequest request, Authentication authentication) {
        validarMesmoUsuario(id, authentication);
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    private void validarMesmoUsuario(Long id, Authentication authentication) {
        JwtService.AuthUser authUser = (JwtService.AuthUser) authentication.getPrincipal();
        if (!authUser.id().equals(id)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
    }
}
