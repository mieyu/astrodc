package mie.astronomy.service.Impl;

import mie.astronomy.dto.FileItemDto;
import mie.astronomy.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileServiceImpl implements FileService {

    private final Path fileStorageLocation;

    // 从 application.yml 注入配置的根路径
    public FileServiceImpl(@Value("${file.root-path}") String rootPath) {
        this.fileStorageLocation = Paths.get(rootPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("无法创建用于存储文件的目录。", ex);
        }
    }

    @Override
    public List<FileItemDto> listFiles(String subPath) {
        Path currentPath = resolvePath(subPath);

        try (Stream<Path> stream = Files.list(currentPath)) {
            return stream.map(path -> {
                        FileItemDto item = new FileItemDto();
                        item.setName(path.getFileName().toString());
                        item.setPath(fileStorageLocation.relativize(path).toString());
                        if (Files.isDirectory(path)) {
                            item.setType("directory");
                            item.setSize(0);
                        } else {
                            item.setType("file");
                            try {
                                item.setSize(Files.size(path));
                            } catch (IOException e) {
                                item.setSize(-1); // 读取大小失败
                            }
                        }
                        return item;
                    })
                    // 将文件夹排在前面
                    .sorted(Comparator.comparing(FileItemDto::getType).reversed())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("无法读取目录内容！", e);
        }
    }

    @Override
    public Resource loadFileAsResource(String filePath) {
        try {
            Path path = resolvePath(filePath);
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("文件不存在或无法读取: " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("文件路径格式错误: " + filePath, ex);
        }
    }

    @Override
    public Path resolvePath(String filePath) {
        String relativePath = toStorageRelativePath(filePath);
        Path targetPath = this.fileStorageLocation.resolve(relativePath).normalize();
        // 安全性检查：确保解析后的路径仍在根目录下
        if (!targetPath.startsWith(this.fileStorageLocation)) {
            throw new RuntimeException("非法路径访问尝试: " + filePath);
        }
        return targetPath;
    }

    /**
     * 数据库历史数据中保存过 Linux/Windows 绝对路径。线上服务器只需要路径中
     * Observated_images 之后的相对部分，再映射到当前配置的存储根目录。
     */
    private String toStorageRelativePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }

        String normalizedPath = filePath.trim().replace('\\', '/');
        String rootName = this.fileStorageLocation.getFileName().toString();
        String lowerPath = normalizedPath.toLowerCase(Locale.ROOT);
        String lowerRootName = rootName.toLowerCase(Locale.ROOT);
        String rootMarker = "/" + lowerRootName + "/";

        int markerIndex = lowerPath.indexOf(rootMarker);
        if (markerIndex >= 0) {
            return normalizedPath.substring(markerIndex + rootMarker.length());
        }

        String rootPrefix = lowerRootName + "/";
        if (lowerPath.startsWith(rootPrefix)) {
            return normalizedPath.substring(rootPrefix.length());
        }

        if (lowerPath.equals(lowerRootName) || lowerPath.endsWith("/" + lowerRootName)) {
            return "";
        }

        // 将盘符路径转换成 / 后，在非 Windows 环境中也必须识别并拒绝。
        boolean windowsAbsolutePath = normalizedPath.matches("^[A-Za-z]:/.*");
        if (Paths.get(filePath).isAbsolute() || windowsAbsolutePath) {
            throw new RuntimeException("非法路径访问尝试: " + filePath);
        }

        return normalizedPath;
    }
}
