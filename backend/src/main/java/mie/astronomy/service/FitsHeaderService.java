package mie.astronomy.service;

import mie.astronomy.dto.FitsHeaderCard;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface FitsHeaderService {
    /**
     * 解析上传的 FITS 文件并返回其头文件信息
     * @param file 用户上传的 FITS 文件
     * @return 头文件中所有卡片的列表
     */
    List<FitsHeaderCard> parseHeader(MultipartFile file);
}