package mie.astronomy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ImageOwnQueryDto {
    private String pwd;
    private String fitName;
    private Integer simple;
    private String object;
    private String imagetyp;
    private Double exptime;
    private Integer otcd;
    private String tele;
    private Integer teleap;
    private Integer telefl;
    private String filter;
    private Integer flipx;
    private Integer flipy;
    private Integer rotCode;
    private String naxis;
    private String rCenter;
    private String dCenter;

    private Integer bitpixMin;
    private Integer naxisMin;
    private Integer naxis1Min;
    private Integer naxis2Min;
    private Double bscaleMin;
    private Double bzeroMin;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateObsMin;
    private Double xpixszMin;
    private Double ypixszMin;
    private Integer xbinningMin;
    private Integer ybinningMin;
    private Double rtAngleMin;

    private Integer bitpixMax;
    private Integer naxisMax;
    private Integer naxis1Max;
    private Integer naxis2Max;
    private Double bscaleMax;
    private Double bzeroMax;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateObsMax;
    private Double xpixszMax;
    private Double ypixszMax;
    private Integer xbinningMax;
    private Integer ybinningMax;
    private Double rtAngleMax;
}
