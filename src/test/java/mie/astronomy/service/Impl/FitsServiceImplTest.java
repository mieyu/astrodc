package mie.astronomy.service.Impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FitsServiceImplTest {

    @Test
    void limitsLongestSideAndPreservesAspectRatio() {
        assertArrayEquals(new int[]{1600, 800},
            FitsServiceImpl.calculatePreviewDimensions(4000, 2000, 1600));
        assertArrayEquals(new int[]{600, 1200},
            FitsServiceImpl.calculatePreviewDimensions(1000, 2000, 1200));
        assertArrayEquals(new int[]{800, 600},
            FitsServiceImpl.calculatePreviewDimensions(800, 600, 1600));
    }

    @Test
    void percentileCutsIgnoreNonFinitePixelsAndOutliers() throws IOException {
        double[] pixels = new double[1002];
        for (int i = 0; i < 1000; i++) pixels[i] = i;
        pixels[1000] = Double.NaN;
        pixels[1001] = Double.POSITIVE_INFINITY;

        double[] cuts = FitsServiceImpl.calculatePercentileCuts(pixels);
        assertEquals(9.0, cuts[0]);
        assertEquals(990.0, cuts[1]);
    }

    @Test
    void percentileCutsRejectImagesWithoutValidPixels() {
        assertThrows(IOException.class,
            () -> FitsServiceImpl.calculatePercentileCuts(
                new double[]{Double.NaN, Double.POSITIVE_INFINITY}));
    }
}
