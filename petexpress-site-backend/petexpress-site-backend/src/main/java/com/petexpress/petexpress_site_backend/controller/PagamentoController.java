package com.petexpress.petexpress_site_backend.controller;

import com.petexpress.petexpress_site_backend.config.MercadoPagoConfig;
import com.petexpress.petexpress_site_backend.dto.PreferenciaRequest;
import com.petexpress.petexpress_site_backend.model.PagamentoCartItem;
import com.petexpress.petexpress_site_backend.model.PedidoStatus;
import com.petexpress.petexpress_site_backend.service.PedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagamento")
@CrossOrigin(origins = "*")
public class PagamentoController {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoController.class);

    private final MercadoPagoConfig mercadoPagoConfig;
    private final RestTemplate restTemplate;
    private final PedidoService pedidoService;

    @Value("${app.base-url:http://localhost:8082}")
    private String appBaseUrl;

    @Value("${mercadopago.checkout-mode:production}")
    private String checkoutMode;

    public PagamentoController(MercadoPagoConfig mercadoPagoConfig, RestTemplate restTemplate, PedidoService pedidoService) {
        this.mercadoPagoConfig = mercadoPagoConfig;
        this.restTemplate = restTemplate;
        this.pedidoService = pedidoService;
    }

    @PostMapping("/criar-preferencia")
    public ResponseEntity<Map<String, Object>> criarPreferencia(@RequestBody Object rawRequest) {
        PreferenciaRequest request = normalizePreferenciaRequest(rawRequest);
        List<PagamentoCartItem> carrinho = request.getItens();
        logger.info("Recebida requisição POST /api/pagamento/criar-preferencia com {} itens", carrinho == null ? 0 : carrinho.size());
        if (carrinho == null || carrinho.isEmpty()) {
            logger.warn("Carrinho vazio recebido");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrinho vazio.");
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (PagamentoCartItem item : carrinho) {
            if (item == null || item.getTitle() == null || item.getQuantity() == null || item.getPrice() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itens do carrinho inválidos.");
            }
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("title", item.getTitle());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("unit_price", item.getPrice());
            itemMap.put("currency_id", "BRL");
            items.add(itemMap);
        }

        logger.info("Itens recebidos para preferencia Mercado Pago: {}", items);

        Map<String, Object> requestBody = new HashMap<>();
        String baseUrl = resolveReturnBaseUrl(request);
        requestBody.put("items", items);
        requestBody.put("back_urls", Map.of(
                "success", baseUrl + "/sucesso.html",
                "failure", baseUrl + "/falha.html",
                "pending", baseUrl + "/pendente.html"
        ));
        if (request.getPedidoId() != null) {
            requestBody.put("external_reference", String.valueOf(request.getPedidoId()));
        }

        logger.info("Payload enviado ao Mercado Pago: {}", requestBody);

        if (mercadoPagoConfig.getAccessToken() == null || mercadoPagoConfig.getAccessToken().isBlank()) {
            logger.error("Mercado Pago access token not configured — cannot create preference.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Mercado Pago access token not configured.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mercadoPagoConfig.getAccessToken());

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity("https://api.mercadopago.com/checkout/preferences", httpEntity, Map.class);
        } catch (RestClientException ex) {
            if (ex instanceof HttpStatusCodeException) {
                String respBody = ((HttpStatusCodeException) ex).getResponseBodyAsString();
                logger.error("Erro ao chamar Mercado Pago: status={}, body={}", ((HttpStatusCodeException) ex).getStatusCode(), respBody);
            } else {
                logger.error("Erro ao chamar Mercado Pago: {}", ex.getMessage(), ex);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha na comunicação com Mercado Pago.");
        }

        logger.info("Resposta Mercado Pago status={}, body={}", response.getStatusCode(), response.getBody());

        if (response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao criar preferência no Mercado Pago.");
        }

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("init_point")) {
            logger.error("Resposta inválida do Mercado Pago: {}", responseBody);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Resposta inválida do Mercado Pago.");
        }

        String preferenceId = (String) responseBody.get("id");
        if (request.getPedidoId() != null && preferenceId != null) {
            pedidoService.vincularPreferencia(request.getPedidoId(), preferenceId);
        }

        Object initPoint = responseBody.get("init_point");
        Object sandboxInitPoint = responseBody.get("sandbox_init_point");
        Object checkoutUrl = shouldUseSandboxCheckout() && sandboxInitPoint != null ? sandboxInitPoint : initPoint;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("init_point", checkoutUrl);
        result.put("checkout_url", checkoutUrl);
        result.put("production_init_point", initPoint);
        if (preferenceId != null) {
            result.put("preference_id", preferenceId);
        }
        if (responseBody.get("sandbox_init_point") != null) {
            result.put("sandbox_init_point", responseBody.get("sandbox_init_point"));
        }
        result.put("environment", shouldUseSandboxCheckout() && sandboxInitPoint != null ? "sandbox" : "production");
        logger.info("Checkout Mercado Pago devolvido para o front: environment={}, checkout_url={}", result.get("environment"), checkoutUrl);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/redirecionar/{preferenceId}")
    public ResponseEntity<Void> redirecionarCheckout(@PathVariable String preferenceId) {
        if (preferenceId == null || !preferenceId.matches("[A-Za-z0-9\\-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preferencia invalida.");
        }

        String host = shouldUseSandboxCheckout()
                ? "https://sandbox.mercadopago.com.br"
                : "https://www.mercadopago.com.br";
        URI checkoutUri = URI.create(host + "/checkout/v1/redirect?pref_id=" + preferenceId);
        return ResponseEntity.status(HttpStatus.FOUND).location(checkoutUri).build();
    }

    private PreferenciaRequest normalizePreferenciaRequest(Object rawRequest) {
        PreferenciaRequest request = new PreferenciaRequest();

        if (rawRequest instanceof List<?>) {
            request.setItens(toCartItems((List<?>) rawRequest));
            return request;
        }

        if (rawRequest instanceof Map<?, ?> map) {
            Object itens = map.get("itens");
            if (itens == null) {
                itens = map.get("items");
            }
            if (itens instanceof List<?>) {
                request.setItens(toCartItems((List<?>) itens));
            }

            Object pedidoId = map.get("pedidoId");
            if (pedidoId instanceof Number number) {
                request.setPedidoId(number.longValue());
            } else if (pedidoId instanceof String text && !text.isBlank()) {
                try {
                    request.setPedidoId(Long.parseLong(text));
                } catch (NumberFormatException ignored) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido invalido.");
                }
            }

            Object frontendBaseUrl = map.get("frontendBaseUrl");
            if (frontendBaseUrl instanceof String text) {
                request.setFrontendBaseUrl(text);
            }
            return request;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requisicao de pagamento invalida.");
    }

    private List<PagamentoCartItem> toCartItems(List<?> rawItems) {
        List<PagamentoCartItem> items = new ArrayList<>();
        for (Object rawItem : rawItems) {
            if (!(rawItem instanceof Map<?, ?> map)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Itens do carrinho invalidos.");
            }

            PagamentoCartItem item = new PagamentoCartItem();
            item.setTitle(firstString(map, "title", "nome", "name"));
            item.setQuantity(firstInteger(map, "quantity", "quantidade"));
            item.setPrice(firstDouble(map, "price", "preco", "unit_price"));
            items.add(item);
        }
        return items;
    }

    private String firstString(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private Integer firstInteger(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private Double firstDouble(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Double.parseDouble(text.replace(',', '.'));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean isSandboxToken() {
        String token = mercadoPagoConfig.getAccessToken();
        return token != null && token.startsWith("TEST-");
    }

    private boolean shouldUseSandboxCheckout() {
        if (isSandboxToken()) {
            return true;
        }
        return "sandbox".equalsIgnoreCase(checkoutMode) && !"production".equalsIgnoreCase(checkoutMode);
    }

    private String resolveReturnBaseUrl(PreferenciaRequest request) {
        String frontendBaseUrl = request.getFrontendBaseUrl();
        if (frontendBaseUrl != null && !frontendBaseUrl.isBlank()) {
            String trimmed = frontendBaseUrl.trim();
            if (!"null".equalsIgnoreCase(trimmed) && (trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
                String normalized = trimmed.replaceAll("/+$", "");
                logger.info("Using frontendBaseUrl for Mercado Pago back_urls: {}", normalized);
                return normalized;
            }
        }
        String normalized = appBaseUrl.replaceAll("/+$", "");
        logger.info("Using app.base-url for Mercado Pago back_urls: {}", normalized);
        return normalized;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {
        logger.info("IPN Mercado Pago Recebido: {}", payload);
        
        try {
            if ("payment".equals(payload.get("type"))) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                if (data != null && data.get("id") != null) {
                    String paymentId = String.valueOf(data.get("id"));
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(mercadoPagoConfig.getAccessToken());
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    
                    ResponseEntity<Map> paymentResp = restTemplate.exchange(
                        "https://api.mercadopago.com/v1/payments/" + paymentId,
                        org.springframework.http.HttpMethod.GET,
                        entity,
                        Map.class
                    );
                    
                    Map<String, Object> paymentData = paymentResp.getBody();
                    if (paymentData != null) {
                        String statusStr = (String) paymentData.get("status");
                        String prefId = null;
                        
                        Map<String, Object> orderData = (Map<String, Object>) paymentData.get("order");
                        if (paymentData.containsKey("preference_id")) {
                            prefId = (String) paymentData.get("preference_id");
                        } else if (orderData != null && orderData.containsKey("preference_id")) {
                            prefId = (String) orderData.get("preference_id");
                        }
                        
                        if (prefId != null) {
                            PedidoStatus novoStatus = "approved".equals(statusStr) ? PedidoStatus.PAGAMENTO_APROVADO : PedidoStatus.PENDENTE;
                            pedidoService.atualizarStatusPagamento(prefId, novoStatus, paymentId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao processar Webhook do Mercado Pago", e);
        }
        
        return ResponseEntity.ok().build();
    }
}
