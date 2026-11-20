package com.petexpress.petexpress_site_backend.config;

import com.petexpress.petexpress_site_backend.repository.UsuarioRepository;
import com.petexpress.petexpress_site_backend.service.JwtService.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AcessoVendedor {
    private final UsuarioRepository usuarios;
    private final Set<Long> ids;

    public AcessoVendedor(UsuarioRepository usuarios,
                         @Value("${app.vendedor.ids:}") String ids,
                         @Value("${app.jwt-secret}") String jwtSecret) {
        this.usuarios = usuarios;
        this.ids = Arrays.stream(ids.split(","))
                .map(String::strip).filter(id -> !id.isBlank()).map(Long::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        if (!this.ids.isEmpty() && (jwtSecret.length() < 32
                || jwtSecret.equals("petexpress-local-dev-secret-change-me"))) {
            throw new IllegalStateException("Configure JWT_SECRET com uma chave própria de pelo menos 32 caracteres.");
        }
    }

    public boolean permitido(AuthUser user) {
        return ids.contains(user.id()) && usuarios.existsById(user.id());
    }
}
