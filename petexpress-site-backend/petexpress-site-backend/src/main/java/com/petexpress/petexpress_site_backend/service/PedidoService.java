package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.dto.ItemPedidoRequest;
import com.petexpress.petexpress_site_backend.dto.PedidoRequest;
import com.petexpress.petexpress_site_backend.dto.PedidoResponse;
import com.petexpress.petexpress_site_backend.model.ItemPedido;
import com.petexpress.petexpress_site_backend.model.Pedido;
import com.petexpress.petexpress_site_backend.model.PedidoStatus;
import com.petexpress.petexpress_site_backend.model.Usuario;
import com.petexpress.petexpress_site_backend.repository.PedidoRepository;
import com.petexpress.petexpress_site_backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(PedidoRepository pedidoRepository, UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public PedidoResponse criar(PedidoRequest request, Long authenticatedUserId) {
        validarPedido(request);

        Usuario usuario = usuarioRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado."));

        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(gerarNumeroPedido());
        pedido.setStatus(PedidoStatus.PENDENTE);
        pedido.setUsuario(usuario);
        pedido.setFormaEntrega(limpar(request.getFormaEntrega()));
        pedido.setCodigoEntrega(limpar(request.getCodigoEntrega()));
        pedido.setTaxaEntrega(numeroSeguro(request.getTaxaEntrega()));
        pedido.setDataCriacao(LocalDateTime.now());
        pedido.setDataAtualizacao(LocalDateTime.now());

        double totalProdutos = 0;
        for (ItemPedidoRequest itemRequest : request.getItens()) {
            validarItem(itemRequest);
            ItemPedido item = new ItemPedido();
            item.setProdutoId(limpar(itemRequest.getProdutoId()));
            item.setNome(limpar(itemRequest.getNome()));
            item.setPrecoUnitario(numeroSeguro(itemRequest.getPreco()));
            item.setQuantidade(itemRequest.getQuantidade());
            item.setImagem(limpar(itemRequest.getImagem()));
            totalProdutos += item.getPrecoUnitario() * item.getQuantidade();
            pedido.addItem(item);
        }

        pedido.setTotal(totalProdutos + pedido.getTaxaEntrega());

        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    public PedidoResponse aprovar(Long id, Long authenticatedUserId) {
        Pedido pedido = buscarPedido(id);
        validarDonoPedido(pedido, authenticatedUserId);
        pedido.setStatus(PedidoStatus.PAGAMENTO_APROVADO);
        pedido.setDataAtualizacao(LocalDateTime.now());
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    public void vincularPreferencia(Long id, String preferenceId) {
        Pedido pedido = buscarPedido(id);
        pedido.setPreferenceId(preferenceId);
        pedidoRepository.save(pedido);
    }

    public void atualizarStatusPagamento(String preferenceId, PedidoStatus novoStatus, String paymentId) {
        Pedido pedido = pedidoRepository.findAll().stream()
                .filter(p -> preferenceId.equals(p.getPreferenceId()))
                .findFirst()
                .orElse(null);
        
        if (pedido != null) {
            pedido.setStatus(novoStatus);
            pedido.setPaymentId(paymentId);
            pedido.setDataAtualizacao(LocalDateTime.now());
            pedidoRepository.save(pedido);
        }
    }

    public PedidoResponse buscarPorId(Long id, Long authenticatedUserId) {
        Pedido pedido = buscarPedido(id);
        validarDonoPedido(pedido, authenticatedUserId);
        return PedidoResponse.from(pedido);
    }

    public List<PedidoResponse> listarPorUsuario(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado.");
        }

        return pedidoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId)
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado."));
    }

    private void validarDonoPedido(Pedido pedido, Long authenticatedUserId) {
        if (!pedido.getUsuario().getId().equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
    }

    private void validarPedido(PedidoRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do pedido nao informados.");
        }

        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido sem itens.");
        }
    }

    private void validarItem(ItemPedidoRequest item) {
        if (item == null || isBlank(item.getNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item do pedido invalido.");
        }

        if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade do item invalida.");
        }

        if (item.getPreco() == null || item.getPreco() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preco do item invalido.");
        }
    }

    private String gerarNumeroPedido() {
        return "PE" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private double numeroSeguro(Double value) {
        return value == null ? 0 : value;
    }

    private String limpar(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
