package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.dto.ItemPedidoRequest;
import com.petexpress.petexpress_site_backend.dto.PedidoRequest;
import com.petexpress.petexpress_site_backend.dto.PedidoResponse;
import com.petexpress.petexpress_site_backend.model.Pedido;
import com.petexpress.petexpress_site_backend.model.PedidoStatus;
import com.petexpress.petexpress_site_backend.model.Usuario;
import com.petexpress.petexpress_site_backend.repository.PedidoRepository;
import com.petexpress.petexpress_site_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PedidoService pedidoService;

    private Usuario usuarioMock;
    private PedidoRequest pedidoRequest;

    @BeforeEach
    void setUp() {
        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setEmail("teste@email.com");

        ItemPedidoRequest itemRequest = new ItemPedidoRequest();
        itemRequest.setProdutoId("123");
        itemRequest.setNome("Ração");
        itemRequest.setPreco(100.0);
        itemRequest.setQuantidade(2);

        pedidoRequest = new PedidoRequest();
        pedidoRequest.setUsuarioId(1L);
        pedidoRequest.setTaxaEntrega(15.0);
        pedidoRequest.setItens(List.of(itemRequest));
    }

    @Test
    void criarPedido_CalculaTotalCorretamente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> {
            Pedido p = i.getArgument(0);
            p.setId(10L);
            return p;
        });

        PedidoResponse response = pedidoService.criar(pedidoRequest, 1L);

        assertNotNull(response);
        // (100.0 * 2) + 15.0 = 215.0
        assertEquals(215.0, response.getTotal());
        assertEquals(PedidoStatus.PENDENTE.name(), response.getStatus());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    void criarPedido_UsaUsuarioAutenticadoMesmoComUsuarioIdAntigoNoPayload() {
        Usuario outroUsuario = new Usuario();
        outroUsuario.setId(2L);
        outroUsuario.setEmail("outro@email.com");

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(outroUsuario));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> {
            Pedido p = i.getArgument(0);
            p.setId(11L);
            return p;
        });

        PedidoResponse response = pedidoService.criar(pedidoRequest, 2L);

        assertEquals(2L, response.getUsuarioId());
        assertEquals(PedidoStatus.PENDENTE.name(), response.getStatus());
        verify(usuarioRepository, times(1)).findById(2L);
    }
}
