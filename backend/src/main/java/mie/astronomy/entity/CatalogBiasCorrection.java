package mie.astronomy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@TableName("catalog_bias_correction")
public class CatalogBiasCorrection {

    @TableId(value = "ipix", type = IdType.INPUT)
    @JsonProperty("ipix")
    private Long ipix;
}
