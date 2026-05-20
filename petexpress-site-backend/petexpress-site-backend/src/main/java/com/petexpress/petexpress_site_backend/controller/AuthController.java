package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.dto.LoginRequest;
import com.petexpress.petexpress_site_backend.dto.LoginResponse;
import com.petexpress.petexpress_site_backend.dto.UsuarioResponse;
import com.petexpress.petexpress_site_backend.service.JwtService;
import com.petexpress.petexpress_site_backend.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        UsuarioResponse usuario = usuarioService.login(request);
        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail());
        return ResponseEntity.ok(new LoginResponse("Login realizado com sucesso.", usuario, token));
    }
}
