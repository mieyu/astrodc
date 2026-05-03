package mie.astronomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EphemerisChartRequest {
    private String ra;
    private String dec;

    @JsonProperty("satellite_name")
    private String satelliteName;

    @JsonProperty("time_str")
    private String timeStr;

    private Double fov = 7.0;
}
