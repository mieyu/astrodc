package mie.astronomy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import mie.astronomy.entity.Paper;
import java.util.List;

public interface PaperService extends IService<Paper> {
    List<String> getGalaxies();
    List<Paper> getPapersByGalaxy(String galaxy);
}