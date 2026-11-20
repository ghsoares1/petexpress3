package com.petexpress.petexpress_site_backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.time.LocalDateTime;
import java.time.Instant;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long versao;
    private Long cupomId;
    private String cupomCodigo;
    private Double descontoCupom = 0.0;
    private LocalDateTime pagoEm;
    private String formaPagamento;
    private String entregaCotacaoId;
    private String numeroPedido;
    private String chaveCheckout;
    private String referenciaPagamento;
    private String urlPagamento;
    private Instant reservaAte;
    private String cliente;
    private String enderecoEntrega;
    private String rastreio;
    @Enumerated(EnumType.STRING)
    private ExpedicaoStatus expedicao = ExpedicaoStatus.AGUARDANDO;
    private Double total;
    @Enumerated(EnumType.STRING)
    private PedidoStatus status;
    private String preferenceId;
    private String paymentId;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private String formaEntrega;
    private String codigoEntrega;
    private Double taxaEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    public Long getCupomId() { return cupomId; }
    public void setCupomId(Long cupomId) { this.cupomId = cupomId; }
    public String getCupomCodigo() { return cupomCodigo; }
    public void setCupomCodigo(String cupomCodigo) { this.cupomCodigo = cupomCodigo; }
    public Double getDescontoCupom() { return descontoCupom; }
    public void setDescontoCupom(Double descontoCupom) { this.descontoCupom = descontoCupom; }
    public LocalDateTime getPagoEm() { return pagoEm; }
    public void setPagoEm(LocalDateTime pagoEm) { this.pagoEm = pagoEm; }

    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public String getEntregaCotacaoId() { return entregaCotacaoId; }
    public void setEntregaCotacaoId(String entregaCotacaoId) { this.entregaCotacaoId = entregaCotacaoId; }

    public String getChaveCheckout() { return chaveCheckout; }
    public void setChaveCheckout(String chaveCheckout) { this.chaveCheckout = chaveCheckout; }
    public String getReferenciaPagamento() { return referenciaPagamento; }
    public void setReferenciaPagamento(String referenciaPagamento) { this.referenciaPagamento = referenciaPagamento; }
    public String getUrlPagamento() { return urlPagamento; }
    public void setUrlPagamento(String urlPagamento) { this.urlPagamento = urlPagamento; }
    public Instant getReservaAte() { return reservaAte; }
    public void setReservaAte(Instant reservaAte) { this.reservaAte = reservaAte; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getEnderecoEntrega() { return enderecoEntrega; }
    public void setEnderecoEntrega(String enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }
    public ExpedicaoStatus getExpedicao() { return expedicao; }
    public void setExpedicao(ExpedicaoStatus expedicao) { this.expedicao = expedicao; }
    public String getRastreio() { return rastreio; }
    public void setRastreio(String rastreio) { this.rastreio = rastreio; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public PedidoStatus getStatus() { return status; }
    public void setStatus(PedidoStatus status) { this.status = status; }

    public String getPreferenceId() { return preferenceId; }
    public void setPreferenceId(String preferenceId) { this.preferenceId = preferenceId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }

    public String getFormaEntrega() { return formaEntrega; }
    public void setFormaEntrega(String formaEntrega) { this.formaEntrega = formaEntrega; }

    public String getCodigoEntrega() { return codigoEntrega; }
    public void setCodigoEntrega(String codigoEntrega) { this.codigoEntrega = codigoEntrega; }

    public Double getTaxaEntrega() { return taxaEntrega; }
    public void setTaxaEntrega(Double taxaEntrega) { this.taxaEntrega = taxaEntrega; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens; }

    public void addItem(ItemPedido item) {
        itens.add(item);
        item.setPedido(this);
    }
}
