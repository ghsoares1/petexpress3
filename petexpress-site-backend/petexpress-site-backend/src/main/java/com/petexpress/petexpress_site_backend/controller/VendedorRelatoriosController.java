package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.service.RelatoriosService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/vendedor/relatorios")
public class VendedorRelatoriosController {
    private final RelatoriosService relatorios;
    public VendedorRelatoriosController(RelatoriosService relatorios) { this.relatorios = relatorios; }
    @GetMapping
    public RelatoriosService.Relatorio consultar(@RequestParam LocalDate inicio, @RequestParam LocalDate fim) {
        return relatorios.consultar(inicio, fim);
    }
}
