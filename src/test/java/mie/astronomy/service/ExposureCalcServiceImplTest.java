package mie.astronomy.service;

import mie.astronomy.dto.CameraParams;
import mie.astronomy.dto.ExposureCalcResult;
import mie.astronomy.service.Impl.ExposureCalcServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用手工合成的 FITS（int16, BZERO=32768）验证 Java 版曝光计算算法。
 */
class ExposureCalcServiceImplTest {

    private static final int W = 128, H = 128;
    private static final int BLOCK = 2880, CARD = 80;

    private long seed = 42;
    private double rnd() { seed = (seed * 1103515245 + 12345) & 0x7fffffffL; return (double) seed / 0x7fffffff; }
    private double gauss() { return Math.sqrt(-2 * Math.log(rnd() + 1e-9)) * Math.cos(2 * Math.PI * rnd()); }

    private String card(String s) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < CARD) sb.append(' ');
        return sb.substring(0, CARD);
    }

    private byte[] buildFits(boolean planet, boolean stars) {
        double[] phys = new double[W * H];
        for (int i = 0; i < phys.length; i++) phys[i] = 1000 + gauss() * 20;
        if (planet) {
            int cx = 64, cy = 64, R = 14;
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++) {
                    double d = Math.hypot(x - cx, y - cy);
                    if (d < R) phys[y * W + x] += 8000 * (1 - d / R) + 2000;
                }
        }
        if (stars) {
            int[][] pts = {{30, 30, 12000}, {90, 40, 9000}, {40, 95, 15000}, {100, 100, 7000}, {70, 20, 11000}};
            for (int[] p : pts) {
                int cx = p[0], cy = p[1], amp = p[2];
                for (int y = cy - 3; y <= cy + 3; y++)
                    for (int x = cx - 3; x <= cx + 3; x++) {
                        double d2 = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                        phys[y * W + x] += amp * Math.exp(-d2 / 4.0);
                    }
            }
        }

        StringBuilder hdr = new StringBuilder();
        hdr.append(card("SIMPLE  =                    T"));
        hdr.append(card("BITPIX  =                   16"));
        hdr.append(card("NAXIS   =                    2"));
        hdr.append(card("NAXIS1  =                  " + W));
        hdr.append(card("NAXIS2  =                  " + H));
        hdr.append(card("BZERO   =                32768"));
        hdr.append(card("BSCALE  =                    1"));
        hdr.append(card("EXPTIME =                 10.0"));
        hdr.append(card("END"));
        int pad = (int) (Math.ceil(hdr.length() / (double) BLOCK) * BLOCK) - hdr.length();
        for (int i = 0; i < pad; i++) hdr.append(' ');

        int dataBytes = W * H * 2;
        int dataBlocks = (int) (Math.ceil(dataBytes / (double) BLOCK) * BLOCK);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < hdr.length(); i++) baos.write(hdr.charAt(i) & 0xFF);
        for (int i = 0; i < phys.length; i++) {
            int raw = (int) Math.round(phys[i]) - 32768;
            if (raw < -32768) raw = -32768;
            if (raw > 32767) raw = 32767;
            baos.write((raw >> 8) & 0xFF); // big-endian high byte
            baos.write(raw & 0xFF);
        }
        int written = hdr.length() + dataBytes;
        for (int i = written; i < hdr.length() + dataBlocks; i++) baos.write(0);
        return baos.toByteArray();
    }

    @Test
    void planetMode() throws Exception {
        ExposureCalcServiceImpl svc = new ExposureCalcServiceImpl();
        MockMultipartFile file = new MockMultipartFile("file", "planet.fits", "application/octet-stream", buildFits(true, false));
        ExposureCalcResult r = svc.analyze(file, "planet", 30, 60, new CameraParams(2.0, 6.33, 0.002));

        assertTrue(r.isValid(), "planet should be valid: " + r.getMessage());
        assertEquals(1000.0, r.getDiagnostics().get("backgroundAdu"), 5.0, "background ~1000");
        assertNotNull(r.getPlanet());
        assertEquals(64.0, r.getPlanet().getCx(), 2.0);
        assertEquals(64.0, r.getPlanet().getCy(), 2.0);
        assertTrue(r.getPlanet().getRadius() > 10 && r.getPlanet().getRadius() < 20);
        assertTrue(r.getExptimeRecommended() > 0);
        assertNotNull(r.getReport());
        assertTrue(r.getReport().contains("行星曝光时间分析报告"));
        assertNotNull(r.getImagePngBase64());
        assertFalse(r.getImagePngBase64().isEmpty());
    }

    @Test
    void starMode() throws Exception {
        ExposureCalcServiceImpl svc = new ExposureCalcServiceImpl();
        MockMultipartFile file = new MockMultipartFile("file", "stars.fits", "application/octet-stream", buildFits(false, true));
        ExposureCalcResult r = svc.analyze(file, "star", 100, 300, new CameraParams(2.0, 6.33, 0.002));

        assertTrue(r.isValid(), "star should be valid: " + r.getMessage());
        assertEquals(5, r.getStarsDetected(), "should detect 5 sources");
        assertTrue(r.getStarsReliable() >= 1);
        assertNotNull(r.getSelectedStar());
        assertTrue(r.getSelectedStar().isSelected());
        assertTrue(r.getExptimeRecommended() > 0);
        assertNotNull(r.getConditionAssessment());
        assertTrue(r.getReport().contains("恒星曝光时间分析报告"));
    }
}
