package mie.astronomy.dto;

import lombok.Data;

import java.util.List;

/**
 * 观测条件评估结果（恒星模式）
 */
@Data
public class ExposureConditionAssessment {
    /** 综合评分 0-100 */
    private int score;
    /** 评级：优 / 良 / 一般 / 差 */
    private String level;
    /** 天光速率 ADU/s */
    private double skyRateAdu;
    /** 天光等级描述 */
    private String skyLevelDesc;
    /** 发现的问题 */
    private List<String> warnings;
    /** 改善建议 */
    private List<String> suggestions;
}
