package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.model.Produto;
import com.petexpress.petexpress_site_backend.service.ProdutoService;
import com.petexpress.petexpress_site_backend.service.ProdutoService.Anuncio;
import com.petexpress.petexpress_site_backend.service.JwtService.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendedor/produtos")
public class VendedorProdutoController {
    private final ProdutoService produtos;

    public VendedorProdutoController(ProdutoService produtos) {
        this.produtos = produtos;
    }

    @GetMapping
    public List<Produto> listar() {
        return produtos.listarParaVendedor();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Produto criar(@RequestBody Anuncio anuncio, @AuthenticationPrincipal AuthUser vendedor) {
        return produtos.salvar(null, anuncio, vendedor.id());
    }

    @PutMapping("/{id}")
    public Produto editar(@PathVariable Long id, @RequestBody Anuncio anuncio, @AuthenticationPrincipal AuthUser vendedor) {
        return produtos.salvar(id, anuncio, vendedor.id());
    }
}
