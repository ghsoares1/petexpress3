package com.petexpress.petexpress_site_backend.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long versao;

    private String nome;
    private String tipoProduto;
    private String tipoAnimal;
    private Double preco;
    private String codigoBarras;
    private String descricao;
    private String imagem;

    private String categoria;
    private String fotos;

    @Enumerated(EnumType.STRING)
    private Situacao situacao = Situacao.PUBLICADO;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "produto_variacao", joinColumns = @JoinColumn(name = "produto_id"))
    @OrderColumn(name = "posicao")
    private List<VariacaoProduto> variacoes = new ArrayList<>();

    public enum Situacao { RASCUNHO, PUBLICADO, PAUSADO }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public List<String> getFotos() { return fotos == null ? List.of() : fotos.lines().toList(); }
    public void setFotos(List<String> urls) { this.fotos = String.join("\n", urls); }
    public Situacao getSituacao() { return situacao; }
    public void setSituacao(Situacao situacao) { this.situacao = situacao; }
    public List<VariacaoProduto> getVariacoes() { return variacoes; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipoProduto() { return tipoProduto; }
    public void setTipoProduto(String tipoProduto) { this.tipoProduto = tipoProduto; }

    public String getTipoAnimal() { return tipoAnimal; }
    public void setTipoAnimal(String tipoAnimal) { this.tipoAnimal = tipoAnimal; }

    public Double getPreco() { return preco; }
    public void setPreco(Double preco) { this.preco = preco; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getImagem() { return imagem; }
    public void setImagem(String imagem) { this.imagem = imagem; }
}
