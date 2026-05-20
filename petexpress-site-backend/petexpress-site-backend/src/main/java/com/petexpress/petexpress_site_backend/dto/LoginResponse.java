package com.petexpress.petexpress_site_backend.dto;

public class LoginResponse {
    private String mensagem;
    private UsuarioResponse usuario;
    private String token;

    public LoginResponse(String mensagem, UsuarioResponse usuario, String token) {
        this.mensagem = mensagem;
        this.usuario = usuario;
        this.token = token;
    }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public UsuarioResponse getUsuario() { return usuario; }
    public void setUsuario(UsuarioResponse usuario) { this.usuario = usuario; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
