package mie.astronomy.dto;

import lombok.Data;

@Data
public class EphemerisCalculateRequest {
    private String satellite;
    private String observatory;
    private String initmom;
    private String plnvar = "0";
    private String nde = "6";
    private String ntimes = "1";
    private String timestep = "1";
}
