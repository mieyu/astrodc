package mie.astronomy.service;

import mie.astronomy.dto.FileItemDto;
import org.springframework.core.io.Resource;
import java.nio.file.Path;
import java.util.List;

public interface FileService {
    /**
     * 根据相对路径列出文件和目录
     * @param subPath 子路径
     * @return 文件和目录列表
     */
    List<FileItemDto> listFiles(String subPath);

    /**
     * 加载文件资源用于下载
     * @param filePath 文件路径
     * @return 文件资源
     */
    Resource loadFileAsResource(String filePath);

    /**
     * 将路径解析为服务器上的绝对路径
     * @param filePath 文件路径
     * @return Path 对象
     */
    Path resolvePath(String filePath);
}