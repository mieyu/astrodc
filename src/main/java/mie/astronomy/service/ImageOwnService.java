package mie.astronomy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import mie.astronomy.dto.ImageOwnQueryDto;
import mie.astronomy.entity.ImageOwn;

import java.util.List;
import java.util.Map;

public interface ImageOwnService extends IService<ImageOwn> {
    List<ImageOwn> getAllImageOwn();

    List<ImageOwn> get100ImageOwn();

    Map<String, List<Object>> getDropdownOptions();

    List<ImageOwn> listByQuery(ImageOwnQueryDto query);

    IPage<ImageOwn> search(ImageOwnQueryDto query, long pageNum, long pageSize, String sortField, String sortOrder);

    boolean updateImageOwn(ImageOwn imageOwn);

    List<String> getNaxisOptions();

    Map<String, Long> getImageTypeStats();

    Map<String, Long> getObjectStats();

    Map<String, Long> getYearStats();

    Map<String, Long> getMonthStats();
}
