package com.petexpress.petexpress_site_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@Component
public class UberDirectGateway {
    private final RestTemplate http;
    private final String clientId;
    private final String clientSecret;
    private final String customerId;
    private final String api;
    private final String auth;
    private String token;
    private Instant tokenAte = Instant.EPOCH;

    public UberDirectGateway(RestTemplate http,
                             @Value("${uber.direct.client-id:}") String clientId,
                             @Value("${uber.direct.client-secret:}") String clientSecret,
                             @Value("${uber.direct.customer-id:}") String customerId,
                             @Value("${app.uber-api:https://api.uber.com}") String api,
                             @Value("${app.uber-auth:https://auth.uber.com/oauth/v2/token}") String auth) {
        this.http = http;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.customerId = customerId;
        this.api = api;
        this.auth = auth;
    }

    public boolean configurado() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !customerId.isBlank();
    }

    private synchronized String token() {
        if (!configurado()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "A conta Uber Direct ainda não foi configurada.");
        if (token != null && Instant.now().isBefore(tokenAte)) return token;
        var body = new LinkedMultiValueMap<String, String>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "client_credentials");
        body.add("scope", "eats.deliveries");
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Token resposta = http.postForObject(auth, new HttpEntity<>(body, headers), Token.class);
        if (resposta == null || resposta.access_token() == null || resposta.expires_in() <= 60) {
            throw new RestClientException("Token Uber inválido.");
        }
        token = resposta.access_token();
        tokenAte = Instant.now().plusSeconds(resposta.expires_in() - 60);
        return token;
    }

    public Cotacao cotar(Map<String, Object> dados) {
        return chamar("/delivery_quotes", HttpMethod.POST, dados, Cotacao.class);
    }

    public Entrega solicitar(String payload) {
        return chamar("/deliveries", HttpMethod.POST, payload, Entrega.class);
    }

    public Entrega consultar(String id) {
        if (!id.matches("[A-Za-z0-9_-]{1,100}")) throw new IllegalArgumentException("Identificador inválido.");
        return chamar("/deliveries/" + id, HttpMethod.GET, null, Entrega.class);
    }

    private <T> T chamar(String path, HttpMethod method, Object body, Class<T> tipo) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token());
        headers.setContentType(MediaType.APPLICATION_JSON);
        T resposta = http.exchange(api + "/v1/customers/" + customerId + path,
                method, new HttpEntity<>(body, headers), tipo).getBody();
        if (resposta == null) throw new RestClientException("Resposta Uber vazia.");
        return resposta;
    }

    private record Token(String access_token, long expires_in) {}
    public record Cotacao(String id, long fee, String currency, String currency_type, Instant expires, Integer duration) {}
    public record Entrega(String id, String status, String tracking_url, long fee, Boolean live_mode) {}
}
