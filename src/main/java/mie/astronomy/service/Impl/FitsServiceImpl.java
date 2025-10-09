package mie.astronomy.service.Impl;

import mie.astronomy.service.FileService;
import mie.astronomy.service.FitsService;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

@Service
public class FitsServiceImpl implements FitsService {

    @Autowired
    private FileService fileService;

    @Override
    public byte[] fitsToPng(String filePath) throws IOException {
        Path absolutePath = fileService.resolvePath(filePath);
        Fits f = null;
        try {
            f = new Fits(absolutePath.toFile());

            ImageHDU hdu = null;
            BasicHDU<?> basicHdu;
            int hduIndex = 0;
            while ((basicHdu = f.readHDU()) != null) {
                if (basicHdu instanceof ImageHDU) {
                    if (basicHdu.getKernel() != null && ((ImageHDU) basicHdu).getAxes().length > 0) {
                        hdu = (ImageHDU) basicHdu;
                        System.out.println("Success: Found valid image data in HDU at index " + hduIndex);
                        break; // Found it, exit the loop
                    }
                }
                hduIndex++;
            }

            if (hdu == null) {
                throw new IOException("FITS file does not contain a valid ImageHDU with data.");
            }

            int[] axes = hdu.getAxes();
            int height = axes[0];
            int width = axes.length > 1 ? axes[1] : 1;
            if (axes.length > 2) {
                width = axes[1];
                height = axes[2];
            }

            BufferedImage bimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            Header header = hdu.getHeader();
            int bitpix = header.getIntValue("BITPIX");
            Object dataKernel = hdu.getKernel();

            int totalPixels = 1;
            for(int axisSize : axes) totalPixels *= axisSize;
            double[] flatData = flattenData(dataKernel, bitpix, totalPixels);

            int frameSize = width * height;
            double[] previewData = new double[frameSize];
            System.arraycopy(flatData, 0, previewData, 0, frameSize);


            double[] sortedData = Arrays.copyOf(previewData, previewData.length);
            Arrays.sort(sortedData);

            int lowerCutIndex = (int) (sortedData.length * 0.01f);
            int upperCutIndex = (int) (sortedData.length * 0.99f) - 1;
            if (upperCutIndex < 0) upperCutIndex = 0;
            if (lowerCutIndex >= sortedData.length) lowerCutIndex = sortedData.length - 1;

            double min = sortedData[lowerCutIndex];
            double max = sortedData[upperCutIndex];
            double range = (max - min) == 0 ? 1.0 : (max - min);

            for (int i = 0; i < previewData.length; i++) {
                double value = previewData[i];
                if (value < min) value = min;
                if (value > max) value = max;
                int gray = (int) (255.0 * (value - min) / range);
                int rgb = (gray << 16) | (gray << 8) | gray;
                int x = i % width;
                int y = i / width;
                bimg.setRGB(x, y, rgb);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bimg, "png", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();
            return imageInByte;

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to process FITS file: " + e.getClass().getName() + " - " + e.getMessage(), e);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    // flattenData function remains unchanged
    private double[] flattenData(Object kernelData, int bitpix, int totalPixels) {
        double[] flat = new double[totalPixels];
        int index = 0;

        switch (bitpix) {
            case 16: // short
                if (kernelData instanceof short[][][]) {
                    for (short[][] plane : (short[][][]) kernelData) {
                        for (short[] row : plane) {
                            for (short val : row) flat[index++] = val;
                        }
                    }
                } else if (kernelData instanceof short[][]) {
                    for (short[] row : (short[][]) kernelData) {
                        for (short val : row) flat[index++] = val;
                    }
                } else {
                    for (short val : (short[]) kernelData) flat[index++] = val;
                }
                break;
            case 32: // int
                if (kernelData instanceof int[][][]) {
                    for (int[][] plane : (int[][][]) kernelData) {
                        for (int[] row : plane) {
                            for (int val : row) flat[index++] = val;
                        }
                    }
                } else if (kernelData instanceof int[][]) {
                    for (int[] row : (int[][]) kernelData) {
                        for (int val : row) flat[index++] = val;
                    }
                } else {
                    for (int val : (int[]) kernelData) flat[index++] = val;
                }
                break;
            case -32: // float
                if (kernelData instanceof float[][][]) {
                    for (float[][] plane : (float[][][]) kernelData) {
                        for (float[] row : plane) {
                            for (float val : row) flat[index++] = val;
                        }
                    }
                } else if (kernelData instanceof float[][]) {
                    for (float[] row : (float[][]) kernelData) {
                        for (float val : row) flat[index++] = val;
                    }
                } else {
                    for (float val : (float[]) kernelData) flat[index++] = val;
                }
                break;
            case -64: // double
                if (kernelData instanceof double[][][]) {
                    for (double[][] plane : (double[][][]) kernelData) {
                        for (double[] row : plane) {
                            for (double val : row) flat[index++] = val;
                        }
                    }
                } else if (kernelData instanceof double[][]) {
                    for (double[] row : (double[][]) kernelData) {
                        for (double val : row) flat[index++] = val;
                    }
                } else {
                    for (double val : (double[]) kernelData) flat[index++] = val;
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported BITPIX type: " + bitpix);
        }
        return flat;
    }
}
