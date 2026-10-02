package mie.astronomy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * positioning_normalized 表实体。
 *
 * Java 字段一律 camelCase(满足 JavaBean / Lombok / MyBatis 全局
 * mapUnderscoreToCamelCase=true 的自动列映射),JSON key 通过 @JsonProperty
 * 强制输出 snake_case,严格对应 DB 列名,以满足前端"按下划线读字段"的约定。
 *
 * 对于本身就含大小写混合的列(ra_A_icrs / dRA_star_cosdec_arcsec / dDEC_star_arcsec),
 * 不能依赖自动转换,@TableField 显式指定列名,@JsonProperty 显式指定输出 key。
 */
@Data
@TableName("positioning_normalized")
public class PositioningNormalized {

    @TableId(value = "global_id", type = IdType.INPUT)
    @JsonProperty("global_id")
    private String globalId;

    @JsonProperty("target_id")
    private String targetId;

    @JsonProperty("relative_to")
    private String relativeTo;

    @JsonProperty("jd1")
    private Double jd1;

    @JsonProperty("jd2")
    private Double jd2;

    @JsonProperty("jd_tt1")
    private Double jdTt1;

    @JsonProperty("jd_tt2")
    private Double jdTt2;

    @JsonProperty("time_scale")
    private String timeScale;

    @JsonProperty("epoch")
    private String epoch;

    @JsonProperty("obs_type")
    private String obsType;

    @JsonProperty("w1")
    private Double w1;

    @JsonProperty("w2")
    private Double w2;

    @JsonProperty("w1_unit")
    private String w1Unit;

    @JsonProperty("w2_unit")
    private String w2Unit;

    @JsonProperty("coordinates")
    private String coordinates;

    @TableField("`reference`")  // reference 是 MySQL 关键字
    @JsonProperty("reference")
    private String reference;

    @JsonProperty("centre_of_frame")
    private String centreOfFrame;

    @JsonProperty("obs_site")
    private String obsSite;

    @JsonProperty("observer_pos")
    private String observerPos;

    @JsonProperty("receptor")
    private String receptor;

    @JsonProperty("telescope")
    private String telescope;

    @JsonProperty("reduction")
    private String reduction;

    @JsonProperty("coord_route")
    private String coordRoute;

    @JsonProperty("coord_status")
    private String coordStatus;

    @JsonProperty("coord_note")
    private String coordNote;

    @JsonProperty("w1_icrs")
    private Double w1Icrs;

    @JsonProperty("w2_icrs")
    private Double w2Icrs;

    // 大小写混合列名,自动 underscoreToCamel 不适用,显式映射
    @TableField("ra_A_icrs")
    @JsonProperty("ra_A_icrs")
    private Double raAIcrs;

    @TableField("dec_A_icrs")
    @JsonProperty("dec_A_icrs")
    private Double decAIcrs;

    @TableField("ra_B_horizons")
    @JsonProperty("ra_B_horizons")
    private Double raBHorizons;

    @TableField("dec_B_horizons")
    @JsonProperty("dec_B_horizons")
    private Double decBHorizons;

    @JsonProperty("w1_theory")
    private Double w1Theory;

    @JsonProperty("w2_theory")
    private Double w2Theory;

    @JsonProperty("w1_nss")
    private Double w1Nss;

    @JsonProperty("w2_nss")
    private Double w2Nss;

    @JsonProperty("w1_nss_icrs")
    private Double w1NssIcrs;

    @JsonProperty("w2_nss_icrs")
    private Double w2NssIcrs;

    @JsonProperty("oc_horizon_icrs_w1")
    private Double ocHorizonIcrsW1;

    @JsonProperty("oc_horizon_w2")
    private Double ocHorizonW2;

    @JsonProperty("oc_nss_native_w1")
    private Double ocNssNativeW1;

    @JsonProperty("oc_nss_native_w2")
    private Double ocNssNativeW2;

    @JsonProperty("oc_nss_icrs_w1")
    private Double ocNssIcrsW1;

    @JsonProperty("oc_nss_icrs_w2")
    private Double ocNssIcrsW2;

    @TableField("dRA_star_cosdec_arcsec")
    @JsonProperty("dRA_star_cosdec_arcsec")
    private Double dRAStarCosdecArcsec;

    @TableField("dDEC_star_arcsec")
    @JsonProperty("dDEC_star_arcsec")
    private Double dDECStarArcsec;

    @JsonProperty("source_web_html")
    private String sourceWebHtml;

    @JsonProperty("source_web_txt")
    private String sourceWebTxt;

    @JsonProperty("source_local_html")
    private String sourceLocalHtml;

    @JsonProperty("source_local_txt")
    private String sourceLocalTxt;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
