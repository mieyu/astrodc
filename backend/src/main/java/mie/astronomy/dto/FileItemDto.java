package mie.astronomy.dto;

import lombok.Data;

@Data
public class FileItemDto {
    private String name;    // 文件或文件夹名
    private String type;    // 'file' 或 'directory'
    private long size;      // 文件大小（字节）
    private String path;    // 相对路径
}