package mie.astronomy.dto;

import lombok.Data;

/**
 * 行星检测结果（用于前端标记绘制）
 */
@Data
public class ExposurePlanet {
    /** 中心 X（列，像素坐标） */
    private double cx;
    /** 中心 Y（行，像素坐标） */
    private double cy;
    /** 等效半径（像素） */
    private double radius;
    /** 当前每像素 SNR */
    private double snr;
    /** 行星像素数 */
    private int pixelCount;
}
