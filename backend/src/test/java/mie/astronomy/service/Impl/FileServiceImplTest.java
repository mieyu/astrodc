package mie.astronomy.service.Impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesRelativeAndLegacyAbsolutePathsInsideConfiguredRoot() {
        Path storageRoot = tempDir.resolve("Observated_images");
        FileServiceImpl service = new FileServiceImpl(storageRoot.toString());
        Path expected = storageRoot.resolve("NAO/session/image.fits").toAbsolutePath().normalize();

        assertEquals(expected, service.resolvePath("NAO/session/image.fits"));
        assertEquals(expected, service.resolvePath(
            "/media/miemieyu/work/Observated_images/NAO/session/image.fits"));
        assertEquals(expected, service.resolvePath(
            "D:\\archive\\Observated_images\\NAO\\session\\image.fits"));
    }

    @Test
    void rejectsTraversalAndUnrelatedAbsolutePaths() {
        Path storageRoot = tempDir.resolve("Observated_images");
        FileServiceImpl service = new FileServiceImpl(storageRoot.toString());

        assertThrows(RuntimeException.class, () -> service.resolvePath("../outside.fits"));
        assertThrows(RuntimeException.class, () -> service.resolvePath("D:\\outside\\image.fits"));
    }
}
