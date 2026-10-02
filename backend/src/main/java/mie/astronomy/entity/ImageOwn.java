package mie.astronomy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("image_own")
public class ImageOwn {

    @TableId(value = "PWD", type = IdType.INPUT)
    private String pwd;

    @TableField("`FIT-NAME`")
    private String fitName;

    @TableField("SIMPLE")
    private Integer simple;

    @TableField("BITPIX")
    private Integer bitpix;

    @TableField("NAXIS")
    private Integer naxis;

    @TableField("NAXIS1")
    private Integer naxis1;

    @TableField("NAXIS2")
    private Integer naxis2;

    @TableField("BSCALE")
    private Double bscale;

    @TableField("BZERO")
    private Double bzero;

    @TableField("OBJECT")
    private String object;

    @TableField("IMAGETYP")
    private String imagetyp;

    @TableField("`DATE-OBS`")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateObs;

    @TableField("EXPTIME")
    private Double exptime;

    @TableField("OTCD")
    private Integer otcd;

    @TableField("TELE")
    private String tele;

    @TableField("TELEAP")
    private Integer teleap;

    @TableField("TELEFL")
    private Integer telefl;

    @TableField("`R-CENTER`")
    private String rCenter;

    @TableField("`D-CENTER`")
    private String dCenter;

    @TableField("XPIXSZ")
    private Double xpixsz;

    @TableField("YPIXSZ")
    private Double ypixsz;

    @TableField("XBINNING")
    private Integer xbinning;

    @TableField("YBINNING")
    private Integer ybinning;

    @TableField("FILTER")
    private String filter;

    @TableField("`RT-ANGLE`")
    private Double rtAngle;

    @TableField("FLIPX")
    private Integer flipx;

    @TableField("FLIPY")
    private Integer flipy;

    @TableField("`ROT-CODE`")
    private Integer rotCode;
}
