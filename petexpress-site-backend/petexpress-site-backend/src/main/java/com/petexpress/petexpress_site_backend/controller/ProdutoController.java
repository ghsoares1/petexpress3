package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.service.ProdutoService;
import com.petexpress.petexpress_site_backend.service.PromocoesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/produtos", "/produtos"})
public class ProdutoController {
    private final ProdutoService produtos;
    private final PromocoesService promocoes;
    public ProdutoController(ProdutoService produtos, PromocoesService promocoes) {
        this.produtos = produtos;
        this.promocoes = promocoes;
    }
    @GetMapping
    public List<Map<String, Object>> listarTodos() {
        return produtos.listarTodos().stream().map(promocoes::catalogo).toList();
    }
    @GetMapping("/categoria/{tipoAnimal}")
    public List<Map<String, Object>> listarPorCategoria(@PathVariable String tipoAnimal) {
        return produtos.buscarPorTipoAnimal(tipoAnimal).stream().map(promocoes::catalogo).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.of(produtos.buscarPorId(id).map(promocoes::catalogo));
    }
}
