package mie.astronomy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import mie.astronomy.entity.Paper;
import mie.astronomy.mapper.PaperMapper;
import mie.astronomy.service.PaperService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    @Override
    public List<String> getGalaxies() {
        QueryWrapper<Paper> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT galaxy").isNotNull("galaxy");
        return this.list(queryWrapper).stream().map(Paper::getGalaxy).toList();
    }

    @Override
    public List<Paper> getPapersByGalaxy(String galaxy) {
        QueryWrapper<Paper> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("galaxy", galaxy);
        return this.list(queryWrapper);
    }
}