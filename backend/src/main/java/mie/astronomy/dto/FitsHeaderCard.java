package mie.astronomy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FitsHeaderCard {
    private String keyword;
    private String value;
    private String comment;
}