package mie.astronomy.service.Impl;

import mie.astronomy.dto.*;
import mie.astronomy.service.DeepSeekService;
import mie.astronomy.service.ExposureCalcService;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

/**
 * 天文曝光时间计算器核心算法（由 Python 版 saturn_exposure_calculator 移植到 Java）。
 *
 * 计算流程：FITS 解析(应用 BZERO/BSCALE) → Sigma-clipping 背景估计 →
 * 行星/恒星检测 → 完整物理噪声模型 → 二次方程求解曝光时间 → 观测条件评估 → 文本报告。
 */
@Service
public class ExposureCalcServiceImpl implements ExposureCalcService {

    private static final int MAX_ADU = 65535;
    private static final String LINE = "=".repeat(50);

    @Autowired
    private DeepSeekService deepSeekService;

    // =====================================================================
    // FITS 图像容器
    // =====================================================================
    private static final class FitsImage {
        double[] data;
        int w, h;
        double exptime = 10.0;
        double min, max;
    }

    // =====================================================================
    // 对外入口
    // =====================================================================
    @Override
    public ExposureCalcResult analyze(MultipartFile file, String mode, double targetSnr,
                                      double maxExposure, CameraParams cam) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }
        FitsImage img = readImage(file);

        boolean star = "star".equalsIgnoreCase(mode);
        ExposureCalcResult result = star
                ? analyzeStars(img, targetSnr, cam, 3.0, 10.0, maxExposure, 2)
                : analyzePlanet(img, targetSnr, cam, maxExposure);

        result.setMode(star ? "star" : "planet");
        result.setTargetSnr(targetSnr);
        result.setCameraParams(cam);
        result.setWidth(img.w);
        result.setHeight(img.h);
        result.setImagePngBase64(renderPng(img));

        if (star) {
            result.setReport(buildStarReport(result));
            result.setPreviewSummary(buildStarPreview(result));
            markStars(result);
        } else {
            result.setReport(buildPlanetReport(result));
            result.setPreviewSummary(buildPlanetPreview(result));
        }
        return result;
    }

    @Override
    public String aiAnalyze(ExposureCalcResult result) {
        String prompt = buildAiPrompt(result);
        String text = deepSeekService.chat(AI_SYSTEM_PROMPT, prompt, 0.7);
        if (text == null || text.isBlank()) {
            return "AI 未返回内容（请检查后端 LLM 配置或网络）";
        }
        return text;
    }

    // =====================================================================
    // FITS 解析（应用 BZERO/BSCALE，遍历 HDU 找首个 2D 图像）
    // =====================================================================
    private FitsImage readImage(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream()) {
            Fits fits = new Fits(is);
            ImageHDU hdu = null;
            BasicHDU<?> basic;
            while ((basic = fits.readHDU()) != null) {
                if (basic instanceof ImageHDU && basic.getKernel() != null
                        && basic.getAxes() != null && basic.getAxes().length == 2) {
                    hdu = (ImageHDU) basic;
                    break;
                }
            }
            if (hdu == null) {
                fits.close();
                throw new IllegalArgumentException("FITS 文件中没有找到有效的 2D 图像数据");
            }

            int[] axes = hdu.getAxes(); // [height(NAXIS2), width(NAXIS1)]
            int h = axes[0];
            int w = axes[1];
            Header header = hdu.getHeader();
            int bitpix = header.getIntValue("BITPIX");
            double bzero = header.getDoubleValue("BZERO", 0.0);
            double bscale = header.getDoubleValue("BSCALE", 1.0);

            FitsImage img = new FitsImage();
            img.w = w;
            img.h = h;
            img.data = flattenScaled(hdu.getKernel(), bitpix, w * h, bzero, bscale);

            for (String key : new String[]{"EXPTIME", "EXPOSURE", "EXP_TIME", "EXP"}) {
                if (header.containsKey(key)) {
                    double v = header.getDoubleValue(key, Double.NaN);
                    if (!Double.isNaN(v)) { img.exptime = v; break; }
                }
            }

            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for (double v : img.data) { if (v < min) min = v; if (v > max) max = v; }
            img.min = min;
            img.max = max;

            fits.close();
            return img;
        }
    }

    private double[] flattenScaled(Object kernel, int bitpix, int total, double bzero, double bscale) {
        double[] flat = new double[total];
        int[] idx = {0};
        switch (bitpix) {
            case 8:
                fill8(kernel, flat, idx, bzero, bscale);
                break;
            case 16:
                fill16(kernel, flat, idx, bzero, bscale);
                break;
            case 32:
                fill32(kernel, flat, idx, bzero, bscale);
                break;
            case 64:
                fill64(kernel, flat, idx, bzero, bscale);
                break;
            case -32:
                fillF(kernel, flat, idx, bzero, bscale);
                break;
            case -64:
                fillD(kernel, flat, idx, bzero, bscale);
                break;
            default:
                throw new UnsupportedOperationException("不支持的 BITPIX: " + bitpix);
        }
        return flat;
    }

    private void fill8(Object k, double[] f, int[] i, double z, double s) {
        for (byte[] row : (byte[][]) k) for (byte v : row) f[i[0]++] = z + s * (v & 0xFF);
    }
    private void fill16(Object k, double[] f, int[] i, double z, double s) {
        for (short[] row : (short[][]) k) for (short v : row) f[i[0]++] = z + s * v;
    }
    private void fill32(Object k, double[] f, int[] i, double z, double s) {
        for (int[] row : (int[][]) k) for (int v : row) f[i[0]++] = z + s * v;
    }
    private void fill64(Object k, double[] f, int[] i, double z, double s) {
        for (long[] row : (long[][]) k) for (long v : row) f[i[0]++] = z + s * v;
    }
    private void fillF(Object k, double[] f, int[] i, double z, double s) {
        for (float[] row : (float[][]) k) for (float v : row) f[i[0]++] = z + s * v;
    }
    private void fillD(Object k, double[] f, int[] i, double z, double s) {
        for (double[] row : (double[][]) k) for (double v : row) f[i[0]++] = z + s * v;
    }

    // =====================================================================
    // 基础数值工具
    // =====================================================================
    private double stdev(double[] arr, int n) {
        if (n == 0) return 0;
        double m = 0;
        for (int i = 0; i < n; i++) m += arr[i];
        m /= n;
        double s = 0;
        for (int i = 0; i < n; i++) { double d = arr[i] - m; s += d * d; }
        return Math.sqrt(s / n);
    }

    private double medianSorted(double[] arr, int n) {
        if (n == 0) return 0;
        return (n % 2 == 1) ? arr[(n - 1) / 2] : (arr[n / 2 - 1] + arr[n / 2]) / 2.0;
    }

    private double medianOf(List<Double> values) {
        double[] a = new double[values.size()];
        for (int i = 0; i < a.length; i++) a[i] = values.get(i);
        Arrays.sort(a);
        return medianSorted(a, a.length);
    }

    // =====================================================================
    // 1. Sigma-clipping 背景估计
    // =====================================================================
    private double[] estimateBackground(double[] data) {
        double[] pixels = Arrays.copyOf(data, data.length);
        int n = pixels.length;
        for (int it = 0; it < 10; it++) {
            Arrays.sort(pixels, 0, n);
            double med = medianSorted(pixels, n);
            double std = stdev(pixels, n);
            if (std < 1e-6) break;
            double thr = 3.0 * std;
            int cnt = 0;
            for (int i = 0; i < n; i++) if (Math.abs(pixels[i] - med) < thr) cnt++;
            if (cnt < 100) break;
            if (cnt == n) break;
            double[] next = new double[cnt];
            int j = 0;
            for (int i = 0; i < n; i++) if (Math.abs(pixels[i] - med) < thr) next[j++] = pixels[i];
            pixels = next;
            n = cnt;
        }
        Arrays.sort(pixels, 0, n);
        return new double[]{medianSorted(pixels, n), stdev(pixels, n)};
    }

    // =====================================================================
    // 形态学（border_value = 0）
    // =====================================================================
    private static final int[][] CROSS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {0, 0}};
    private static final int[][] BOX22 = {{-1, -1}, {-1, 0}, {0, -1}, {0, 0}};

    private boolean[] erode(boolean[] mask, int w, int h, int[][] off) {
        boolean[] out = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean ok = true;
                for (int[] o : off) {
                    int ny = y + o[0], nx = x + o[1];
                    if (ny < 0 || ny >= h || nx < 0 || nx >= w || !mask[ny * w + nx]) { ok = false; break; }
                }
                out[y * w + x] = ok;
            }
        }
        return out;
    }

    private boolean[] dilate(boolean[] mask, int w, int h, int[][] off) {
        boolean[] out = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean on = false;
                for (int[] o : off) {
                    int ny = y - o[0], nx = x - o[1]; // 膨胀对结构元反射
                    if (ny >= 0 && ny < h && nx >= 0 && nx < w && mask[ny * w + nx]) { on = true; break; }
                }
                out[y * w + x] = on;
            }
        }
        return out;
    }

    private boolean[] erodeN(boolean[] m, int w, int h, int[][] off, int n) {
        for (int i = 0; i < n; i++) m = erode(m, w, h, off);
        return m;
    }
    private boolean[] dilateN(boolean[] m, int w, int h, int[][] off, int n) {
        for (int i = 0; i < n; i++) m = dilate(m, w, h, off);
        return m;
    }

    // =====================================================================
    // 连通域标记（4-连通），返回每个连通域的像素索引
    // =====================================================================
    private List<int[]> labelComponents(boolean[] mask, int w, int h) {
        int[] labels = new int[w * h];
        int[] stack = new int[w * h];
        List<int[]> comps = new ArrayList<>();
        int cur = 0;
        for (int s = 0; s < w * h; s++) {
            if (mask[s] && labels[s] == 0) {
                cur++;
                int sp = 0;
                stack[sp++] = s;
                labels[s] = cur;
                // 第一遍统计大小，之后填充
                int[] tmp = new int[64];
                int tn = 0;
                while (sp > 0) {
                    int p = stack[--sp];
                    if (tn == tmp.length) tmp = Arrays.copyOf(tmp, tmp.length * 2);
                    tmp[tn++] = p;
                    int y = p / w, x = p - y * w;
                    if (x > 0) { int q = p - 1; if (mask[q] && labels[q] == 0) { labels[q] = cur; stack[sp++] = q; } }
                    if (x < w - 1) { int q = p + 1; if (mask[q] && labels[q] == 0) { labels[q] = cur; stack[sp++] = q; } }
                    if (y > 0) { int q = p - w; if (mask[q] && labels[q] == 0) { labels[q] = cur; stack[sp++] = q; } }
                    if (y < h - 1) { int q = p + w; if (mask[q] && labels[q] == 0) { labels[q] = cur; stack[sp++] = q; } }
                }
                comps.add(Arrays.copyOf(tmp, tn));
            }
        }
        return comps;
    }

    // =====================================================================
    // 行星检测
    // =====================================================================
    private static final class PlanetDet {
        boolean[] mask;
        double cy, cx, radius;
        int pixelCount;
    }

    private PlanetDet detectPlanet(double[] data, int w, int h, double bg, double bgStd) {
        double threshold = bg + 2.0 * bgStd;
        boolean[] mask = new boolean[w * h];
        for (int i = 0; i < data.length; i++) mask[i] = data[i] > threshold;

        mask = erodeN(mask, w, h, CROSS, 1);
        mask = dilateN(mask, w, h, CROSS, 2);

        List<int[]> comps = labelComponents(mask, w, h);
        if (comps.isEmpty()) return null;

        int[] best = comps.get(0);
        for (int[] c : comps) if (c.length > best.length) best = c;
        if (best.length < 20) return null;

        long sy = 0, sx = 0;
        for (int p : best) { int y = p / w; sy += y; sx += p - y * w; }
        PlanetDet d = new PlanetDet();
        d.cy = (double) sy / best.length;
        d.cx = (double) sx / best.length;
        d.radius = Math.sqrt(best.length / Math.PI);
        d.pixelCount = best.length;
        d.mask = new boolean[w * h];
        for (int p : best) d.mask[p] = true;
        return d;
    }

    // =====================================================================
    // 恒星检测
    // =====================================================================
    private List<ExposureStar> detectStars(double[] data, int w, int h, double bg, double bgStd,
                                           double detectionSigma, int minArea, int posMethod,
                                           double gain, double readNoise) {
        double threshold = bg + detectionSigma * bgStd;
        boolean[] mask = new boolean[w * h];
        for (int i = 0; i < data.length; i++) mask[i] = data[i] > threshold;

        mask = erodeN(mask, w, h, BOX22, 1);
        mask = dilateN(mask, w, h, BOX22, 1);

        int edgeMargin = 10;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (y < edgeMargin || y >= h - edgeMargin || x < edgeMargin || x >= w - edgeMargin) {
                    mask[y * w + x] = false;
                }
            }
        }

        List<int[]> comps = labelComponents(mask, w, h);
        if (comps.isEmpty()) return new ArrayList<>();

        int maxSources = 500;
        if (comps.size() > maxSources) {
            comps.sort((a, b) -> Integer.compare(b.length, a.length));
            comps = comps.subList(0, maxSources);
        }

        double dtBase = bg + 2.6 * bgStd;
        List<ExposureStar> stars = new ArrayList<>();
        for (int[] comp : comps) {
            int area = comp.length;
            if (area < minArea) continue;

            double sumW = 0, sumWX = 0, sumWY = 0, sumY = 0, sumX = 0;
            for (int p : comp) {
                int y = p / w, x = p - y * w;
                sumY += y; sumX += x;
                double dt = data[p] - dtBase;
                if (dt < 0) dt = 0;
                double wgt = Math.pow(dt + 1e-9, posMethod);
                sumW += wgt; sumWX += x * wgt; sumWY += y * wgt;
            }
            double cx, cy;
            if (sumW < 1e-9) { cy = sumY / area; cx = sumX / area; }
            else { cx = sumWX / sumW; cy = sumWY / sumW; }

            if (!(edgeMargin <= cx && cx < w - edgeMargin && edgeMargin <= cy && cy < h - edgeMargin)) continue;

            double total = 0, peak = Double.NEGATIVE_INFINITY;
            for (int p : comp) { double v = data[p]; total += v; if (v > peak) peak = v; }
            double totalFlux = total - bg * area;
            double meanFlux = totalFlux / area;
            boolean saturated = peak >= MAX_ADU * 0.95;

            double signalE = totalFlux * gain;
            double bgEPerPixel = bg * gain;
            double noiseVar = signalE + bgEPerPixel * area + readNoise * readNoise * area;
            double snrEst = noiseVar > 0 ? signalE / Math.sqrt(noiseVar) : 0;

            ExposureStar s = new ExposureStar();
            s.setCy(cy);
            s.setCx(cx);
            s.setArea(area);
            s.setRadius(Math.max(Math.sqrt(area), 4.0));
            s.setTotalFluxAdu(totalFlux);
            s.setMeanFluxAdu(meanFlux);
            s.setPeakAdu(peak);
            s.setSnrEstimate(snrEst);
            s.setSaturated(saturated);
            stars.add(s);
        }
        return stars;
    }

    // =====================================================================
    // 物理噪声模型 + 曝光求解（行星：每像素）
    // =====================================================================
    private Map<String, Object> calcSnrAndExposure(double meanSignalAdu, double bgAdu, double currentExptime,
                                                   double targetSnr, double gain, double readNoise, double darkCurrent) {
        double S0 = meanSignalAdu * gain;
        double B0 = bgAdu * gain;
        double R2 = readNoise * readNoise;
        double D = darkCurrent;
        double t0 = currentExptime;

        Map<String, Object> out = new HashMap<>();
        if (S0 <= 0) {
            out.put("snrCurrent", 0.0);
            out.put("tRecommended", t0 * 2.0);
            out.put("valid", false);
            out.put("message", "未检测到有效行星信号");
            return out;
        }

        double snrCurrentPixel = S0 / Math.sqrt(S0 + B0 + D * t0 + R2);
        double totalBg = B0 + D * t0;
        double a = S0 * S0;
        double b = targetSnr * targetSnr * (S0 + totalBg);
        double c = targetSnr * targetSnr * R2;
        double disc = b * b + 4 * a * c;
        double r = disc < 0 ? 2.0 : (b + Math.sqrt(disc)) / (2 * a);
        double tRec = t0 * r;

        double denom = S0 + B0 + D * t0 + R2;
        Map<String, Double> nb = new LinkedHashMap<>();
        nb.put("photonNoisePct", S0 / denom * 100);
        nb.put("skyNoisePct", B0 / denom * 100);
        nb.put("darkNoisePct", D * t0 / denom * 100);
        nb.put("readNoisePct", R2 / denom * 100);

        out.put("snrCurrent", snrCurrentPixel);
        out.put("tRecommended", tRec);
        out.put("valid", true);
        out.put("message", "计算成功");
        out.put("signalElectron", S0);
        out.put("backgroundElectron", B0);
        out.put("darkCurrentElectron", D * t0);
        out.put("readNoiseElectron", readNoise);
        out.put("noiseBudget", nb);
        return out;
    }

    // =====================================================================
    // 行星模式分析
    // =====================================================================
    private ExposureCalcResult analyzePlanet(FitsImage img, double targetSnr, CameraParams cam, double maxExposure) {
        ExposureCalcResult r = new ExposureCalcResult();
        Map<String, Double> diag = new LinkedHashMap<>();
        r.setDiagnostics(diag);
        r.setExptimeCurrent(img.exptime);

        double[] bgRes = estimateBackground(img.data);
        double bg = bgRes[0], bgStd = bgRes[1];
        diag.put("backgroundAdu", bg);
        diag.put("backgroundStdAdu", bgStd);

        if (bgStd < 1.0) {
            r.setMessage(String.format(Locale.US, "背景噪声异常低 (%.2f ADU)，图像可能已饱和或有问题", bgStd));
            return r;
        }

        PlanetDet det = detectPlanet(img.data, img.w, img.h, bg, bgStd);
        if (det == null) {
            r.setMessage("未检测到行星目标。请确认图像中包含行星。");
            return r;
        }

        ExposurePlanet planet = new ExposurePlanet();
        planet.setCx(det.cx);
        planet.setCy(det.cy);
        planet.setRadius(det.radius);
        planet.setPixelCount(det.pixelCount);
        r.setPlanet(planet);

        boolean[] eroded = erodeN(det.mask, img.w, img.h, CROSS, 3);
        int erodedCount = 0;
        for (boolean v : eroded) if (v) erodedCount++;
        if (erodedCount < 10) eroded = det.mask;

        double sum = 0, peak = Double.NEGATIVE_INFINITY;
        int cnt = 0;
        List<Double> vals = new ArrayList<>();
        for (int i = 0; i < eroded.length; i++) {
            if (eroded[i]) { double v = img.data[i]; sum += v; cnt++; vals.add(v); if (v > peak) peak = v; }
        }
        double meanPlanet = sum / cnt;
        double meanSignal = meanPlanet - bg;
        double peakNet = peak - bg;

        double overThr = MAX_ADU * 0.8;
        if (peak > overThr) {
            r.setValid(false);
            r.setMessage(String.format(Locale.US,
                    "⚠️ 当前图像可能过曝！\n   峰值亮度: %.0f ADU (%.1f%% 满量程)\n   建议: 缩短曝光时间，而非增加\n   推荐曝光: ≤ %.1f s (保持峰值<70%%满量程)",
                    peak, peak / MAX_ADU * 100, MAX_ADU * 0.7 / peakNet * img.exptime));
            diag.put("peakAdu", peak);
            diag.put("peakOverflowPct", peak / MAX_ADU * 100);
            return r;
        } else if (peak > MAX_ADU * 0.6) {
            r.setWarning(String.format(Locale.US,
                    "⚠️ 峰值亮度较高 (%.0f ADU, %.1f%% 满量程)\n   建议当前曝光时间不要大幅增加",
                    peak, peak / MAX_ADU * 100));
        }

        diag.put("peakAdu", peak);
        diag.put("peakOverflowPct", peak / MAX_ADU * 100);
        diag.put("planetMeanAdu", meanPlanet);
        diag.put("planetSignalAdu", meanSignal);
        diag.put("planetStdAdu", stdevOfList(vals));

        Map<String, Object> calc = calcSnrAndExposure(meanSignal, bg, img.exptime, targetSnr,
                cam.getGain(), cam.getReadNoise(), cam.getDarkCurrent());

        double snrCurrent = (double) calc.get("snrCurrent");
        double tRec = (double) calc.get("tRecommended");
        r.setSnrCurrent(snrCurrent);
        r.setExptimeRecommended(tRec);
        r.setValid((boolean) calc.get("valid"));
        r.setMessage((String) calc.get("message"));
        if (calc.get("noiseBudget") != null) {
            //noinspection unchecked
            r.setNoiseInfo((Map<String, Double>) calc.get("noiseBudget"));
        }
        planet.setSnr(snrCurrent);

        if (calc.containsKey("signalElectron") && tRec > maxExposure && (boolean) calc.get("valid")) {
            double S0 = (double) calc.get("signalElectron");
            double totalBg = (double) calc.get("backgroundElectron") + (double) calc.get("darkCurrentElectron");
            double R2 = Math.pow((double) calc.get("readNoiseElectron"), 2);
            double t0 = img.exptime;
            double rMax = maxExposure / t0;
            double noiseSq = (S0 + totalBg) * rMax + R2;
            r.setSuggestedSnr(noiseSq > 0 ? S0 * rMax / Math.sqrt(noiseSq) : 0);
            r.setMaxExposureUsed(maxExposure);
        }
        return r;
    }

    private double stdevOfList(List<Double> vals) {
        int n = vals.size();
        double[] a = new double[n];
        for (int i = 0; i < n; i++) a[i] = vals.get(i);
        return stdev(a, n);
    }

    // =====================================================================
    // 恒星模式分析
    // =====================================================================
    private ExposureCalcResult analyzeStars(FitsImage img, double targetSnr, CameraParams cam,
                                            double detectionSigma, double minSnrFilter,
                                            double maxExposure, int posMethod) {
        ExposureCalcResult r = new ExposureCalcResult();
        Map<String, Double> diag = new LinkedHashMap<>();
        r.setDiagnostics(diag);
        r.setExptimeCurrent(img.exptime);

        double[] bgRes = estimateBackground(img.data);
        double bg = bgRes[0], bgStd = bgRes[1];
        diag.put("backgroundAdu", bg);
        diag.put("backgroundStdAdu", bgStd);

        if (bgStd < 1.0) {
            r.setMessage(String.format(Locale.US, "背景噪声异常低 (%.2f ADU)，图像可能已饱和或有问题", bgStd));
            return r;
        }

        double gain = cam.getGain(), rn = cam.getReadNoise(), dark = cam.getDarkCurrent();
        List<ExposureStar> stars = detectStars(img.data, img.w, img.h, bg, bgStd, detectionSigma, 4, posMethod, gain, rn);
        r.setStarsDetected(stars.size());

        int saturatedCount = 0;
        for (ExposureStar s : stars) if (s.isSaturated()) saturatedCount++;
        diag.put("saturatedCount", (double) saturatedCount);

        if (stars.isEmpty()) {
            r.setMessage("未检测到恒星目标。请确认图像中包含恒星。");
            return r;
        }

        List<ExposureStar> reliable = new ArrayList<>();
        for (ExposureStar s : stars) if (s.getSnrEstimate() >= minSnrFilter) reliable.add(s);
        List<ExposureStar> reliableUnsat = new ArrayList<>();
        for (ExposureStar s : reliable) if (!s.isSaturated()) reliableUnsat.add(s);

        List<ExposureStar> reliableFiltered;
        if (reliableUnsat.isEmpty() && !reliable.isEmpty()) {
            r.setWarning("⚠️ 所有 " + reliable.size() + " 颗可靠恒星均已过曝！建议缩短曝光时间。");
            reliableFiltered = reliable;
            r.setAllReliableSaturated(true);
        } else {
            reliableFiltered = reliableUnsat;
            r.setAllReliableSaturated(false);
        }

        r.setStarsReliable(reliableFiltered.size());
        r.setStarsSaturated(reliable.size() - reliableUnsat.size());
        r.setStars(stars);

        if (reliableFiltered.isEmpty()) {
            double maxSnr = 0;
            for (ExposureStar s : stars) maxSnr = Math.max(maxSnr, s.getSnrEstimate());
            r.setMessage(String.format(Locale.US,
                    "检测到 %d 个源，但无一达到最低SNR=%.1f要求。\n最亮源SNR≈%.1f，建议增加曝光时间或降低SNR过滤阈值。",
                    stars.size(), minSnrFilter, maxSnr));
            return r;
        }

        List<ExposureStar> rel = reliableFiltered;

        // 选星：峰值中位数附近
        List<Double> peaks = new ArrayList<>();
        for (ExposureStar s : rel) peaks.add(s.getPeakAdu());
        double medianPeak = medianOf(peaks);
        int medianIdx = 0;
        double bestDiff = Double.POSITIVE_INFINITY;
        for (int i = 0; i < rel.size(); i++) {
            double d = Math.abs(rel.get(i).getPeakAdu() - medianPeak);
            if (d < bestDiff) { bestDiff = d; medianIdx = i; }
        }
        ExposureStar selected = rel.get(medianIdx);
        selected.setSelected(true);
        r.setSelectedStar(selected);

        // 物理模型（总通量）
        double S0Total = selected.getTotalFluxAdu() * gain;
        double B0Pixel = bg * gain;
        int area = selected.getArea();
        double D = dark;
        double R2 = rn * rn;
        double t0 = img.exptime;

        double noiseSqCurrent = S0Total + B0Pixel * area + D * area * t0 + R2 * area;
        double snrCurrent = noiseSqCurrent > 0 ? S0Total / Math.sqrt(noiseSqCurrent) : 0;

        double bgTotalPerT0 = (B0Pixel + D * t0) * area;
        double a = S0Total * S0Total;
        double b = targetSnr * targetSnr * (S0Total + bgTotalPerT0);
        double c = targetSnr * targetSnr * R2 * area;
        double disc = b * b + 4 * a * c;
        double rr = (disc < 0 || S0Total <= 0) ? 2.0 : (b + Math.sqrt(disc)) / (2 * a);
        double tRec = t0 * rr;

        double noiseTotal = S0Total + B0Pixel * area + D * area * t0 + R2 * area;
        Map<String, Double> nb = new LinkedHashMap<>();
        nb.put("photonNoisePct", noiseTotal > 0 ? S0Total / noiseTotal * 100 : 0);
        nb.put("skyNoisePct", noiseTotal > 0 ? B0Pixel * area / noiseTotal * 100 : 0);
        nb.put("darkNoisePct", noiseTotal > 0 ? D * area * t0 / noiseTotal * 100 : 0);
        nb.put("readNoisePct", noiseTotal > 0 ? R2 * area / noiseTotal * 100 : 0);
        r.setNoiseInfo(nb);

        r.setSnrCurrent(snrCurrent);
        r.setExptimeRecommended(tRec);
        r.setValid(true);
        r.setMessage("计算成功");

        if (tRec > maxExposure) {
            double rMax = maxExposure / t0;
            double noiseSqMax = (S0Total + bgTotalPerT0) * rMax + R2 * area;
            r.setSuggestedSnr(noiseSqMax > 0 ? S0Total * rMax / Math.sqrt(noiseSqMax) : 0);
            r.setMaxExposureUsed(maxExposure);
        }

        double minPeak = Double.POSITIVE_INFINITY, maxPeak = Double.NEGATIVE_INFINITY;
        for (double p : peaks) { minPeak = Math.min(minPeak, p); maxPeak = Math.max(maxPeak, p); }
        diag.put("peakRangeMin", minPeak);
        diag.put("peakRangeMax", maxPeak);
        diag.put("medianPeakAdu", medianPeak);
        diag.put("selectedArea", (double) area);
        diag.put("selectedTotalFluxAdu", selected.getTotalFluxAdu());
        diag.put("selectedPeakAdu", selected.getPeakAdu());

        r.setConditionAssessment(assessConditions(rel.size(), bg, bgStd, img.exptime,
                snrCurrent, nb.get("skyNoisePct"), gain, rn));
        return r;
    }

    // =====================================================================
    // 观测条件评估
    // =====================================================================
    private ExposureConditionAssessment assessConditions(int reliableCount, double bgAdu, double bgStdAdu,
                                                         double exptime, double snrCurrent, double skyNoisePct,
                                                         double gain, double readNoise) {
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        int score = 100;

        if (reliableCount == 0) {
            warnings.add("未检测到任何达到SNR≥10的可靠恒星");
            score -= 40;
        } else if (reliableCount <= 3) {
            warnings.add("仅检测到 " + reliableCount + " 颗可靠恒星，选星统计意义不足");
            score -= 20;
            suggestions.add("尝试降低检测阈值或增加曝光时间以获取更多可靠源");
        } else if (reliableCount <= 10) {
            warnings.add("检测到 " + reliableCount + " 颗可靠恒星，数量偏少");
            score -= 8;
        }

        double skyRateAdu = exptime > 0 ? bgAdu / exptime : 0;
        String levelDesc;
        if (skyRateAdu > 80) {
            levelDesc = "极高（重度光污染/月光干扰）";
            warnings.add(String.format(Locale.US, "天光背景速率: %.0f ADU/s — %s", skyRateAdu, levelDesc));
            score -= 20;
            suggestions.add("建议选择无月光、远离城市灯光的暗夜观测");
            suggestions.add(String.format(Locale.US, "当前每秒积累的天光(≈%.0f e⁻/s/pixel)远超读出噪声(%.0f e⁻)", skyRateAdu * gain, readNoise));
        } else if (skyRateAdu > 30) {
            levelDesc = "较高（中度光污染）";
            warnings.add(String.format(Locale.US, "天光背景速率: %.0f ADU/s — %s", skyRateAdu, levelDesc));
            score -= 10;
            suggestions.add("光污染较重，SNR提升效率较低，可考虑堆叠多张短曝光");
        } else if (skyRateAdu > 10) {
            levelDesc = "中等（轻度光污染）";
        } else {
            levelDesc = "较低（观测条件良好）";
        }

        if (skyNoisePct > 80) {
            warnings.add(String.format(Locale.US, "天光噪声占总噪声 %.0f%%，SNR随曝光时间仅按√t增长", skyNoisePct));
            score -= 10;
            suggestions.add("天光主导区间: 曝光时间翻倍仅提升SNR约41%，而非翻倍");
            suggestions.add("可考虑堆叠 N 张短曝光，等效SNR ≈ √N × 单张SNR");
        } else if (skyNoisePct > 60) {
            warnings.add(String.format(Locale.US, "天光噪声占比 %.0f%%，SNR增长受天光限制", skyNoisePct));
            score -= 5;
        }

        if (snrCurrent < 10) {
            warnings.add(String.format(Locale.US, "选中恒星当前SNR仅 %.1f，远低于精确测光要求(SNR≥30)", snrCurrent));
            score -= 15;
            suggestions.add("当前曝光不足以进行精确测光，需大幅增加曝光或堆叠");
        } else if (snrCurrent < 30) {
            warnings.add(String.format(Locale.US, "选中恒星当前SNR为 %.1f，达到一般测光水平但精度有限", snrCurrent));
            score -= 5;
            suggestions.add("如需高精度测光(SNR≥50)，建议增加曝光或堆叠图像");
        }

        double bgElectrons = bgAdu * gain;
        if (bgElectrons > 0) {
            double rnStd = readNoise / gain;
            if (bgStdAdu < rnStd * 0.8) {
                warnings.add(String.format(Locale.US,
                        "背景σ(%.1f ADU)低于读出噪声(%.1f ADU)，sigma-clipping可能过度裁剪导致σ低估", bgStdAdu, rnStd));
                score -= 3;
            }
        }

        if (reliableCount > 0 && reliableCount <= 3) {
            warnings.add("仅 " + reliableCount + " 颗可靠恒星，\"中位数选星\"无法发挥稳健性优势，不同图像可能选中不同恒星导致结果不一致");
            suggestions.add("建议在星场密集的区域拍摄，或使用更长的曝光时间");
        }

        String level;
        if (score >= 80) level = "优";
        else if (score >= 60) level = "良";
        else if (score >= 35) level = "一般";
        else level = "差";
        score = Math.max(0, Math.min(100, score));

        ExposureConditionAssessment ca = new ExposureConditionAssessment();
        ca.setScore(score);
        ca.setLevel(level);
        ca.setSkyRateAdu(skyRateAdu);
        ca.setSkyLevelDesc(levelDesc);
        ca.setWarnings(warnings);
        ca.setSuggestions(suggestions);
        return ca;
    }

    // =====================================================================
    // 预览 PNG（min-max 拉伸为灰度）
    // =====================================================================
    private String renderPng(FitsImage img) throws Exception {
        BufferedImage bimg = new BufferedImage(img.w, img.h, BufferedImage.TYPE_BYTE_GRAY);
        double range = (img.max - img.min) == 0 ? 1.0 : (img.max - img.min);
        for (int i = 0; i < img.data.length; i++) {
            double v = (img.data[i] - img.min) / range * 255.0;
            if (v < 0) v = 0;
            if (v > 255) v = 255;
            int g = (int) v;
            int x = i % img.w, y = i / img.w;
            bimg.setRGB(x, y, (g << 16) | (g << 8) | g);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bimg, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    // 标记选中星（恒星列表里设置 selected 已在 analyzeStars 完成，这里兜底保持一致）
    private void markStars(ExposureCalcResult r) {
        if (r.getStars() == null || r.getSelectedStar() == null) return;
        ExposureStar sel = r.getSelectedStar();
        for (ExposureStar s : r.getStars()) {
            s.setSelected(Math.abs(s.getCx() - sel.getCx()) < 1e-6 && Math.abs(s.getCy() - sel.getCy()) < 1e-6);
        }
    }

    // =====================================================================
    // 文本报告
    // =====================================================================
    private String f(double x, int d) {
        return String.format(Locale.US, "%." + d + "f", x);
    }

    private double dg(Map<String, Double> m, String k) {
        Double v = m == null ? null : m.get(k);
        return v == null ? 0 : v;
    }

    private String buildPlanetReport(ExposureCalcResult r) {
        List<String> L = new ArrayList<>();
        L.add(LINE); L.add("  行星曝光时间分析报告"); L.add(LINE); L.add("");
        if (!r.isValid()) { L.add("❌ " + r.getMessage()); return String.join("\n", L); }

        Map<String, Double> d = r.getDiagnostics();
        L.add("【图像信息】");
        L.add("  当前曝光时间:  " + f(r.getExptimeCurrent(), 1) + " s"); L.add("");
        L.add("【背景估计】(Sigma-clipping迭代)");
        L.add("  背景均值:      " + f(dg(d, "backgroundAdu"), 1) + " ADU");
        L.add("  背景噪声(σ):   " + f(dg(d, "backgroundStdAdu"), 1) + " ADU"); L.add("");

        ExposurePlanet p = r.getPlanet();
        L.add("【行星检测】");
        L.add("  行星中心:      (" + f(p.getCx(), 1) + ", " + f(p.getCy(), 1) + ")");
        L.add("  等效半径:      " + f(p.getRadius(), 1) + " 像素");
        L.add("  行星像素数:    " + p.getPixelCount() + " px");
        L.add("  行星平均亮度:  " + f(dg(d, "planetMeanAdu"), 1) + " ADU");
        L.add("  行星净信号:    " + f(dg(d, "planetSignalAdu"), 1) + " ADU");
        L.add("  行星峰值亮度:  " + f(dg(d, "peakAdu"), 1) + " ADU");
        L.add("  峰值占比:      " + f(dg(d, "peakOverflowPct"), 1) + "% 满量程"); L.add("");

        L.add("【信噪比分析】");
        L.add("  当前每像素SNR: " + f(r.getSnrCurrent(), 2));
        L.add("  目标每像素SNR: " + f(r.getTargetSnr(), 1)); L.add("");

        appendNoise(L, r.getNoiseInfo(), false);
        if (r.getWarning() != null) { L.add("【⚠️ 警告】"); L.add("  " + r.getWarning()); L.add(""); }

        L.add(LINE); L.add("  🎯 推荐曝光时间: " + f(r.getExptimeRecommended(), 1) + " s"); L.add(LINE); L.add("");
        appendAdvice(L, r);
        return String.join("\n", L);
    }

    private String buildStarReport(ExposureCalcResult r) {
        List<String> L = new ArrayList<>();
        L.add(LINE); L.add("  恒星曝光时间分析报告"); L.add(LINE); L.add("");
        Map<String, Double> d = r.getDiagnostics();

        if (d != null && d.containsKey("backgroundAdu")) {
            L.add("【背景估计】(Sigma-clipping迭代)");
            L.add("  背景均值:      " + f(dg(d, "backgroundAdu"), 1) + " ADU");
            L.add("  背景噪声(σ):   " + f(dg(d, "backgroundStdAdu"), 1) + " ADU"); L.add("");
        }

        L.add("【恒星检测】");
        L.add("  检测阈值:      3σ (" + f(dg(d, "backgroundAdu"), 1) + " + 3×" + f(dg(d, "backgroundStdAdu"), 1) + " ADU)");
        L.add("  总检测源数:    " + r.getStarsDetected());
        L.add("  可靠源数(SNR≥10): " + r.getStarsReliable()); L.add("");

        if (!r.isValid()) { L.add("❌ " + r.getMessage()); return String.join("\n", L); }

        ExposureStar sel = r.getSelectedStar();
        L.add("【选中的典型恒星】(中位数亮度)");
        L.add("  重心坐标:      (" + f(sel.getCx(), 3) + ", " + f(sel.getCy(), 3) + ") (子像素精度)");
        L.add("  覆盖面积:      " + sel.getArea() + " px");
        L.add("  峰值信号:      " + f(sel.getPeakAdu(), 1) + " ADU");
        if (sel.isSaturated()) L.add("  ⚠️ 该星已过曝！(峰值 ≥ 65535×0.95)");
        L.add("  总通量:        " + f(sel.getTotalFluxAdu(), 1) + " ADU");
        L.add("  平均通量:      " + f(sel.getMeanFluxAdu(), 1) + " ADU/px");
        L.add("  精确SNR估计:  " + f(sel.getSnrEstimate(), 2) + " (改进算法)"); L.add("");

        int saturatedCount = r.getStarsSaturated();
        if (saturatedCount > 0) {
            L.add("【过曝星象统计】");
            L.add("  过曝星数: " + saturatedCount + " 颗");
            L.add("  可靠星中过曝: " + (r.isAllReliableSaturated() ? "True" : "False"));
            if (r.isAllReliableSaturated()) L.add("  ⚠️ 所有可靠星均已过曝！请缩短曝光时间");
            L.add("");
        }
        L.add("  可靠源亮度范围: " + f(dg(d, "peakRangeMin"), 1) + " ~ " + f(dg(d, "peakRangeMax"), 1) + " ADU (峰值)");
        L.add("  中位数峰值:     " + f(dg(d, "medianPeakAdu"), 1) + " ADU"); L.add("");

        L.add("【信噪比分析】(总通量)");
        L.add("  当前总通量SNR: " + f(r.getSnrCurrent(), 2));
        L.add("  目标总通量SNR: " + f(r.getTargetSnr(), 1)); L.add("");

        appendNoise(L, r.getNoiseInfo(), true);

        ExposureConditionAssessment ca = r.getConditionAssessment();
        if (ca != null) {
            String emoji = Map.of("优", "🟢", "良", "🟡", "一般", "🟠", "差", "🔴").getOrDefault(ca.getLevel(), "⚪");
            L.add("【观测条件评估】");
            L.add("  综合评分:      " + emoji + " " + ca.getScore() + "分 / 100  (" + ca.getLevel() + ")");
            L.add("  天光速率:      " + f(ca.getSkyRateAdu(), 1) + " ADU/s  (" + ca.getSkyLevelDesc() + ")"); L.add("");
            if (ca.getWarnings() != null && !ca.getWarnings().isEmpty()) {
                L.add("  ⚠ 发现的问题:");
                for (String w : ca.getWarnings()) L.add("    • " + w);
                L.add("");
            }
            if (ca.getSuggestions() != null && !ca.getSuggestions().isEmpty()) {
                L.add("  💡 改善建议:");
                for (String s : ca.getSuggestions()) L.add("    → " + s);
                L.add("");
            }
        }

        L.add(LINE); L.add("  🎯 推荐曝光时间: " + f(r.getExptimeRecommended(), 1) + " s"); L.add(LINE); L.add("");
        appendAdvice(L, r);
        return String.join("\n", L);
    }

    private void appendNoise(List<String> L, Map<String, Double> nb, boolean star) {
        if (nb == null) return;
        L.add("【噪声分解】(当前曝光)");
        L.add("  光子噪声占比:  " + f(dg(nb, "photonNoisePct"), 1) + "%");
        L.add("  天光噪声占比:  " + f(dg(nb, "skyNoisePct"), 1) + "%");
        L.add("  暗电流占比:    " + f(dg(nb, "darkNoisePct"), 1) + "%");
        L.add("  读出噪声占比:  " + f(dg(nb, "readNoisePct"), 1) + "%"); L.add("");
        String dom = dominantNoise(nb);
        if ("readNoisePct".equals(dom)) L.add(star ? "  ⚠️ 读出噪声主导！建议增加曝光时间" : "  ⚠️ 读出噪声主导！建议适当增加曝光时间");
        else if ("photonNoisePct".equals(dom)) L.add("  ✅ 已进入光子噪声主导区间，曝光效率良好");
        L.add("");
    }

    private String dominantNoise(Map<String, Double> nb) {
        String[] keys = {"photonNoisePct", "skyNoisePct", "darkNoisePct", "readNoisePct"};
        String best = keys[0];
        double bestV = dg(nb, best);
        for (String k : keys) { double v = dg(nb, k); if (v > bestV) { bestV = v; best = k; } }
        return best;
    }

    private void appendAdvice(List<String> L, ExposureCalcResult r) {
        double tRec = r.getExptimeRecommended();
        double tCur = r.getExptimeCurrent();
        if (tRec > tCur * 2) {
            L.add("  💡 建议显著增加曝光时间以提高SNR");
            L.add("     (当前 " + f(tCur, 0) + "s → 推荐 " + f(tRec, 1) + "s)");
        } else if (tRec > tCur * 1.2) {
            L.add("  💡 可适当增加曝光时间");
        } else if (tRec < tCur * 0.8) {
            L.add("  💡 当前曝光可能过曝，建议缩短");
            L.add("     (当前 " + f(tCur, 0) + "s → 推荐 " + f(tRec, 1) + "s)");
        } else {
            L.add("  ✅ 当前曝光时间接近最优值");
        }

        if (r.getSuggestedSnr() != null) {
            double snrSug = r.getSuggestedSnr();
            double maxExp = r.getMaxExposureUsed() == null ? 0 : r.getMaxExposureUsed();
            L.add("");
            L.add("【目标SNR合理性评估】");
            L.add("  当前目标SNR:     " + f(r.getTargetSnr(), 1));
            L.add("  推荐曝光时间:    " + f(tRec, 1) + "s  (超过最大可接受值 " + f(maxExp, 0) + "s)"); L.add("");
            L.add("  💡 若限制最大曝光时间为 " + f(maxExp, 0) + "s:");
            L.add("     → 可达到的SNR ≈ " + f(snrSug, 1));
            int snrRound = (int) (snrSug / 5) * 5;
            if (snrRound < 5) snrRound = 5;
            L.add("     → 建议将目标SNR设为 " + snrRound + " 以获得合理曝光时间");
            L.add("     → 或通过堆叠 " + (int) Math.ceil(Math.pow(r.getTargetSnr() / snrSug, 2)) + " 张 " + f(maxExp, 0) + "s 图像达到目标");
        }
    }

    private String buildPlanetPreview(ExposureCalcResult r) {
        if (!r.isValid() || r.getPlanet() == null) return r.getMessage() == null ? "" : r.getMessage();
        ExposurePlanet p = r.getPlanet();
        return "检测到行星目标，位于图像 (" + f(p.getCx(), 0) + ", " + f(p.getCy(), 0) + ")，覆盖 "
                + p.getPixelCount() + " 个像素（等效半径 " + f(p.getRadius(), 0) + "px）\n"
                + "每像素SNR: " + f(r.getSnrCurrent(), 2) + " → 目标SNR: " + f(r.getTargetSnr(), 1);
    }

    private String buildStarPreview(ExposureCalcResult r) {
        if (!r.isValid() || r.getSelectedStar() == null) return r.getMessage() == null ? "" : r.getMessage();
        ExposureStar sel = r.getSelectedStar();
        return "检测到 " + r.getStarsDetected() + " 个源，其中 " + r.getStarsReliable() + " 个达到SNR≥10\n"
                + "选中典型恒星: 峰值" + f(sel.getPeakAdu(), 0) + " ADU, 面积" + sel.getArea() + " px, 中心("
                + f(sel.getCx(), 0) + ", " + f(sel.getCy(), 0) + ")\n"
                + "总通量SNR: " + f(r.getSnrCurrent(), 1) + " → 目标SNR: " + f(r.getTargetSnr(), 0);
    }

    // =====================================================================
    // AI 分析 prompt
    // =====================================================================
    private static final String AI_SYSTEM_PROMPT = "你是一位专业的天文观测顾问，精通CCD/CMOS天文摄影、测光和图像处理。\n\n" +
            "你的任务是根据曝光时间计算器给出的分析结果，提供专业、实用的观测建议。\n\n" +
            "请遵循以下原则：\n" +
            "1. 用简洁清晰的中文回答\n" +
            "2. 解释关键数据背后的物理含义（噪声分解、SNR 的意义、光子/天光噪声的区别）\n" +
            "3. 根据噪声分解判断观测条件质量（天光噪声主导=光污染严重，光子噪声主导=条件良好）\n" +
            "4. 给出具体可执行的拍摄建议（曝光时间、堆叠策略、滤镜选择、观测时段等）\n" +
            "5. 如果数据异常（检测源过少、背景噪声异常低、推荐曝光过长等），明确指出问题\n" +
            "6. 考虑实际观测限制：跟踪精度、大气视宁度、光污染、月光干扰、CCD冷却温度等\n" +
            "7. 回答控制在400字以内，重点突出，用项目符号分条列出建议";

    private String buildAiPrompt(ExposureCalcResult r) {
        boolean planet = !"star".equalsIgnoreCase(r.getMode());
        String modeText = planet ? "行星模式（面源/扩展天体，如土星、木星）" : "恒星模式（点源/测光）";
        CameraParams cam = r.getCameraParams() == null ? new CameraParams(2.0, 6.33, 0.002) : r.getCameraParams();

        List<String> L = new ArrayList<>();
        L.add("## 观测模式: " + modeText);
        L.add("## 目标 SNR: " + f(r.getTargetSnr(), 1));
        L.add("## 相机参数: 增益=" + cam.getGain() + " e-/ADU, 读出噪声=" + cam.getReadNoise() + " e-, 暗电流=" + cam.getDarkCurrent() + " e-/s");
        L.add("");
        L.add("## 核心结果:");
        L.add("- 当前曝光时间: " + f(r.getExptimeCurrent(), 1) + " s");
        L.add("- 推荐曝光时间: " + f(r.getExptimeRecommended(), 1) + " s");
        L.add("- 当前 SNR: " + f(r.getSnrCurrent(), 2));
        L.add("- 计算状态: " + (r.isValid() ? "成功" : (r.getMessage() == null ? "失败" : r.getMessage())));

        Map<String, Double> d = r.getDiagnostics();
        if (d != null && !d.isEmpty()) {
            L.add(""); L.add("## 诊断数据:");
            L.add("- 背景均值: " + f(dg(d, "backgroundAdu"), 1) + " ADU");
            L.add("- 背景噪声 σ: " + f(dg(d, "backgroundStdAdu"), 1) + " ADU");
            if (planet) {
                L.add("- 行星平均亮度: " + f(dg(d, "planetMeanAdu"), 1) + " ADU");
                L.add("- 行星净信号: " + f(dg(d, "planetSignalAdu"), 1) + " ADU");
                ExposurePlanet p = r.getPlanet();
                if (p != null) {
                    L.add("- 行星中心: (" + f(p.getCx(), 1) + ", " + f(p.getCy(), 1) + ")");
                    L.add("- 行星像素数: " + p.getPixelCount() + " px");
                }
            } else {
                L.add("- 检测到的源数: " + r.getStarsDetected());
                L.add("- 可靠源数 (SNR≥10): " + r.getStarsReliable());
                ExposureStar sel = r.getSelectedStar();
                if (sel != null) {
                    L.add("- 选中恒星峰值: " + f(sel.getPeakAdu(), 1) + " ADU");
                    L.add("- 选中恒星总通量: " + f(sel.getTotalFluxAdu(), 1) + " ADU");
                    L.add("- 选中恒星面积: " + sel.getArea() + " px");
                }
            }
        }

        Map<String, Double> nb = r.getNoiseInfo();
        if (nb != null) {
            L.add(""); L.add("## 噪声分解:");
            L.add("- 光子噪声: " + f(dg(nb, "photonNoisePct"), 1) + "%");
            L.add("- 天光噪声: " + f(dg(nb, "skyNoisePct"), 1) + "%");
            L.add("- 暗电流: " + f(dg(nb, "darkNoisePct"), 1) + "%");
            L.add("- 读出噪声: " + f(dg(nb, "readNoisePct"), 1) + "%");
        }

        if (r.getSuggestedSnr() != null) {
            L.add("");
            L.add("## 限制条件: 最大可接受曝光 " + f(r.getMaxExposureUsed() == null ? 0 : r.getMaxExposureUsed(), 0)
                    + "s 下可达 SNR ≈ " + f(r.getSuggestedSnr(), 1));
        }

        L.add(""); L.add("请根据以上数据，给出专业的观测建议。");
        return String.join("\n", L);
    }
}
