package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.service.EstoqueService;
import com.petexpress.petexpress_site_backend.service.EstoqueService.Lancamento;
import com.petexpress.petexpress_site_backend.service.EstoqueService.Minimo;
import com.petexpress.petexpress_site_backend.service.EstoqueService.PaginaHistorico;
import com.petexpress.petexpress_site_backend.service.EstoqueService.Saldo;
import com.petexpress.petexpress_site_backend.service.JwtService.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendedor/estoque")
public class VendedorEstoqueController {
    private final EstoqueService estoque;

    public VendedorEstoqueController(EstoqueService estoque) {
        this.estoque = estoque;
    }

    @GetMapping
    public List<Saldo> listar() {
        return estoque.listarSaldos();
    }

    @GetMapping("/movimentacoes")
    public PaginaHistorico historico(@RequestParam(required = false) String codigo,
                                    @RequestParam(required = false) Long antesDe) {
        return estoque.listarHistorico(codigo, antesDe);
    }

    @PostMapping("/{produtoId}/{codigo}/movimentacoes")
    public Saldo movimentar(@PathVariable long produtoId, @PathVariable String codigo,
                            @RequestBody Lancamento lancamento, @AuthenticationPrincipal AuthUser vendedor) {
        return estoque.movimentar(produtoId, codigo, lancamento, vendedor.id());
    }

    @PutMapping("/{produtoId}/{codigo}/minimo")
    public Saldo definirMinimo(@PathVariable long produtoId, @PathVariable String codigo, @RequestBody Minimo minimo) {
        return estoque.definirMinimo(produtoId, codigo, minimo);
    }
}
