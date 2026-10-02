package mie.astronomy.service;

import mie.astronomy.dto.EphemerisCalculateRequest;
import mie.astronomy.dto.EphemerisChartRequest;

import java.util.Map;

public interface EphemerisService {
    /**
     * 返回 {success:true, data:[...]} 或 {success:false, message:"..."}
     */
    Map<String, Object> calculate(EphemerisCalculateRequest req);

    /**
     * 返回 {success:true, download_url:"...", filename:"..."} 或 {success:false, message:"..."}
     */
    Map<String, Object> downloadChart(EphemerisChartRequest req);

    /**
     * 寻星图缓存目录绝对路径(白名单)。/get_file 必须校验请求路径在该目录下。
     */
    String getChartCacheDir();
}
