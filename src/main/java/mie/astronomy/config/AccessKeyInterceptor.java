package mie.astronomy.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AccessKeyInterceptor implements HandlerInterceptor {
    private final AccessTokenService accessTokenService;
    private final AccessProperties properties;

    public AccessKeyInterceptor(AccessTokenService accessTokenService, AccessProperties properties) {
        this.accessTokenService = accessTokenService;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (isPublicRequest(request)) {
            return true;
        }
        if (accessTokenService.isRequestUnlocked(request)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":401,\"message\":\"access key required\"}");
        return false;
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(method)
                || path.startsWith("/api/access/")
                || "/analyze/health".equals(path)
                || "/error".equals(path);
    }
}
