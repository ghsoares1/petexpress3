package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.service.PromocoesService;
import com.petexpress.petexpress_site_backend.service.PromocoesService.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vendedor/promocoes")
public class VendedorPromocoesController {
    private final PromocoesService promocoes;
    public VendedorPromocoesController(PromocoesService promocoes) { this.promocoes = promocoes; }

    @GetMapping("/ofertas")
    public List<Oferta> ofertas() { return promocoes.ofertas(); }
    @GetMapping("/cupons")
    public List<Cupom> cupons() { return promocoes.cupons(); }
    @PostMapping("/ofertas")
    public void criarOferta(@RequestBody Oferta dados) { promocoes.salvarOferta(null, dados); }
    @PutMapping("/ofertas/{id}")
    public void editarOferta(@PathVariable long id, @RequestBody Oferta dados) { promocoes.salvarOferta(id, dados); }
    @PostMapping("/cupons")
    public void criarCupom(@RequestBody Cupom dados) { promocoes.salvarCupom(null, dados); }
    @PutMapping("/cupons/{id}")
    public void editarCupom(@PathVariable long id, @RequestBody Cupom dados) { promocoes.salvarCupom(id, dados); }
}
