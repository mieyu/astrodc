package mie.astronomy.service;

import java.io.IOException;

public interface FitsService {
    /**
     * 读取 FITS 文件并将其转换为 PNG 图像的字节数组
     * @param filePath FITS文件的相对路径
     * @return PNG 图像的 byte[]
     */
    byte[] fitsToPng(String filePath) throws IOException;
}