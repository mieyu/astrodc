package mie.astronomy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import mie.astronomy.entity.Paper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaperMapper extends BaseMapper<Paper> {
}