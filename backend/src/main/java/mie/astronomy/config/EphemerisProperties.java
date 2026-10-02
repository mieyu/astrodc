package mie.astronomy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ephemeris")
public class EphemerisProperties {
    private String msuUrl;
    private String dssUrl;
    private String dssSurvey = "poss2ukstu_red";
    private int connectTimeoutMs = 10000;
    private int msuReadTimeoutMs = 15000;
    private int dssReadTimeoutMs = 60000;
    private Proxy proxy = new Proxy();

    @Data
    public static class Proxy {
        private boolean enabled;
        private String host;
        private int port;
    }
}
