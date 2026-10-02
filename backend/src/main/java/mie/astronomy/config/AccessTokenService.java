package mie.astronomy.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class AccessTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AccessProperties properties;

    public AccessTokenService(AccessProperties properties) {
        this.properties = properties;
    }

    public String createToken() {
        return createToken(Instant.now());
    }

    String createToken(Instant issuedAt) {
        String issuedAtMillis = Long.toString(issuedAt.toEpochMilli());
        return encode(issuedAtMillis) + "." + encode(sign(issuedAtMillis));
    }

    public boolean isRequestUnlocked(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        for (Cookie cookie : request.getCookies()) {
            if (properties.getCookieName().equals(cookie.getName())) {
                return isValid(cookie.getValue(), Instant.now());
            }
        }
        return false;
    }

    boolean isValid(String token, Instant now) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) {
            return false;
        }
        try {
            String issuedAtMillis = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            byte[] expectedSignature = sign(issuedAtMillis);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return false;
            }
            long issuedAt = Long.parseLong(issuedAtMillis);
            long ageMillis = now.toEpochMilli() - issuedAt;
            return ageMillis >= 0 && ageMillis <= properties.getCookieMaxAgeSeconds() * 1000;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public boolean matchesKey(String submittedKey) {
        if (submittedKey == null || properties.getKey() == null || properties.getKey().isBlank()) {
            return false;
        }
        byte[] expected = properties.getKey().getBytes(StandardCharsets.UTF_8);
        byte[] actual = submittedKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.getKey().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign access token", ex);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
