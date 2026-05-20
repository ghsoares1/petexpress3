package com.petexpress.petexpress_site_backend.dto;

import com.petexpress.petexpress_site_backend.model.Usuario;

public class UsuarioResponse {
    private Long id;
    private String nome;
    private String sobrenome;
    private String cpf;
    private String email;
    private String endereco;
    private String complemento;
    private String bairro;
    private String cep;

    public static UsuarioResponse from(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(usuario.getId());
        response.setNome(usuario.getNome());
        response.setSobrenome(usuario.getSobrenome());
        response.setCpf(usuario.getCpf());
        response.setEmail(usuario.getEmail());
        response.setEndereco(usuario.getEndereco());
        response.setComplemento(usuario.getComplemento());
        response.setBairro(usuario.getBairro());
        response.setCep(usuario.getCep());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getSobrenome() { return sobrenome; }
    public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
}
