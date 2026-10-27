package mie.astronomy.service;

import mie.astronomy.dto.ImageOwnQueryDto;
import mie.astronomy.entity.ImageOwn;
import mie.astronomy.service.Impl.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImageDownloadServiceTest {
    @TempDir Path temp;
    Path root;
    ImageOwnService images;
    ImageDownloadService downloads;
    ImageOwnQueryDto query;
    List<ImageOwn> matched;
    List<ImageOwn> daily;
    ImageOwn u0, later, otherTarget, otherScope, nextDay, bias, flat, dark;

    @BeforeEach void setup() throws Exception {
        root = temp.resolve("Observated_images");
        images = mock(ImageOwnService.class);
        downloads = new ImageDownloadService(images, new FileServiceImpl(root.toString()));
        query = new ImageOwnQueryDto();
        query.setObject("U0");
        u0 = image("望远镜/first/U0.fits", "U0", "T1", "2024-11-03T08:00:00", "LIGHT");
        later = image("望远镜/second/U0.fits", "U0", "T1", "2024-11-03T23:59:59", "LIGHT");
        otherTarget = image("other.fits", "S9", "T1", "2024-11-03T08:00:00", "LIGHT");
        otherScope = image("other-tele.fits", "U0", "T2", "2024-11-03T08:00:00", "LIGHT");
        nextDay = image("next.fits", "U0", "T1", "2024-11-04T00:00:00", "LIGHT");
        bias = image("bias.fit", "BIAS", "T1", "2024-11-03T01:00:00", "BIAS");
        flat = image("flat.fts", "SKY", "T1", "2024-11-03T02:00:00", "FLAT");
        dark = image("dark.fits", "DARK", "T1", "2024-11-03T03:00:00", "DARK");
        matched = new ArrayList<>(List.of(u0));
        daily = new ArrayList<>(List.of(u0, later, otherTarget, otherScope, nextDay, bias, flat, dark));
        when(images.listByQuery(any())).thenAnswer(invocation -> matched);
        when(images.listDownloadMetadata(any(), any())).thenAnswer(invocation -> daily);
    }

    @Test void groupsByDateTargetAndTelescopeAndReportsFullDayCounts() {
        var overview = downloads.overview(query);
        assertEquals(1, overview.groups().size());
        var group = overview.groups().get(0);
        assertEquals("2024-11-03", group.date());
        assertEquals("U0", group.object());
        assertEquals("T1", group.tele());
        assertEquals(1, group.matchedCount());
        assertEquals(2, group.fileCount());
        assertEquals(3, group.calibrationCount());
        verify(images).listDownloadMetadata(LocalDateTime.parse("2024-11-03T00:00:00"),
                LocalDateTime.parse("2024-11-04T00:00:00"));
    }

    @Test void fullDayArchiveContainsExactOriginalBytesAndPreservesDuplicateBasenames() throws Exception {
        var prepared = prepare(false, false);
        assertEquals(2, prepared.fileCount());
        Map<String, byte[]> archive = unzip(prepared);
        assertEquals(Set.of("望远镜/first/U0.fits", "望远镜/second/U0.fits"), archive.keySet());
        long bytes = 0;
        for (var entry : archive.entrySet()) {
            byte[] original = Files.readAllBytes(root.resolve(entry.getKey()));
            assertArrayEquals(original, entry.getValue());
            bytes += original.length;
        }
        assertEquals(bytes, prepared.totalBytes());
        assertEquals("astrodc_U0_2024-11-03.zip", prepared.filename());
    }

    @Test void strictQueryModeDoesNotExpandToOtherExposures() throws Exception {
        var prepared = prepare(false, true);
        assertEquals(Set.of("望远镜/first/U0.fits"), unzip(prepared).keySet());
    }

    @Test void addsOnlySameDayAndTelescopeCalibrationAndDeduplicatesAcrossTargets() throws Exception {
        matched.add(otherTarget);
        ImageOwn alias = new ImageOwn();
        alias.setPwd("/archive/Observated_images/bias.fit");
        alias.setObject("BIAS"); alias.setTele("T1");
        alias.setDateObs(bias.getDateObs()); alias.setImagetyp("BIAS");
        daily.add(alias);
        daily.add(image("wrong-day-bias.fits", "BIAS", "T1", "2024-11-04T00:00:00", "BIAS"));
        daily.add(image("wrong-tele-flat.fits", "FLAT", "T2", "2024-11-03T00:00:00", "FLAT"));
        var prepared = prepare(true, false);
        assertEquals(Set.of("望远镜/first/U0.fits", "望远镜/second/U0.fits", "other.fits",
                "bias.fit", "flat.fts", "dark.fits"), unzip(prepared).keySet());
    }

    @Test void reportsMissingNonFitsAndTraversalWithoutSilentlyIncludingThem() throws Exception {
        ImageOwn missing = image("missing.fits", "U0", "T1", "2024-11-03T08:00:00", "LIGHT");
        Files.delete(root.resolve("missing.fits"));
        daily.add(missing);
        daily.add(image("header.csv", "U0", "T1", "2024-11-03T08:00:00", "LIGHT"));
        ImageOwn traversal = new ImageOwn();
        traversal.setPwd("../outside.fits"); traversal.setObject("U0"); traversal.setTele("T1");
        traversal.setDateObs(u0.getDateObs());
        daily.add(traversal);
        var prepared = prepare(false, false);
        assertEquals(3, prepared.missingCount());
        assertEquals(2, prepared.fileCount());
        assertEquals(3, prepared.missingExamples().size());
        assertEquals(2, unzip(prepared).size());
    }

    @Test void rejectsEmptyUnknownAndChangedSelectionsBeforeSendingArchive() throws Exception {
        assertEquals(400, assertThrows(ResponseStatusException.class, () ->
                downloads.prepare(new ImageDownloadService.Request(query, List.of(), false, false, null)))
                .getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () ->
                downloads.prepare(new ImageDownloadService.Request(query, List.of("not-a-group"), false, false, null)))
                .getStatusCode().value());
        assertEquals(410, assertThrows(ResponseStatusException.class, () -> downloads.ready("unknown")).getStatusCode().value());
        var prepared = prepare(false, false);
        Files.write(root.resolve("望远镜/first/U0.fits"), new byte[]{1});
        assertEquals(409, assertThrows(ResponseStatusException.class, () ->
                downloads.ready(prepared.token())).getStatusCode().value());
    }

    @Test void rejectsAllMissingInsteadOfReturningAnEmptySuccessfulZip() throws Exception {
        daily.clear();
        Files.delete(root.resolve("望远镜/first/U0.fits"));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> prepare(false, true))
                .getStatusCode().value());
    }

    @Test void replacementPlanDoesNotExhaustQueueAndInvalidatesOldToken() {
        var first = prepare(false, false);
        String token = first.token();
        var groupIds = downloads.overview(query).groups().stream().map(ImageDownloadService.Group::id).toList();
        for (int i = 0; i < 12; i++) {
            token = downloads.prepare(new ImageDownloadService.Request(query, groupIds, false, false, token)).token();
        }
        assertEquals(410, assertThrows(ResponseStatusException.class, () -> downloads.ready(first.token())).getStatusCode().value());
        assertNotNull(downloads.ready(token));
    }

    @Test void skipsUndatedRecordsAndDoesNotPairUnknownTelescopeCalibrations() throws Exception {
        ImageOwn undated = new ImageOwn();
        undated.setPwd("undated.fits"); undated.setObject("U0");
        matched.add(undated);
        assertEquals(1, downloads.overview(query).ungroupedCount());
        u0.setTele(null);
        daily.add(image("unknown-tele-bias.fits", "BIAS", null, "2024-11-03T01:00:00", "BIAS"));
        assertEquals(0, downloads.overview(query).groups().get(0).calibrationCount());
        assertEquals(1, prepare(true, false).fileCount());
    }

    @Test void stopsImmediatelyOnClientDisconnectWithoutBufferingWholeArchive() {
        var prepared = prepare(false, false);
        OutputStream disconnected = new OutputStream() {
            int written;
            @Override public void write(int value) throws IOException {
                if (++written > 100) throw new IOException("client disconnected");
            }
        };
        assertThrows(IOException.class, () -> downloads.write(downloads.ready(prepared.token()), disconnected));
    }

    private ImageDownloadService.Prepared prepare(boolean calibration, boolean matchedOnly) {
        var ids = downloads.overview(query).groups().stream().map(ImageDownloadService.Group::id).toList();
        return downloads.prepare(new ImageDownloadService.Request(query, ids, calibration, matchedOnly, null));
    }

    private ImageOwn image(String path, String object, String telescope, String date, String type) throws Exception {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        byte[] bytes = new byte[5760];
        Arrays.fill(bytes, 0, 2880, (byte)' ');
        byte[] header = "SIMPLE  =                    T".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(header, 0, bytes, 0, header.length);
        new Random(path.hashCode()).nextBytes(bytes);
        // Preserve a recognizable FITS header and non-text pixel payload.
        System.arraycopy(header, 0, bytes, 0, header.length);
        Files.write(file, bytes);
        ImageOwn image = new ImageOwn();
        image.setPwd(path); image.setObject(object); image.setTele(telescope);
        image.setDateObs(LocalDateTime.parse(date)); image.setImagetyp(type);
        return image;
    }

    private Map<String, byte[]> unzip(ImageDownloadService.Prepared prepared) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        downloads.write(downloads.ready(prepared.token()), output);
        Map<String, byte[]> members = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(output.toByteArray()),
                java.nio.charset.StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                assertNull(members.put(entry.getName(), zip.readAllBytes()), "Duplicate archive entry");
            }
        }
        return members;
    }
}
