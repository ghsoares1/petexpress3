package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.config.MercadoPagoConfig;
import com.petexpress.petexpress_site_backend.service.PedidosLoja.Checkout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PagamentoGateway {
    private final RestTemplate http;
    private final MercadoPagoConfig config;
    private final String base;
    private final String frontend;
    private final String backend;
    private final String modo;

    public PagamentoGateway(RestTemplate http, MercadoPagoConfig config,
                            @Value("${app.mercadopago-api:https://api.mercadopago.com}") String base,
                            @Value("${app.frontend-url}") String frontend,
                            @Value("${app.base-url}") String backend,
                            @Value("${mercadopago.checkout-mode:sandbox}") String modo) {
        this.http = http;
        this.config = config;
        this.base = base.replaceAll("/+$", "");
        this.frontend = frontend.replaceAll("/+$", "");
        this.backend = backend.replaceAll("/+$", "");
        this.modo = modo;
    }

    public boolean configurado() {
        return config.getAccessToken() != null && !config.getAccessToken().isBlank();
    }

    public boolean retornoPublico() {
        return publico(frontend) && publico(backend);
    }

    public void exigirRetornoPublico() {
        if (!retornoPublico()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure o endereço HTTPS público em APP_BASE_URL e PETEXPRESS_FRONTEND_URL para concluir o pagamento.");
        }
    }

    private boolean publico(String valor) {
        try {
            URI url = URI.create(valor);
            String host = url.getHost();
            return "https".equalsIgnoreCase(url.getScheme()) && host != null
                    && !host.equals("localhost") && !host.startsWith("127.") && !host.endsWith(".local");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public Diagnostico diagnosticar() {
        if (!configurado()) return new Diagnostico(false, false, retornoPublico(), null, "Preencha MERCADOPAGO_ACCESS_TOKEN no arquivo .env.");
        try {
            Conta conta = http.exchange(base + "/users/me", HttpMethod.GET, new HttpEntity<>(headers()), Conta.class).getBody();
            if (conta == null || conta.id() == null) throw new RestClientException("Resposta inválida.");
            boolean teste = conta.tags() != null && conta.tags().contains("test_user");
            return new Diagnostico(true, true, retornoPublico(), teste,
                    retornoPublico() ? "Credencial aceita pelo Mercado Pago." : "Credencial aceita. Configure a URL HTTPS pública para retorno e notificações.");
        } catch (HttpClientErrorException ex) {
            return new Diagnostico(true, false, retornoPublico(), null, "O Mercado Pago recusou a credencial. Confira o Access Token da aplicação.");
        } catch (RestClientException ex) {
            return new Diagnostico(true, false, retornoPublico(), null, "Não foi possível consultar o Mercado Pago agora.");
        }
    }

    public Preferencia criar(Checkout checkout) {
        exigirRetornoPublico();
        String retorno = frontend + "/sucesso.html?pedido=" + checkout.id();
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("items", checkout.linhas().stream().map(item -> Map.of(
                "title", item.title(), "quantity", item.quantity(),
                "unit_price", item.unit_price(), "currency_id", "BRL")).toList());
        dados.put("external_reference", checkout.referencia());
        dados.put("back_urls", Map.of("success", retorno, "failure", retorno, "pending", retorno));
        if (frontend.startsWith("https://")) dados.put("auto_return", "approved");
        if (backend.startsWith("https://")) dados.put("notification_url", backend + "/api/pagamento/webhook?source_news=webhooks");
        dados.put("expires", true);
        dados.put("expiration_date_from", Instant.now().toString());
        dados.put("expiration_date_to", checkout.reservaAte().toString());
        dados.put("payment_methods", Map.of("excluded_payment_types", List.of(Map.of("id", "ticket"))));

        var resposta = http.exchange(base + "/checkout/preferences", HttpMethod.POST,
                new HttpEntity<>(dados, headers()), PreferenciaApi.class).getBody();
        if (resposta == null || resposta.id() == null) throw new RestClientException("Preferência sem identificador.");
        boolean sandbox = "sandbox".equalsIgnoreCase(modo);
        String url = sandbox && resposta.sandbox_init_point() != null ? resposta.sandbox_init_point() : resposta.init_point();
        if (url == null || !"https".equalsIgnoreCase(URI.create(url).getScheme())) {
            throw new RestClientException("Preferência sem URL válida.");
        }
        return new Preferencia(resposta.id(), url);
    }

    public Pagamento buscar(String id) {
        var resultado = http.exchange(base + "/v1/payments/{id}", HttpMethod.GET,
                new HttpEntity<>(headers()), PagamentoApi.class, id).getBody();
        if (resultado == null) throw new RestClientException("Pagamento sem resposta.");
        return resultado.pagamento();
    }

    public List<Pagamento> buscarPorReferencia(String referencia) {
        List<Pagamento> pagamentos = new ArrayList<>();
        int limite = 50;
        for (int offset = 0; offset <= 10000; offset += limite) {
            var resposta = http.exchange(base + "/v1/payments/search?external_reference={referencia}"
                            + "&sort=date_created&criteria=desc&range=date_created&begin_date=NOW-365DAYS&end_date=NOW"
                            + "&limit={limite}&offset={offset}",
                    HttpMethod.GET, new HttpEntity<>(headers()), BuscaApi.class, referencia, limite, offset).getBody();
            if (resposta == null || resposta.results() == null) {
                throw new RestClientException("Consulta de pagamentos indisponível.");
            }
            resposta.results().stream().map(PagamentoApi::pagamento)
                    .filter(pagamento -> referencia.equals(pagamento.referencia())).forEach(pagamentos::add);
            if (resposta.results().size() < limite
                    || (resposta.paging() != null && offset + limite >= resposta.paging().total())) {
                return pagamentos;
            }
        }
        throw new RestClientException("Quantidade de pagamentos excede a paginação disponível.");
    }

    private HttpHeaders headers() {
        var headers = new HttpHeaders();
        headers.setBearerAuth(config.getAccessToken());
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    public record Preferencia(String id, String url) {}
    public record Pagamento(String id, String referencia, String status, BigDecimal valor, String moeda, String forma) {}
    public record Diagnostico(boolean configurado, boolean conectado, boolean retornoPublico, Boolean contaTeste, String mensagem) {}
    private record Conta(Long id, List<String> tags) {}
    private record PreferenciaApi(String id, String init_point, String sandbox_init_point) {}
    private record BuscaApi(List<PagamentoApi> results, Paginacao paging) {}
    private record Paginacao(int total) {}
    private record PagamentoApi(String id, String external_reference, String status,
                               BigDecimal transaction_amount, String currency_id, String payment_type_id, String payment_method_id) {
        Pagamento pagamento() {
            return new Pagamento(id, external_reference, status == null ? "" : status, transaction_amount, currency_id, "pix".equals(payment_method_id) ? "pix" : payment_type_id);
        }
    }
}
