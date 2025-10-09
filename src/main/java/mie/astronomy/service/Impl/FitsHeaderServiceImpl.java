package mie.astronomy.service.Impl;

import mie.astronomy.dto.FitsHeaderCard;
import mie.astronomy.service.FitsHeaderService;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class FitsHeaderServiceImpl implements FitsHeaderService {

    @Override
    public List<FitsHeaderCard> parseHeader(MultipartFile file) {
        List<FitsHeaderCard> headerData = new ArrayList<>();
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        try (InputStream is = file.getInputStream()) {
            Fits fits = new Fits(is);
            Header header = fits.getHDU(0).getHeader(); // 获取主HDU的头文件

            Cursor<String, HeaderCard> iterator = header.iterator();
            while (iterator.hasNext()) {
                HeaderCard card = iterator.next();
                headerData.add(new FitsHeaderCard(card.getKey(), card.getValue(), card.getComment()));
            }
            fits.close();
        } catch (Exception e) {
            throw new RuntimeException("解析 FITS 头文件失败: " + e.getMessage(), e);
        }
        return headerData;
    }
}