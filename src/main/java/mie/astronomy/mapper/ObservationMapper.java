package mie.astronomy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import mie.astronomy.entity.Observation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ObservationMapper extends BaseMapper<Observation> {
}
