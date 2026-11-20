package com.petexpress.petexpress_site_backend.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vendedor/configuracoes")
public class VendedorConfiguracaoController {
    private final JdbcTemplate jdbc;
    public VendedorConfiguracaoController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public Loja ler() {
        return jdbc.queryForObject("SELECT nome,telefone,endereco FROM loja_configuracao WHERE id=1",
                (rs,n) -> new Loja(rs.getString("nome"),rs.getString("telefone"),rs.getString("endereco")));
    }

    @PutMapping
    public Loja salvar(@RequestBody Loja loja) {
        if (loja.nome() == null || loja.nome().isBlank() || loja.nome().length() > 100
                || loja.telefone() == null || loja.telefone().length() > 30
                || loja.endereco() == null || loja.endereco().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confira os dados da loja.");
        }
        Loja dados = new Loja(loja.nome().strip(),loja.telefone().strip(),loja.endereco().strip());
        jdbc.update("UPDATE loja_configuracao SET nome=?,telefone=?,endereco=? WHERE id=1",
                dados.nome(),dados.telefone(),dados.endereco());
        return dados;
    }

    public record Loja(String nome,String telefone,String endereco) {}
}
