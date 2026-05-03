package mie.astronomy.service.external;

import mie.astronomy.config.EphemerisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DssClient {

    private static final Logger log = LoggerFactory.getLogger(DssClient.class);
    private static final int MIN_VALID_BYTES = 1024;

    /** key = "{ra}_{dec}_{fov}", value = 已下载文件的绝对路径 */
    private final ConcurrentHashMap<String, Path> cache = new ConcurrentHashMap<>();

    private final RestTemplate restTemplate;
    private final EphemerisProperties props;

    public DssClient(@Qualifier("dssRestTemplate") RestTemplate restTemplate,
                     EphemerisProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    /**
     * 下载 DSS 图像到指定路径。命中缓存时直接复制已有文件,否则发请求拉取。
     * 失败抛 IllegalStateException(消息会原样进 success=false 的 message)。
     */
    public void download(String ra, String dec, double fov, Path outputFile) {
        String cacheKey = ra + "_" + dec + "_" + fov;
        Path cached = cache.get(cacheKey);
        if (cached != null && Files.exists(cached)) {
            if (cached.equals(outputFile)) {
                return;
            }
            try {
                Files.copy(cached, outputFile, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException e) {
                log.warn("缓存文件复制失败,改为重新下载: {}", e.getMessage());
            }
        }

        URI uri = UriComponentsBuilder.fromUriString(props.getDssUrl())
                .queryParam("v", props.getDssSurvey())
                .queryParam("r", ra)
                .queryParam("d", dec)
                .queryParam("e", "J2000")
                .queryParam("h", fov)
                .queryParam("w", fov)
                .queryParam("f", "gif")
                .queryParam("c", "none")
                .queryParam("fov", "NONE")
                .queryParam("v3", "")
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");

        log.info("Downloading DSS: ra={}, dec={}, fov={}", ra, dec, fov);

        ResponseEntity<byte[]> resp;
        try {
            resp = restTemplate.exchange(
                    new RequestEntity<>(headers, HttpMethod.GET, uri), byte[].class);
        } catch (Exception e) {
            log.error("DSS 请求失败", e);
            throw new IllegalStateException("从 DSS 服务器获取图片失败");
        }

        byte[] body = resp.getBody();
        if (body == null || body.length < MIN_VALID_BYTES) {
            throw new IllegalStateException("从 DSS 服务器获取图片失败");
        }

        String contentType = resp.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType != null
                && !contentType.contains("image")
                && !contentType.contains("fits")) {
            log.warn("DSS 返回非图像 Content-Type: {}", contentType);
            throw new IllegalStateException("从 DSS 服务器获取图片失败");
        }

        try {
            Files.write(outputFile, body);
        } catch (IOException e) {
            log.error("写文件失败: {}", outputFile, e);
            throw new IllegalStateException("从 DSS 服务器获取图片失败");
        }

        cache.put(cacheKey, outputFile);
    }
}
