package mie.astronomy.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 曝光时间计算完整结果。
 * 同时用于：1) /api/exposure/analyze 的返回；2) /api/exposure/ai-analyze 的请求体。
 */
@Data
public class ExposureCalcResult {
    private boolean valid;
    private String message;
    /** planet / star */
    private String mode;

    private double exptimeCurrent;
    private double exptimeRecommended;
    private double snrCurrent;

    private double targetSnr;
    private CameraParams cameraParams;

    /** 背景、行星信号等诊断量（ADU） */
    private Map<String, Double> diagnostics;
    /** 噪声分解（百分比），可能为空 */
    private Map<String, Double> noiseInfo;

    /** 行星检测（行星模式） */
    private ExposurePlanet planet;

    /** 全部检测到的恒星（恒星模式，供绘图） */
    private List<ExposureStar> stars;
    /** 选中的典型恒星 */
    private ExposureStar selectedStar;
    private int starsDetected;
    private int starsReliable;
    private int starsSaturated;
    private boolean allReliableSaturated;

    private ExposureConditionAssessment conditionAssessment;

    /** 受最大曝光限制时，可达到的 SNR */
    private Double suggestedSnr;
    private Double maxExposureUsed;
    /** 峰值偏高等非阻断警告 */
    private String warning;

    /** 完整文本报告（与桌面版一致） */
    private String report;
    /** 图像预览区的检测摘要 */
    private String previewSummary;

    /** 预览图尺寸 */
    private int width;
    private int height;
    /** min-max 拉伸后的灰度预览 PNG（base64，不含 data: 前缀） */
    private String imagePngBase64;
}
