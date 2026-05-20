package com.petexpress.petexpress_site_backend.dto;

import com.petexpress.petexpress_site_backend.model.PagamentoCartItem;
import java.util.List;

public class PreferenciaRequest {
    private Long pedidoId;
    private String frontendBaseUrl;
    private List<PagamentoCartItem> itens;

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public String getFrontendBaseUrl() { return frontendBaseUrl; }
    public void setFrontendBaseUrl(String frontendBaseUrl) { this.frontendBaseUrl = frontendBaseUrl; }

    public List<PagamentoCartItem> getItens() { return itens; }
    public void setItens(List<PagamentoCartItem> itens) { this.itens = itens; }
}
