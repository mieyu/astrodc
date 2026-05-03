package mie.astronomy.service.external;

import mie.astronomy.config.EphemerisProperties;
import mie.astronomy.dto.EphemerisCalculateRequest;
import mie.astronomy.dto.EphemerisRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MsuClient {

    private static final Logger log = LoggerFactory.getLogger(MsuClient.class);
    private static final Pattern DATA_LINE = Pattern.compile("^\\d{4}\\s+\\d+\\s+\\d+.*");

    private final RestTemplate restTemplate;
    private final EphemerisProperties props;

    public MsuClient(@Qualifier("msuRestTemplate") RestTemplate restTemplate,
                     EphemerisProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    public List<EphemerisRow> fetch(EphemerisCalculateRequest req) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("langue", "30");
        form.add("plnvar", req.getPlnvar());
        form.add("satellite", req.getSatellite());
        form.add("relative", "-1");
        form.add("nde", req.getNde());
        form.add("observatory", req.getObservatory());
        form.add("epoch", "ICRF");
        form.add("tscale", "UTC");
        form.add("initform", "1");
        form.add("initmom", req.getInitmom());
        form.add("steptype", "1");
        form.add("timestep", req.getTimestep());
        form.add("ntimes", req.getNtimes());
        form.add("outputtype", "0");
        form.add("vangle", "0");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "Mozilla/5.0");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        log.info("Requesting MSU ephemeris: satellite={}, observatory={}, initmom={}",
                req.getSatellite(), req.getObservatory(), req.getInitmom());

        String html = restTemplate.postForObject(props.getMsuUrl(), entity, String.class);

        if (html == null || html.isBlank()) {
            throw new IllegalStateException("MSU 返回空响应");
        }

        Document doc = Jsoup.parse(html);
        Element pre = doc.selectFirst("pre");
        if (pre == null) {
            log.warn("MSU 响应中未找到 <pre> 标签,响应前 500 字符: {}",
                    html.substring(0, Math.min(500, html.length())));
            throw new IllegalStateException("未找到数据区域 (pre tag missing)");
        }

        // 必须用 wholeText() 而不是 text():后者会把换行折叠成空格
        String preContent = pre.wholeText();

        List<EphemerisRow> rows = new ArrayList<>();
        for (String line : preContent.split("\\r?\\n")) {
            String trimmed = line.trim();
            Matcher m = DATA_LINE.matcher(trimmed);
            if (!m.matches()) {
                continue;
            }
            String[] p = trimmed.split("\\s+");
            if (p.length < 12) {
                continue;
            }

            int sec;
            try {
                sec = (int) Double.parseDouble(p[5]);
            } catch (NumberFormatException e) {
                continue;
            }

            String time = String.format("%s-%s-%s %s:%s:%02d", p[0], p[1], p[2], p[3], p[4], sec);
            String ra = String.format("%sh %sm %ss", p[6], p[7], p[8]);
            String de = String.format("%s° %s' %s\"", p[9], p[10], p[11]);
            String raPure = String.format("%s %s %s", p[6], p[7], p[8]);
            String dePure = String.format("%s %s %s", p[9], p[10], p[11]);

            rows.add(new EphemerisRow(time, ra, de, raPure, dePure, line));
        }

        if (rows.isEmpty()) {
            String snippet = preContent.length() > 500
                    ? preContent.substring(0, 500) + "..."
                    : preContent;
            log.warn("MSU 解析到 0 行数据,<pre> 内容: {}", snippet);
            throw new IllegalStateException("未从返回内容中解析到星历数据,MSU 返回片段: "
                    + snippet.replaceAll("\\s+", " ").trim());
        }

        return rows;
    }
}
