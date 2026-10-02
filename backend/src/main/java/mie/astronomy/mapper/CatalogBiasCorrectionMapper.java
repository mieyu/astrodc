package mie.astronomy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import mie.astronomy.dto.CatalogBiasCorrectionQuery;
import mie.astronomy.entity.CatalogBiasCorrection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;

import java.util.Map;

@Mapper
public interface CatalogBiasCorrectionMapper extends BaseMapper<CatalogBiasCorrection> {

    Cursor<Map<String, Object>> streamByQuery(@Param("q") CatalogBiasCorrectionQuery query);
}
