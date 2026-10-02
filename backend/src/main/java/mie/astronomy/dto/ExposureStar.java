package mie.astronomy.dto;

import lombok.Data;

/**
 * 单颗恒星检测信息
 */
@Data
public class ExposureStar {
    /** 重心 X（列，子像素精度） */
    private double cx;
    /** 重心 Y（行，子像素精度） */
    private double cy;
    /** 标记半径（像素） */
    private double radius;
    /** 覆盖面积（像素） */
    private int area;
    /** 总通量（减背景，ADU） */
    private double totalFluxAdu;
    /** 平均通量（ADU/px） */
    private double meanFluxAdu;
    /** 峰值（未减背景，ADU） */
    private double peakAdu;
    /** 精确 SNR 估计 */
    private double snrEstimate;
    /** 是否过曝 */
    private boolean saturated;
    /** 是否为选中的典型恒星 */
    private boolean selected;
}
