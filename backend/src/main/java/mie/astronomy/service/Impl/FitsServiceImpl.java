package mie.astronomy.service.Impl;

import mie.astronomy.service.FileService;
import mie.astronomy.service.FitsService;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Service
public class FitsServiceImpl implements FitsService {

    private static final Logger log = LoggerFactory.getLogger(FitsServiceImpl.class);
    private static final int MAX_PREVIEW_SIZE = 1600;
    private static final int MAX_PERCENTILE_SAMPLES = 100_000;
    private static final double ASINH_STRENGTH = 8.0;

    @Autowired
    private FileService fileService;

    @Override
    public byte[] fitsToPng(String filePath) throws IOException {
        Path absolutePath = fileService.resolvePath(filePath);
        if (!Files.isRegularFile(absolutePath) || !Files.isReadable(absolutePath)) {
            throw new IOException("FITS 文件不存在或无法读取: " + filePath);
        }

        Fits fits = null;
        try {
            fits = new Fits(absolutePath.toFile());
            ImageHDU hdu = findFirstImageHdu(fits);

            int[] axes = hdu.getAxes();
            if (axes == null || axes.length < 2) {
                throw new IOException("FITS 文件不包含二维图像数据");
            }
            int sourceHeight = axes[axes.length - 2];
            int sourceWidth = axes[axes.length - 1];
            int[] dimensions = calculatePreviewDimensions(sourceWidth, sourceHeight, MAX_PREVIEW_SIZE);
            int previewWidth = dimensions[0];
            int previewHeight = dimensions[1];

            Header header = hdu.getHeader();
            double bzero = header.getDoubleValue("BZERO", 0.0);
            double bscale = header.getDoubleValue("BSCALE", 1.0);
            Object imagePlane = selectFirstImagePlane(hdu.getKernel(), axes.length);
            double[] previewData = downsample(
                imagePlane, sourceWidth, sourceHeight, previewWidth, previewHeight, bzero, bscale);
            double[] cuts = calculatePercentileCuts(previewData);
            byte[] png = renderPng(previewData, previewWidth, previewHeight, cuts[0], cuts[1]);
            log.info("Generated FITS preview {}x{} from {}x{}: {}",
                previewWidth, previewHeight, sourceWidth, sourceHeight, absolutePath.getFileName());
            return png;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("FITS 预览生成失败: " + e.getMessage(), e);
        } finally {
            if (fits != null) {
                try {
                    fits.close();
                } catch (IOException closeError) {
                    log.debug("Failed to close FITS file {}", absolutePath, closeError);
                }
            }
        }
    }

    private ImageHDU findFirstImageHdu(Fits fits) throws Exception {
        BasicHDU<?> basicHdu;
        while ((basicHdu = fits.readHDU()) != null) {
            if (basicHdu instanceof ImageHDU imageHdu
                    && imageHdu.getKernel() != null
                    && imageHdu.getAxes() != null
                    && imageHdu.getAxes().length >= 2) {
                return imageHdu;
            }
        }
        throw new IOException("FITS 文件中没有找到有效图像数据");
    }

    private Object selectFirstImagePlane(Object kernel, int dimensions) throws IOException {
        Object plane = kernel;
        for (int i = 0; i < dimensions - 2; i++) {
            if (plane == null || !plane.getClass().isArray() || Array.getLength(plane) == 0) {
                throw new IOException("FITS 数据立方体不包含可预览图层");
            }
            plane = Array.get(plane, 0);
        }
        return plane;
    }

    private double[] downsample(
            Object plane,
            int sourceWidth,
            int sourceHeight,
            int previewWidth,
            int previewHeight,
            double bzero,
            double bscale) throws IOException {
        if (plane == null || !plane.getClass().isArray() || Array.getLength(plane) < sourceHeight) {
            throw new IOException("FITS 图像数据与头部尺寸不一致");
        }

        double[] pixels = new double[Math.multiplyExact(previewWidth, previewHeight)];
        int targetIndex = 0;
        for (int y = 0; y < previewHeight; y++) {
            int sourceY = Math.min(sourceHeight - 1,
                (int) (((long) y * sourceHeight + sourceHeight / 2L) / previewHeight));
            Object row = Array.get(plane, sourceY);
            if (row == null || !row.getClass().isArray() || Array.getLength(row) < sourceWidth) {
                throw new IOException("FITS 图像行数据与头部尺寸不一致");
            }
            for (int x = 0; x < previewWidth; x++) {
                int sourceX = Math.min(sourceWidth - 1,
                    (int) (((long) x * sourceWidth + sourceWidth / 2L) / previewWidth));
                pixels[targetIndex++] = bzero + bscale * readPixel(row, sourceX);
            }
        }
        return pixels;
    }

    private double readPixel(Object row, int x) throws IOException {
        if (row instanceof byte[] values) return values[x] & 0xFF;
        if (row instanceof short[] values) return values[x];
        if (row instanceof int[] values) return values[x];
        if (row instanceof long[] values) return values[x];
        if (row instanceof float[] values) return values[x];
        if (row instanceof double[] values) return values[x];
        throw new IOException("不支持的 FITS 像素类型: " + row.getClass().getTypeName());
    }

    static int[] calculatePreviewDimensions(int width, int height, int maxSize) {
        if (width <= 0 || height <= 0 || maxSize <= 0) {
            throw new IllegalArgumentException("图像尺寸必须大于 0");
        }
        int longestSide = Math.max(width, height);
        if (longestSide <= maxSize) {
            return new int[]{width, height};
        }
        double scale = (double) maxSize / longestSide;
        return new int[]{
            Math.max(1, (int) Math.round(width * scale)),
            Math.max(1, (int) Math.round(height * scale))
        };
    }

    static double[] calculatePercentileCuts(double[] pixels) throws IOException {
        int stride = Math.max(1, (int) Math.ceil((double) pixels.length / MAX_PERCENTILE_SAMPLES));
        double[] samples = new double[Math.min(pixels.length, MAX_PERCENTILE_SAMPLES)];
        int sampleCount = 0;
        for (int i = 0; i < pixels.length && sampleCount < samples.length; i += stride) {
            if (Double.isFinite(pixels[i])) {
                samples[sampleCount++] = pixels[i];
            }
        }
        if (sampleCount == 0) {
            throw new IOException("FITS 图像不包含有效像素");
        }

        Arrays.sort(samples, 0, sampleCount);
        int lowerIndex = (int) Math.floor((sampleCount - 1) * 0.01);
        int upperIndex = (int) Math.ceil((sampleCount - 1) * 0.99);
        double lower = samples[lowerIndex];
        double upper = samples[upperIndex];
        if (!(upper > lower)) {
            upper = lower + 1.0;
        }
        return new double[]{lower, upper};
    }

    private byte[] renderPng(
            double[] pixels, int width, int height, double lowerCut, double upperCut) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] raster = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        double range = upperCut - lowerCut;
        double stretchDenominator = asinh(ASINH_STRENGTH);

        for (int i = 0; i < pixels.length; i++) {
            double normalized = (pixels[i] - lowerCut) / range;
            if (!Double.isFinite(normalized) || normalized <= 0) {
                raster[i] = 0;
            } else if (normalized >= 1) {
                raster[i] = (byte) 255;
            } else {
                double stretched = asinh(normalized * ASINH_STRENGTH) / stretchDenominator;
                raster[i] = (byte) Math.round(stretched * 255.0);
            }
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IOException("当前运行环境不支持 PNG 编码");
            }
            return output.toByteArray();
        }
    }

    private static double asinh(double value) {
        return Math.log(value + Math.sqrt(value * value + 1.0));
    }

}
