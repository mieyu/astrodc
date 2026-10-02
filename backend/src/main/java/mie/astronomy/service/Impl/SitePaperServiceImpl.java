package mie.astronomy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import mie.astronomy.entity.SitePaper;
import mie.astronomy.mapper.SitePaperMapper;
import mie.astronomy.service.SitePaperService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SitePaperServiceImpl extends ServiceImpl<SitePaperMapper, SitePaper> implements SitePaperService {

    @Override
    public List<String> getGalaxies() {
        QueryWrapper<SitePaper> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT galaxy").isNotNull("galaxy");
        return this.list(queryWrapper).stream().map(SitePaper::getGalaxy).toList();
    }

    @Override
    public List<SitePaper> getPapersByGalaxy(String galaxy) {
        QueryWrapper<SitePaper> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("galaxy", galaxy);
        return this.list(queryWrapper);
    }
}
