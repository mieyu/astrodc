package mie.astronomy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import mie.astronomy.entity.SitePaper;
import java.util.List;

public interface SitePaperService extends IService<SitePaper> {
    List<String> getGalaxies();
    List<SitePaper> getPapersByGalaxy(String galaxy);
}
