package mie.astronomy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
public class EphemerisRestTemplateConfig {

    @Bean("msuRestTemplate")
    public RestTemplate msuRestTemplate(EphemerisProperties props) {
        return build(props, props.getMsuReadTimeoutMs());
    }

    @Bean("dssRestTemplate")
    public RestTemplate dssRestTemplate(EphemerisProperties props) {
        return build(props, props.getDssReadTimeoutMs());
    }

    private RestTemplate build(EphemerisProperties props, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(readTimeoutMs);

        EphemerisProperties.Proxy proxyCfg = props.getProxy();
        if (proxyCfg != null && proxyCfg.isEnabled()
                && proxyCfg.getHost() != null && !proxyCfg.getHost().isBlank()) {
            factory.setProxy(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyCfg.getHost(), proxyCfg.getPort())));
        }

        return new RestTemplate(factory);
    }
}
