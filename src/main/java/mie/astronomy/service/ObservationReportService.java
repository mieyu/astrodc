package mie.astronomy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Read-only archive. Files are maintained directly in the station directories. */
@Service
public class ObservationReportService {
    private static final Map<String, String> STATIONS = Map.of(
            "yunnan", "云南天文台", "xinglong", "兴隆观测基地");
    private static final List<String> STATION_ORDER = List.of("yunnan", "xinglong");
    private static final Map<String, String> TYPES = Map.of(
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "pdf", "application/pdf", "md", "text/markdown", "markdown", "text/markdown",
            "doc", "application/msword");
    private final Path root;

    public ObservationReportService(
            @Value("${observation-reports.root-path:${ASTRONOMY_REPORT_ROOT:E:/Observation_reports}}") String rootPath)
            throws IOException {
        root = Path.of(rootPath).toAbsolutePath().normalize();
        for (String name : STATIONS.values()) Files.createDirectories(root.resolve(name));
    }

    public record Report(String path, String name, String type, long size, Instant modifiedAt) {}
    public record Station(String id, String name, int count) {}

    public List<Station> stations() throws IOException {
        List<Station> result = new ArrayList<>();
        for (String id : STATION_ORDER) result.add(new Station(id, STATIONS.get(id), list(id).size()));
        return result;
    }

    public List<Report> list(String station) throws IOException {
        Path directory = stationDirectory(station);
        List<Report> result = new ArrayList<>();
        try (var paths = Files.walk(directory, 10)) {
            for (Path path : paths.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList()) {
                if (path.getFileName().toString().startsWith("~$") || !TYPES.containsKey(extension(path))) continue;
                if (!path.toRealPath().startsWith(directory)) continue;
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                result.add(new Report(directory.relativize(path).toString().replace('\\', '/'),
                        path.getFileName().toString(), extension(path), attrs.size(), attrs.lastModifiedTime().toInstant()));
            }
        }
        result.sort(Comparator.comparing(Report::modifiedAt).reversed().thenComparing(Report::path));
        return result;
    }

    public Path file(String station, String relativePath) throws IOException {
        Path directory = stationDirectory(station);
        if (relativePath == null || relativePath.isBlank() || relativePath.contains("\\")
                || relativePath.contains(":") || relativePath.indexOf('\0') >= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的文档路径");
        Path candidate;
        try {
            candidate = directory.resolve(relativePath).normalize();
        } catch (java.nio.file.InvalidPathException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的文档路径");
        }
        if (Path.of(relativePath).isAbsolute() || !candidate.startsWith(directory))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的文档路径");
        if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)
                || !TYPES.containsKey(extension(candidate)) || candidate.getFileName().toString().startsWith("~$"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文档不存在");
        Path realPath = candidate.toRealPath();
        if (!realPath.startsWith(directory))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的文档路径");
        return realPath;
    }

    private Path stationDirectory(String station) throws IOException {
        String name = STATIONS.get(station);
        if (name == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "台站不存在");
        Path directory = root.resolve(name).toRealPath();
        if (!directory.startsWith(root.toRealPath()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的台站目录");
        return directory;
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public String contentType(Path path) { return TYPES.get(extension(path)); }
}
