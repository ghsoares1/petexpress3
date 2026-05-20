package com.petexpress.petexpress_site_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petexpress.petexpress_site_backend.dto.LoginRequest;
import com.petexpress.petexpress_site_backend.dto.LoginResponse;
import com.petexpress.petexpress_site_backend.dto.UsuarioResponse;
import com.petexpress.petexpress_site_backend.service.JwtService;
import com.petexpress.petexpress_site_backend.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AuthControllerTest {

    @Test
    void login_Sucesso() {
        AuthController controller = new AuthController(
                new StubUsuarioService(),
                new JwtService(new ObjectMapper(), "test-secret", 480)
        );

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("joao@email.com");
        loginRequest.setSenha("123456");

        ResponseEntity<LoginResponse> response = controller.login(loginRequest);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Login realizado com sucesso.", response.getBody().getMensagem());
        assertEquals("Joao", response.getBody().getUsuario().getNome());
        assertNotNull(response.getBody().getToken());
    }

    private static class StubUsuarioService extends UsuarioService {
        StubUsuarioService() {
            super(null, null, null);
        }

        @Override
        public UsuarioResponse login(LoginRequest request) {
            UsuarioResponse usuario = new UsuarioResponse();
            usuario.setId(1L);
            usuario.setNome("Joao");
            usuario.setEmail(request.getEmail());
            return usuario;
        }
    }
}
