package mie.astronomy.controller;

import mie.astronomy.common.Result;
import mie.astronomy.dto.FitsHeaderCard;
import mie.astronomy.service.FileService;
import mie.astronomy.service.FitsHeaderService;
import mie.astronomy.service.FitsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/fits")
public class FitsController {
    private static final Logger log = LoggerFactory.getLogger(FitsController.class);

    @Autowired
    private FitsHeaderService fitsHeaderService;

    @Autowired
    private FitsService fitsService;

    @Autowired
    private FileService fileService;

    @GetMapping(value = "/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getFitsImage(
            @RequestParam String path,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        try {
            Path resolvedPath = fileService.resolvePath(path);
            if (!Files.isRegularFile(resolvedPath) || !Files.isReadable(resolvedPath)) {
                return ResponseEntity.notFound().build();
            }

            String etag = createEtag(resolvedPath);
            CacheControl cacheControl = CacheControl.maxAge(Duration.ofHours(6)).cachePrivate();
            if (etag.equals(ifNoneMatch)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(cacheControl)
                    .build();
            }

            byte[] imageBytes = fitsService.fitsToPng(path);
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(imageBytes.length)
                .eTag(etag)
                .cacheControl(cacheControl)
                .body(imageBytes);
        } catch (Exception e) {
            log.warn("Failed to generate FITS preview for {}: {}", path, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String createEtag(Path path) throws Exception {
        long modified = Files.getLastModifiedTime(path).toMillis();
        long size = Files.size(path);
        String value = path.toAbsolutePath().normalize() + ":" + modified + ":" + size;
        return "\"fits-" + Integer.toHexString(value.hashCode()) + "-" + Long.toHexString(modified) + "\"";
    }


    @PostMapping("/parse")
    public Result<List<FitsHeaderCard>> parseHeader(@RequestParam("file") MultipartFile file) {
        try {
            List<FitsHeaderCard> headerData = fitsHeaderService.parseHeader(file);
            return Result.success(headerData);
        } catch (Exception e) {
            return Result.error("解析失败: " + e.getMessage());
        }
    }
}
