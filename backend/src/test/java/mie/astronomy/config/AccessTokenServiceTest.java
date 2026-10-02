package mie.astronomy.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AccessTokenServiceTest {

    @Test
    void rejectsUnlockWhenAccessKeyIsNotConfigured() {
        AccessProperties properties = new AccessProperties();
        AccessTokenService service = new AccessTokenService(properties);

        assertFalse(service.matchesKey(""));
        assertFalse(service.matchesKey("test-access-key"));
        properties.setKey(null);
        assertFalse(service.matchesKey(""));
    }

    @Test
    void createsValidSignedTokenAndRejectsTampering() {
        AccessProperties properties = new AccessProperties();
        properties.setKey("test-access-key");
        properties.setCookieMaxAgeSeconds(Duration.ofDays(7).toSeconds());
        AccessTokenService service = new AccessTokenService(properties);

        String token = service.createToken(Instant.parse("2026-06-23T12:00:00Z"));

        assertTrue(service.isValid(token, Instant.parse("2026-06-24T12:00:00Z")));
        assertFalse(service.isValid(token + "x", Instant.parse("2026-06-24T12:00:00Z")));
    }

    @Test
    void rejectsExpiredToken() {
        AccessProperties properties = new AccessProperties();
        properties.setKey("test-access-key");
        properties.setCookieMaxAgeSeconds(Duration.ofDays(7).toSeconds());
        AccessTokenService service = new AccessTokenService(properties);

        String token = service.createToken(Instant.parse("2026-06-01T00:00:00Z"));

        assertFalse(service.isValid(token, Instant.parse("2026-06-10T00:00:01Z")));
    }
}
