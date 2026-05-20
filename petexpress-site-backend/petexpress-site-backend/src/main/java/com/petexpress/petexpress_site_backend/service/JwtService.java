package com.petexpress.petexpress_site_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expirationMinutes;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.jwt-secret}") String secret,
            @Value("${app.jwt-expiration-minutes:480}") long expirationMinutes
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Long userId, String email) {
        long now = Instant.now().getEpochSecond();
        long expiration = now + (expirationMinutes * 60);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("email", email);
        payload.put("iat", now);
        payload.put("exp", expiration);

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signature = sign(headerPart + "." + payloadPart);
        return headerPart + "." + payloadPart + "." + signature;
    }

    public Optional<AuthUser> validateToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return Optional.empty();
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = sign(unsignedToken);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<>() {}
            );

            long expiration = asLong(payload.get("exp"));
            if (expiration < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            Long userId = Long.valueOf(String.valueOf(payload.get("sub")));
            String email = String.valueOf(payload.get("email"));
            return Optional.of(new AuthUser(userId, email));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar token.", ex);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel assinar token.", ex);
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] first = a.getBytes(StandardCharsets.UTF_8);
        byte[] second = b.getBytes(StandardCharsets.UTF_8);
        if (first.length != second.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < first.length; i++) {
            result |= first[i] ^ second[i];
        }
        return result == 0;
    }

    public record AuthUser(Long id, String email) {}
}
