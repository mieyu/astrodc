package mie.astronomy.controller;

import mie.astronomy.common.Result;
import mie.astronomy.dto.CameraParams;
import mie.astronomy.dto.ExposureCalcResult;
import mie.astronomy.service.ExposureCalcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 天文曝光时间计算器接口。
 * FITS 图像分析与物理噪声模型计算全部在后端完成。
 */
@RestController
@CrossOrigin
@RequestMapping("/api/exposure")
public class ExposureCalcController {

    @Autowired
    private ExposureCalcService exposureCalcService;

    /**
     * 上传 FITS 并计算最佳曝光时间。
     */
    @PostMapping("/analyze")
    public Result<ExposureCalcResult> analyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "planet") String mode,
            @RequestParam(value = "targetSnr", defaultValue = "15") double targetSnr,
            @RequestParam(value = "maxExposure", defaultValue = "60") double maxExposure,
            @RequestParam(value = "gain", defaultValue = "2.0") double gain,
            @RequestParam(value = "readNoise", defaultValue = "6.33") double readNoise,
            @RequestParam(value = "darkCurrent", defaultValue = "0.002") double darkCurrent) {
        try {
            CameraParams cam = new CameraParams(gain, readNoise, darkCurrent);
            ExposureCalcResult result = exposureCalcService.analyze(file, mode, targetSnr, maxExposure, cam);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("计算失败: " + e.getMessage());
        }
    }

    /**
     * 对计算结果进行 AI 智能分析（复用后端 LLM，避免前端跨域）。
     */
    @PostMapping("/ai-analyze")
    public Result<String> aiAnalyze(@RequestBody ExposureCalcResult result) {
        try {
            return Result.success(exposureCalcService.aiAnalyze(result));
        } catch (Exception e) {
            return Result.error("AI 分析失败: " + e.getMessage());
        }
    }
}
