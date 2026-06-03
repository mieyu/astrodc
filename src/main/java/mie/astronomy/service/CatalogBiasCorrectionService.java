package mie.astronomy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import mie.astronomy.dto.CatalogBiasCorrectionQuery;

import java.io.OutputStream;
import java.util.Map;

public interface CatalogBiasCorrectionService {

    IPage<Map<String, Object>> search(CatalogBiasCorrectionQuery query,
                                      long page, long pageSize,
                                      String sortField, String sortOrder);

    Map<String, Object> columns();

    void exportCsv(CatalogBiasCorrectionQuery query, OutputStream out);
}
