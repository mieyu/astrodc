package mie.astronomy.controller;

import mie.astronomy.config.AccessProperties;
import mie.astronomy.config.AccessTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessControllerTest {

    @Test
    void meRejectsRequestWithoutAccessCookie() {
        AccessProperties properties = new AccessProperties();
        properties.setKey("test-access-key");
        AccessTokenService service = new AccessTokenService(properties);
        AccessController controller = new AccessController(service, properties);

        ResponseEntity<Map<String, Object>> response = controller.me(new MockHttpServletRequest());

        assertEquals(401, response.getStatusCode().value());
    }
}
