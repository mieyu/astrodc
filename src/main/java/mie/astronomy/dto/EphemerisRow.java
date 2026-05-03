package mie.astronomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EphemerisRow {
    private String time;
    private String ra;
    private String de;

    @JsonProperty("ra_pure")
    private String raPure;

    @JsonProperty("de_pure")
    private String dePure;

    @JsonProperty("raw_line")
    private String rawLine;
}
