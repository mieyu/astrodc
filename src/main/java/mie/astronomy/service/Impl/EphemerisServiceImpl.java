package mie.astronomy.service.Impl;

import jakarta.annotation.PostConstruct;
import mie.astronomy.dto.EphemerisCalculateRequest;
import mie.astronomy.dto.EphemerisChartRequest;
import mie.astronomy.dto.EphemerisRow;
import mie.astronomy.service.EphemerisService;
import mie.astronomy.service.external.DssClient;
import mie.astronomy.service.external.MsuClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EphemerisServiceImpl implements EphemerisService {

    private static final Logger log = LoggerFactory.getLogger(EphemerisServiceImpl.class);
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FMT  = DateTimeFormatter.ofPattern("yyyyMMdd_HH'h'mm'm'ss's'");

    private final MsuClient msuClient;
    private final DssClient dssClient;
    private Path chartCacheDir;

    public EphemerisServiceImpl(MsuClient msuClient, DssClient dssClient) {
        this.msuClient = msuClient;
        this.dssClient = dssClient;
    }

    @PostConstruct
    public void init() throws IOException {
        chartCacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "astronomy-ephemeris")
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(chartCacheDir);
        log.info("Ephemeris chart cache dir: {}", chartCacheDir);
    }

    @Override
    public String getChartCacheDir() {
        return chartCacheDir.toString();
    }

    @Override
    public Map<String, Object> calculate(EphemerisCalculateRequest req) {
        if (isBlank(req.getSatellite()) || isBlank(req.getObservatory()) || isBlank(req.getInitmom())) {
            return fail("缺少必要参数 (卫星、观测站或时间)");
        }
        try {
            List<EphemerisRow> rows = msuClient.fetch(req);
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("success", true);
            ok.put("data", rows);
            return ok;
        } catch (ResourceAccessException e) {
            log.error("MSU 调用超时/不可达", e);
            return fail("请求超时,请检查网络或重试");
        } catch (IllegalStateException e) {
            log.warn("MSU 解析失败: {}", e.getMessage());
            return fail(e.getMessage());
        } catch (Exception e) {
            log.error("calculate 未知错误", e);
            return fail("发生错误: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> downloadChart(EphemerisChartRequest req) {
        if (isBlank(req.getRa()) || isBlank(req.getDec())) {
            return fail("缺少必要参数 (赤经或赤纬)");
        }

        double fovVal;
        try {
            fovVal = req.getFov() == null ? 7.0 : req.getFov();
            if (fovVal <= 0) fovVal = 7.0;
        } catch (Exception e) {
            fovVal = 7.0;
        }

        // 文件名:{sat}_{YYYYMMdd_HHhmmmss}_fov{int}.gif
        String satClean = req.getSatelliteName() == null ? "Unknown"
                : req.getSatelliteName().split("\\(")[0].trim();
        if (satClean.isEmpty()) satClean = "Unknown";

        String formattedTime;
        try {
            LocalDateTime dt = LocalDateTime.parse(req.getTimeStr(), INPUT_FMT);
            formattedTime = dt.format(FILE_FMT);
        } catch (DateTimeParseException | NullPointerException e) {
            formattedTime = LocalDateTime.now().format(FILE_FMT);
        }

        String fovStr;
        if (Math.abs(fovVal - Math.round(fovVal)) < 0.001) {
            fovStr = String.valueOf((int) Math.round(fovVal));
        } else {
            fovStr = String.valueOf(fovVal);
        }

        String filename = String.format("%s_%s_fov%s.gif", sanitize(satClean), formattedTime, fovStr);
        Path outputFile = chartCacheDir.resolve(filename).normalize();

        // 路径越界保险(filename 里如果含 / 或 .. 被 sanitize 拦下,这里再校一道)
        if (!outputFile.startsWith(chartCacheDir)) {
            return fail("从 DSS 服务器获取图片失败");
        }

        try {
            dssClient.download(req.getRa(), req.getDec(), fovVal, outputFile);
        } catch (ResourceAccessException e) {
            log.error("DSS 调用超时/不可达", e);
            return fail("从 DSS 服务器获取图片失败");
        } catch (IllegalStateException e) {
            return fail(e.getMessage());
        } catch (Exception e) {
            log.error("DSS 调用未知错误", e);
            return fail("从 DSS 服务器获取图片失败");
        }

        String pathParam = URLEncoder.encode(outputFile.toString(), StandardCharsets.UTF_8);
        String fileParam = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        String downloadUrl = "/api/ephemeris/get_file?path=" + pathParam + "&filename=" + fileParam;

        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("success", true);
        ok.put("download_url", downloadUrl);
        ok.put("filename", filename);
        return ok;
    }

    private static Map<String, Object> fail(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("message", message);
        return m;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
