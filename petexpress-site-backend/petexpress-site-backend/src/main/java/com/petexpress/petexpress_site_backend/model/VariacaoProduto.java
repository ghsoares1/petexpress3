package com.petexpress.petexpress_site_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class VariacaoProduto {
    private String codigo;
    private String embalagem;
    private BigDecimal preco;
    private Integer estoque;
    private int estoqueMinimo;
    private int reservado;

    protected VariacaoProduto() {}

    public VariacaoProduto(String codigo, String embalagem, BigDecimal preco, Integer estoque, int estoqueMinimo, int reservado) {
        this.codigo = codigo;
        this.embalagem = embalagem;
        this.preco = preco;
        this.estoque = estoque;
        this.estoqueMinimo = estoqueMinimo;
        this.reservado = reservado;
    }

    public String getCodigo() { return codigo; }
    public String getEmbalagem() { return embalagem; }
    public BigDecimal getPreco() { return preco; }
    public Integer getEstoque() { return estoque; }

    @JsonIgnore
    public int getReservado() { return reservado; }

    @JsonIgnore
    public int getEstoqueMinimo() { return estoqueMinimo; }
}
