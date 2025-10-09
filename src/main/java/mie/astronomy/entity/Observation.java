package mie.astronomy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("total")
public class Observation {

    /**
     * Primary Key: PWD (varchar)
     */
    @TableId(value = "PWD", type = IdType.INPUT)
    private String pwd;

    /**
     * FITS-NAME (text)
     */
    @TableField("`FIT-NAME`")
    private String fitName;

    /**
     * SIMPLE (int)
     */
    @TableField("SIMPLE")
    private Integer simple;

    /**
     * BITPIX (int)
     */
    @TableField("BITPIX")
    private Integer bitpix;

    /**
     * NAXIS (int)
     */
    @TableField("NAXIS")
    private Integer naxis;

    /**
     * NAXIS1 (int)
     */
    @TableField("NAXIS1")
    private Integer naxis1;

    /**
     * NAXIS2 (int)
     */
    @TableField("NAXIS2")
    private Integer naxis2;

    /**
     * BSCALE (double)
     */
    @TableField("BSCALE")
    private Double bscale;

    /**
     * BZERO (double)
     */
    @TableField("BZERO")
    private Double bzero;

    /**
     * OBJECT (text)
     */
    @TableField("OBJECT")
    private String object;

    /**
     * IMAGETYP (text)
     */
    @TableField("IMAGETYP")
    private String imagetyp;

    /**
     * DATE-OBS (datetime)
     * Using LocalDateTime to map to the datetime column.
     * The JsonFormat annotation ensures consistent date formatting for the frontend.
     */
    @TableField("`DATE-OBS`")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateObs;


    /**
     * EXPTIME (double)
     */
    @TableField("EXPTIME")
    private Double exptime;

    /**
     * OTCD (int)
     */
    @TableField("OTCD")
    private Integer otcd;

    /**
     * TELE (text)
     */
    @TableField("TELE")
    private String tele;

    /**
     * TELEAP (int)
     */
    @TableField("TELEAP")
    private Integer teleap;

    /**
     * TELEFL (int)
     */
    @TableField("TELEFL")
    private Integer telefl;



    /**
     * RA-CENTER (string)
     */
    @TableField("`R-CENTER`")
    private String rCenter;

    /**
     * DE-CENTER (string)
     */
    @TableField("`D-CENTER`")
    private String dCenter;

    /**
     * XPIXSZ (double)
     */
    @TableField("XPIXSZ")
    private Double xpixsz;

    /**
     * YPIXSZ (double)
     */
    @TableField("YPIXSZ")
    private Double ypixsz;

    /**
     * XBINNING (int)
     */
    @TableField("XBINNING")
    private Integer xbinning;

    /**
     * YBINNING (int)
     */
    @TableField("YBINNING")
    private Integer ybinning;

    /**
     * FILTER (text)
     */
    @TableField("FILTER")
    private String filter;

    /**
     * ROT-ANGLE (double)
     */
    @TableField("`RT-ANGLE`")
    private Double rtAngle;

    /**
     * FLIPX (int)
     */
    @TableField("FLIPX")
    private Integer flipx;

    /**
     * FLIPY (int)
     */
    @TableField("FLIPY")
    private Integer flipy;

    /**
     * ROT-CODE (int)
     */
    @TableField("`ROT-CODE`")
    private Integer rotCode;
}
