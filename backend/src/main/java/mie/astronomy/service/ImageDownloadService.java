package mie.astronomy.service;

import mie.astronomy.dto.ImageOwnQueryDto;
import mie.astronomy.entity.ImageOwn;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Groups observations by recorded DATE-OBS day, target and telescope; streams original bytes. */
@Service
public class ImageDownloadService {
    private static final Set<String> CALIBRATIONS = Set.of("BIAS", "FLAT", "DARK");
    private static final Duration PLAN_LIFETIME = Duration.ofMinutes(30);
    private final ImageOwnService images;
    private final FileService files;
    private final Map<String, Plan> plans = new ConcurrentHashMap<>();

    public ImageDownloadService(ImageOwnService images, FileService files) {
        this.images = images;
        this.files = files;
    }

    public record Group(String id, String date, String object, String tele,
                        long matchedCount, long fileCount, long calibrationCount) {}
    public record Overview(List<Group> groups, long ungroupedCount) {}
    public record Request(ImageOwnQueryDto query, List<String> groupIds,
                          boolean includeCalibration, boolean matchedOnly, String previousToken) {}
    public record Prepared(String token, String filename, int fileCount, long totalBytes,
                           int missingCount, List<String> missingExamples, Instant expiresAt) {}
    private record DayTarget(LocalDate date, String object, String tele) {
        String id() {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    (date + "\n" + object + "\n" + tele).getBytes(StandardCharsets.UTF_8));
        }
    }
    private record Selection(List<ImageOwn> matched, List<ImageOwn> daily, Map<String, DayTarget> keys,
                             Overview overview) {}
    private record Entry(Path path, String name, long size, long modified) {}
    public record Plan(String filename, List<Entry> entries, long totalBytes, Instant expiresAt) {}

    public Overview overview(ImageOwnQueryDto query) {
        return selection(query).overview();
    }

    private Selection selection(ImageOwnQueryDto query) {
        List<ImageOwn> matched = images.listByQuery(query == null ? new ImageOwnQueryDto() : query);
        Map<DayTarget, Long> counts = new LinkedHashMap<>();
        long ungrouped = 0;
        for (ImageOwn image : matched) {
            DayTarget key = key(image);
            if (key == null) { ungrouped++; continue; }
            counts.merge(key, 1L, Long::sum);
        }
        if (counts.isEmpty()) return new Selection(matched, List.of(), Map.of(), new Overview(List.of(), ungrouped));
        LocalDate first = counts.keySet().stream().map(DayTarget::date).min(LocalDate::compareTo).orElseThrow();
        LocalDate last = counts.keySet().stream().map(DayTarget::date).max(LocalDate::compareTo).orElseThrow();
        List<ImageOwn> daily = images.listDownloadMetadata(first.atStartOfDay(), last.plusDays(1).atStartOfDay());
        Map<DayTarget, Long> totals = new HashMap<>();
        Map<String, Long> calibrations = new HashMap<>();
        for (ImageOwn image : daily) {
            DayTarget key = key(image);
            if (key == null) continue;
            totals.merge(key, 1L, Long::sum);
            if (isCalibration(image) && !key.tele().isEmpty()) {
                calibrations.merge(key.date() + "\n" + key.tele(), 1L, Long::sum);
            }
        }
        List<Group> groups = new ArrayList<>();
        Map<String, DayTarget> keys = new HashMap<>();
        for (Map.Entry<DayTarget, Long> entry : counts.entrySet()) {
            DayTarget key = entry.getKey();
            keys.put(key.id(), key);
            groups.add(new Group(key.id(), key.date().toString(), key.object(), key.tele(),
                    entry.getValue(), totals.getOrDefault(key, entry.getValue()),
                    key.tele().isEmpty() ? 0 : calibrations.getOrDefault(key.date() + "\n" + key.tele(), 0L)));
        }
        groups.sort(Comparator.comparing(Group::date).reversed()
                .thenComparing(Group::object).thenComparing(Group::tele));
        return new Selection(matched, daily, keys, new Overview(groups, ungrouped));
    }

    public Prepared prepare(Request request) {
        if (request == null || request.groupIds() == null || request.groupIds().isEmpty()) {
            throw badRequest("请先选择要下载的观测日。");
        }
        Selection selection = selection(request.query());
        Set<DayTarget> chosen = new HashSet<>();
        for (String id : request.groupIds()) {
            DayTarget key = selection.keys().get(id);
            if (key == null) throw badRequest("查询结果已变化，请重新选择观测日。");
            chosen.add(key);
        }
        Set<String> calibrationDays = new HashSet<>();
        if (request.includeCalibration()) {
            for (DayTarget key : chosen) {
                if (!key.tele().isEmpty()) calibrationDays.add(key.date() + "\n" + key.tele());
            }
        }
        Map<String, ImageOwn> candidates = new LinkedHashMap<>();
        List<ImageOwn> source = request.matchedOnly() ? selection.matched() : selection.daily();
        for (ImageOwn image : source) {
            if (chosen.contains(key(image))) candidates.put(image.getPwd(), image);
        }
        if (request.includeCalibration()) {
            for (ImageOwn image : selection.daily()) {
                DayTarget key = key(image);
                if (key != null && isCalibration(image) && calibrationDays.contains(key.date() + "\n" + key.tele())) {
                    candidates.put(image.getPwd(), image);
                }
            }
        }
        List<Entry> entries = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        List<String> missing = new ArrayList<>();
        int missingCount = 0;
        long bytes = 0;
        for (ImageOwn image : candidates.values()) {
            try {
                Path path = files.resolvePath(image.getPwd());
                if (!isFits(path.getFileName().toString()) || !Files.isRegularFile(path)
                        || !Files.isReadable(path) || Files.size(path) == 0) {
                    throw new IOException("文件不存在、为空或不是 FITS");
                }
                Path real = path.toRealPath();
                if (!seen.add(real)) continue;
                long size = Files.size(real);
                String name = files.storageRelativePath(image.getPwd()).replace('\\', '/');
                entries.add(new Entry(real, name, size, Files.getLastModifiedTime(real).toMillis()));
                bytes = Math.addExact(bytes, size);
            } catch (IOException | RuntimeException e) {
                missingCount++;
                if (missing.size() < 10) missing.add(displayName(image));
            }
        }
        if (entries.isEmpty()) throw badRequest("所选数据没有可下载的原始 FITS 文件，请检查数据存储；缺失或不可读 " + missingCount + " 个文件。");
        entries.sort(Comparator.comparing(Entry::name));
        String filename = filename(chosen);
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plus(PLAN_LIFETIME);
        synchronized (plans) {
            if (request.previousToken() != null) plans.remove(request.previousToken());
            plans.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(Instant.now()));
            if (plans.size() >= 8) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "下载准备请求较多，请稍后再试。");
            }
            plans.put(token, new Plan(filename, List.copyOf(entries), bytes, expiry));
        }
        return new Prepared(token, filename, entries.size(), bytes, missingCount, List.copyOf(missing), expiry);
    }

    /** Verify all files before sending HTTP headers, so unavailable data never produces a successful empty ZIP. */
    public Plan ready(String token) {
        Plan plan = plans.get(token);
        if (plan == null || plan.expiresAt().isBefore(Instant.now())) {
            plans.remove(token);
            throw new ResponseStatusException(HttpStatus.GONE, "下载已过期，请重新准备。");
        }
        for (Entry entry : plan.entries()) {
            try { verify(entry); }
            catch (IOException e) {
                plans.remove(token);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "原始文件已变化，请重新准备下载。");
            }
        }
        return plan;
    }

    public void write(Plan plan, OutputStream output) throws IOException {
        ArchiveStream zip = new ArchiveStream(output);
        zip.setLevel(Deflater.NO_COMPRESSION);
        try {
            for (Entry entry : plan.entries()) {
                verify(entry);
                ZipEntry member = new ZipEntry(entry.name());
                member.setTime(entry.modified());
                zip.putNextEntry(member);
                Files.copy(entry.path(), zip);
                verify(entry);
                zip.closeEntry();
            }
            zip.finish();
            zip.flush();
        } finally {
            zip.release();
        }
    }

    private static final class ArchiveStream extends ZipOutputStream {
        ArchiveStream(OutputStream output) { super(output, StandardCharsets.UTF_8); }
        // Never finish a failed archive: the browser must detect an incomplete download.
        void release() { def.end(); }
    }
    private static void verify(Entry entry) throws IOException {
        if (!Files.isRegularFile(entry.path()) || !Files.isReadable(entry.path())
                || Files.size(entry.path()) != entry.size()
                || Files.getLastModifiedTime(entry.path()).toMillis() != entry.modified()) {
            throw new IOException("原始文件已变化: " + entry.name());
        }
    }

    private static DayTarget key(ImageOwn image) {
        if (image.getDateObs() == null || text(image.getObject()).isEmpty()) return null;
        return new DayTarget(image.getDateObs().toLocalDate(), text(image.getObject()), text(image.getTele()));
    }

    private static boolean isCalibration(ImageOwn image) {
        return CALIBRATIONS.contains(text(image.getObject()).toUpperCase(Locale.ROOT))
                || CALIBRATIONS.contains(text(image.getImagetyp()).toUpperCase(Locale.ROOT));
    }

    private static boolean isFits(String filename) {
        return filename.toLowerCase(Locale.ROOT).matches(".*\\.(fits?|fts)(\\.(gz|fz))?");
    }

    private static String displayName(ImageOwn image) {
        String path = text(image.getPwd()).replace('\\', '/');
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static String filename(Set<DayTarget> chosen) {
        List<LocalDate> dates = chosen.stream().map(DayTarget::date).sorted().toList();
        Set<String> objects = new HashSet<>();
        chosen.forEach(key -> objects.add(key.object()));
        String target = objects.size() == 1 ? objects.iterator().next().replaceAll("[^a-zA-Z0-9_-]", "_") : "observations";
        return "astrodc_" + target + "_" + dates.get(0)
                + (dates.get(0).equals(dates.get(dates.size() - 1)) ? "" : "_" + dates.get(dates.size() - 1)) + ".zip";
    }

    private static String text(String value) { return value == null ? "" : value.trim(); }
    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
