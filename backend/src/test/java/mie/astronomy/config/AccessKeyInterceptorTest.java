package mie.astronomy.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AccessKeyInterceptorTest {

    @Test
    void rejectsBusinessRequestWithoutAccessCookie() throws Exception {
        AccessProperties properties = new AccessProperties();
        properties.setKey("test-access-key");
        AccessTokenService service = new AccessTokenService(properties);
        AccessKeyInterceptor interceptor = new AccessKeyInterceptor(service, properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/image/own/stats");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void allowsBusinessRequestWithValidAccessCookie() throws Exception {
        AccessProperties properties = new AccessProperties();
        properties.setKey("test-access-key");
        AccessTokenService service = new AccessTokenService(properties);
        AccessKeyInterceptor interceptor = new AccessKeyInterceptor(service, properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/image/own/stats");
        request.setCookies(new Cookie(properties.getCookieName(), service.createToken(Instant.now())));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(200, response.getStatus());
    }
}
