package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.dto.UsuarioCadastroRequest;
import com.petexpress.petexpress_site_backend.dto.UsuarioResponse;
import com.petexpress.petexpress_site_backend.model.Usuario;
import com.petexpress.petexpress_site_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioService usuarioService;

    private UsuarioCadastroRequest usuarioRequest;
    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, new PasswordService(), null);

        usuarioRequest = new UsuarioCadastroRequest();
        usuarioRequest.setNome("João Silva");
        usuarioRequest.setEmail("joao@email.com");
        usuarioRequest.setSenha("123456");
        usuarioRequest.setEndereco("Rua 1");
        usuarioRequest.setBairro("Bairro");
        usuarioRequest.setCep("12345-678");
        usuarioRequest.setSobrenome("Silva");
        usuarioRequest.setCpf("12345678901");

        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNome("João Silva");
        usuarioMock.setEmail("joao@email.com");
        usuarioMock.setSenhaHash("hashed_password");
        usuarioMock.setCpf("12345678901");
    }

    @Test
    void criarUsuario_ComSucesso() {
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCpf(anyString())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioMock);

        UsuarioResponse response = usuarioService.cadastrar(usuarioRequest);

        assertNotNull(response);
        assertEquals(usuarioMock.getId(), response.getId());
        assertEquals(usuarioMock.getNome(), response.getNome());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void criarUsuario_LancaExcecaoQuandoEmailJaExiste() {
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> usuarioService.cadastrar(usuarioRequest));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
