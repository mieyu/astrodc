package mie.astronomy.controller;

import jakarta.servlet.http.HttpServletRequest;
import mie.astronomy.common.Result;
import mie.astronomy.dto.FileItemDto;
import mie.astronomy.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @GetMapping("/list")
    public Result<List<FileItemDto>> listFiles(@RequestParam(defaultValue = "") String path) {
        List<FileItemDto> files = fileService.listFiles(path);
        return Result.success(files);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String path, HttpServletRequest request) {
        Resource resource = fileService.loadFileAsResource(path);

        String contentType = null; // 初始化为 null
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // 忽略异常，contentType 保持为 null
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // 对文件名进行URL编码，以支持中文等特殊字符
        String encodedFilename = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }
}