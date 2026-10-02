package mie.astronomy.controller;

import mie.astronomy.common.Result;
import mie.astronomy.service.ObservationReportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/observation-reports")
public class ObservationReportController {
    private final ObservationReportService service;

    public ObservationReportController(ObservationReportService service) { this.service = service; }

    @GetMapping("/stations")
    public Result<List<ObservationReportService.Station>> stations() throws IOException {
        return Result.success(service.stations());
    }

    @GetMapping("/{station}")
    public Result<List<ObservationReportService.Report>> reports(@PathVariable String station) throws IOException {
        return Result.success(service.list(station));
    }

    @GetMapping("/{station}/file")
    public ResponseEntity<Resource> file(@PathVariable String station, @RequestParam String path) throws IOException {
        Path file = service.file(station, path);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName().toString(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(service.contentType(file)))
                .contentLength(Files.size(file)).body(new FileSystemResource(file));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Result<Void>> storageError(IOException ex) {
        return ResponseEntity.internalServerError().body(Result.error("暂时无法读取观测报告，请稍后重试"));
    }
}
