package mie.astronomy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import mie.astronomy.dto.PositioningNormalizedQuery;
import mie.astronomy.entity.PositioningNormalized;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;

import java.util.List;

@Mapper
public interface PositioningNormalizedMapper extends BaseMapper<PositioningNormalized> {

    /**
     * 取某列的去重值(下拉框候选)。fieldName 必须由调用方做白名单校验。
     */
    List<String> selectDistinctValues(@Param("fieldName") String fieldName);

    /**
     * 流式导出。返回 Cursor 配合 fetchSize=Integer.MIN_VALUE 实现 MySQL 的流式读取,
     * 调用方需在事务/SqlSession 内使用 try-with-resources 消费。
     */
    Cursor<PositioningNormalized> streamByQuery(@Param("q") PositioningNormalizedQuery query);
}
