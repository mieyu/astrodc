package mie.astronomy.controller;

import mie.astronomy.config.AccessProperties;
import mie.astronomy.config.AccessTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/access")
public class AccessController {
    private final AccessTokenService accessTokenService;
    private final AccessProperties properties;

    public AccessController(AccessTokenService accessTokenService, AccessProperties properties) {
        this.accessTokenService = accessTokenService;
        this.properties = properties;
    }

    @PostMapping("/unlock")
    public ResponseEntity<Map<String, Object>> unlock(@RequestBody Map<String, String> body) {
        if (!accessTokenService.matchesKey(body.get("key"))) {
            return ResponseEntity.status(401).body(Map.of("unlocked", false, "message", "????"));
        }
        ResponseCookie cookie = baseCookie(accessTokenService.createToken())
                .maxAge(Duration.ofSeconds(properties.getCookieMaxAgeSeconds()))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("unlocked", true));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpServletRequest request) {
        if (!accessTokenService.isRequestUnlocked(request)) {
            return ResponseEntity.status(401).body(Map.of("unlocked", false));
        }
        return ResponseEntity.ok(Map.of("unlocked", true));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        ResponseCookie cookie = baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("unlocked", false));
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(properties.getCookieName(), value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path("/");
    }
}
