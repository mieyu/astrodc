package mie.astronomy.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import mie.astronomy.common.Result;
import mie.astronomy.dto.ImageOwnQueryDto;
import mie.astronomy.entity.ImageOwn;
import mie.astronomy.service.ImageOwnService;
import mie.astronomy.service.ImageDownloadService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/image/own")
public class ImageOwnController {
    @Autowired
    private ImageOwnService imageOwnService;

    @Autowired
    private ImageDownloadService downloads;

    @PostMapping("/downloads/groups")
    public Result<ImageDownloadService.Overview> downloadGroups(@RequestBody ImageOwnQueryDto query) {
        return Result.success(downloads.overview(query));
    }

    @PostMapping("/downloads/prepare")
    public Result<ImageDownloadService.Prepared> prepareDownload(@RequestBody ImageDownloadService.Request request) {
        return Result.success(downloads.prepare(request));
    }

    @GetMapping("/downloads/{token}/check")
    public Result<Boolean> checkDownload(@PathVariable String token) {
        downloads.ready(token);
        return Result.success(true);
    }
    @GetMapping("/downloads/{token}")
    public void downloadFits(@PathVariable String token, HttpServletResponse response) throws IOException {
        ImageDownloadService.Plan plan = downloads.ready(token);
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(plan.filename(), StandardCharsets.UTF_8).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        downloads.write(plan, response.getOutputStream());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Object>> downloadError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Result.error(error.getReason()));
    }
    @GetMapping("/queryAll")
    public Result queryAll() {
        List<ImageOwn> list = imageOwnService.getAllImageOwn();
        return Result.success(list);
    }

    @GetMapping("/query100")
    public Result query100() {
        List<ImageOwn> list = imageOwnService.get100ImageOwn();
        return Result.success(list);
    }

    @GetMapping("/options")
    public Result<Map<String, List<Object>>> getOptions() {
        Map<String, List<Object>> options = imageOwnService.getDropdownOptions();
        return Result.success(options);
    }

    @PostMapping("/search")
    public Result<IPage<ImageOwn>> search(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "100") long pageSize,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            @RequestBody ImageOwnQueryDto query) {
        long size = Math.max(1, Math.min(pageSize, 1000));
        IPage<ImageOwn> pageResult = imageOwnService.search(query, page, size, sortField, sortOrder);
        return Result.success(pageResult);
    }

    @PostMapping({"/metadata/export", "/download"})
    public void exportMetadata(@RequestBody ImageOwnQueryDto query, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=utf-8");
        response.getWriter().write(new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}));
        response.setHeader("Content-Disposition", "attachment; filename=\"image_own.csv\"");

        List<ImageOwn> list = imageOwnService.listByQuery(query);

        try (PrintWriter writer = response.getWriter()) {
            Field[] fields = ImageOwn.class.getDeclaredFields();

            String header = Stream.of(fields).map(Field::getName).collect(Collectors.joining(","));
            writer.println(header);

            for (ImageOwn obs : list) {
                for (int i = 0; i < fields.length; i++) {
                    try {
                        fields[i].setAccessible(true);
                        Object value = fields[i].get(obs);
                        String formattedValue = (value == null) ? "" : "\"" + value.toString().replace("\"", "\"\"") + "\"";
                        writer.print(formattedValue);
                    } catch (IllegalAccessException e) {
                        writer.print("");
                    }
                    if (i < fields.length - 1) {
                        writer.print(",");
                    }
                }
                writer.println();
            }
        }
    }

    @PutMapping("/update")
    public Result<?> update(@RequestBody ImageOwn imageOwn) {
        return Result.error("自有图像数据修改功能已临时关闭");
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = imageOwnService.count();
        stats.put("total", total);
        stats.put("imageType", imageOwnService.getImageTypeStats());
        stats.put("object", imageOwnService.getObjectStats());
        stats.put("year", imageOwnService.getYearStats());
        stats.put("month", imageOwnService.getMonthStats());
        return Result.success(stats);
    }
}
