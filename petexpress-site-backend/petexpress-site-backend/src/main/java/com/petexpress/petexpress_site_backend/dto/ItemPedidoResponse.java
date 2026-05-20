package com.petexpress.petexpress_site_backend.dto;

import com.petexpress.petexpress_site_backend.model.ItemPedido;

public class ItemPedidoResponse {
    private Long id;
    private String produtoId;
    private String nome;
    private Double preco;
    private Integer quantidade;
    private String imagem;

    public static ItemPedidoResponse from(ItemPedido item) {
        ItemPedidoResponse response = new ItemPedidoResponse();
        response.setId(item.getId());
        response.setProdutoId(item.getProdutoId());
        response.setNome(item.getNome());
        response.setPreco(item.getPrecoUnitario());
        response.setQuantidade(item.getQuantidade());
        response.setImagem(item.getImagem());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Double getPreco() { return preco; }
    public void setPreco(Double preco) { this.preco = preco; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public String getImagem() { return imagem; }
    public void setImagem(String imagem) { this.imagem = imagem; }
}
