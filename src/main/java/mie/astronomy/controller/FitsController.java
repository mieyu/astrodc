package mie.astronomy.controller;

import mie.astronomy.common.Result;
import mie.astronomy.dto.FitsHeaderCard;
import mie.astronomy.service.FitsHeaderService;
import mie.astronomy.service.FitsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/fits")
public class FitsController {
    @Autowired
    private FitsHeaderService fitsHeaderService;

    @Autowired
    private FitsService fitsService;

    @GetMapping(value = "/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getFitsImage(@RequestParam String path) {
        try {
            byte[] imageBytes = fitsService.fitsToPng(path);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes);
        } catch (Exception e) {
            // 在实际项目中，这里应该返回一个默认的“图像加载失败”图片
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
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