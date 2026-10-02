package mie.astronomy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import mie.astronomy.dto.PositioningNormalizedQuery;
import mie.astronomy.entity.PositioningNormalized;

import java.io.OutputStream;
import java.util.List;

public interface PositioningNormalizedService {

    IPage<PositioningNormalized> search(PositioningNormalizedQuery query,
                                        long page, long pageSize,
                                        String sortField, String sortOrder);

    List<String> options(String field);

    /**
     * 流式 CSV 导出。columns 为空时导出全部列;非空时按列出顺序输出。
     */
    void exportCsv(PositioningNormalizedQuery query, OutputStream out);
}
