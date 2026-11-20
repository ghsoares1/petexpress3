package com.petexpress.petexpress_site_backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutRequest(UUID chave, List<Item> itens, String formaEntrega,
                              String cliente, String enderecoEntrega, String cupom, String cotacaoId) {
    public record Item(Long produtoId, String variacaoCodigo, String nome, Integer quantidade, BigDecimal preco) {}
}
