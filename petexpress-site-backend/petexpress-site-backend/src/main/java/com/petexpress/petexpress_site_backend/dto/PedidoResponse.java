package com.petexpress.petexpress_site_backend.dto;

import com.petexpress.petexpress_site_backend.model.Pedido;

import java.time.LocalDateTime;
import java.util.List;

public class PedidoResponse {
    private Long id;
    private String numeroPedido;
    private Long usuarioId;
    private String usuarioEmail;
    private Double total;
    private String status;
    private LocalDateTime data;
    private String formaEntrega;
    private String codigoEntrega;
    private Double taxaEntrega;
    private List<ItemPedidoResponse> itens;

    public static PedidoResponse from(Pedido pedido) {
        PedidoResponse response = new PedidoResponse();
        response.setId(pedido.getId());
        response.setNumeroPedido(pedido.getNumeroPedido());
        response.setUsuarioId(pedido.getUsuario().getId());
        response.setUsuarioEmail(pedido.getUsuario().getEmail());
        response.setTotal(pedido.getTotal());
        response.setStatus(pedido.getStatus() != null ? pedido.getStatus().name() : null);
        response.setData(pedido.getDataCriacao());
        response.setFormaEntrega(pedido.getFormaEntrega());
        response.setCodigoEntrega(pedido.getCodigoEntrega());
        response.setTaxaEntrega(pedido.getTaxaEntrega());
        response.setItens(pedido.getItens().stream().map(ItemPedidoResponse::from).toList());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getFormaEntrega() { return formaEntrega; }
    public void setFormaEntrega(String formaEntrega) { this.formaEntrega = formaEntrega; }

    public String getCodigoEntrega() { return codigoEntrega; }
    public void setCodigoEntrega(String codigoEntrega) { this.codigoEntrega = codigoEntrega; }

    public Double getTaxaEntrega() { return taxaEntrega; }
    public void setTaxaEntrega(Double taxaEntrega) { this.taxaEntrega = taxaEntrega; }

    public List<ItemPedidoResponse> getItens() { return itens; }
    public void setItens(List<ItemPedidoResponse> itens) { this.itens = itens; }
}
