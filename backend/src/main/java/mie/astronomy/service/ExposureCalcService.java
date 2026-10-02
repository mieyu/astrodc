package mie.astronomy.service;

import mie.astronomy.dto.CameraParams;
import mie.astronomy.dto.ExposureCalcResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 天文曝光时间计算器服务（FITS 分析 + 物理噪声模型 + AI 解读）。
 */
public interface ExposureCalcService {

    /**
     * 分析上传的 FITS 图像，计算达到目标 SNR 所需的最佳曝光时间。
     *
     * @param file        FITS 文件
     * @param mode        观测模式：planet（行星，每像素SNR）/ star（恒星，总通量SNR）
     * @param targetSnr   目标信噪比
     * @param maxExposure 最大可接受曝光时间（秒）
     * @param cam         相机参数
     */
    ExposureCalcResult analyze(MultipartFile file, String mode, double targetSnr,
                              double maxExposure, CameraParams cam) throws Exception;

    /**
     * 调用 LLM 对计算结果进行专业解读，返回观测建议文本。
     */
    String aiAnalyze(ExposureCalcResult result);
}
