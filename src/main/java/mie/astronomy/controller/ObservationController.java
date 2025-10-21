package mie.astronomy.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import mie.astronomy.common.Result;
import mie.astronomy.dto.ObservationQueryDto;
import mie.astronomy.entity.Observation;
import mie.astronomy.service.ObservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@CrossOrigin
@RequestMapping("/observation")
public class ObservationController {
    @Autowired
    private ObservationService observationService;



    @GetMapping("/queryAll")
    public Result queryAllObservation() {
        List<Observation> observations = observationService.getAllObservation();
        return Result.success(observations);
    }

    @GetMapping("/query100")
    public Result query100Observation() {
        List<Observation> observations = observationService.get100Observation();
        return Result.success(observations);
    }

    /**
     * 用于提供下拉框选项
     * @return 包含所有下拉框选项数据的 Result 对象
     */
    @GetMapping("/options")
    public Result<Map<String, List<Object>>> getOptions() {
        Map<String, List<Object>> options = observationService.getDropdownOptions();
        return Result.success(options);
    }

    /**
     * 新增的搜索接口，使用POST请求接收复杂的查询条件
     * @param page 当前页码
     * @param query 查询条件对象
     * @return 分页的查询结果
     */
    @PostMapping("/search")
    public Result<IPage<Observation>> search(
            @RequestParam(defaultValue = "1") long page, @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder, @RequestBody ObservationQueryDto query)
    {
        long size = 100;
        // 将接收到的排序参数传递给 service
        IPage<Observation> pageResult = observationService.search(query, page, size, sortField, sortOrder);
        return Result.success(pageResult);
    }

    /**
     * 下载查询结果为CSV文件
     */
    @PostMapping("/download")
    public void download(@RequestBody ObservationQueryDto query, HttpServletResponse response) throws IOException {

        // 1. 设置HTTP响应头
        response.setContentType("text/csv;charset=utf-8");
        // 添加BOM头以防止Excel中文乱码
        response.getWriter().write(new String(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }));
        response.setHeader("Content-Disposition", "attachment; filename=\"observations.csv\"");

        // 2. 查询所有符合条件的数据
        List<Observation> list = observationService.listByQuery(query);

        // 3. 将数据写入响应流
        try (PrintWriter writer = response.getWriter()) {
            // 修正：使用反射动态获取所有字段
            Field[] fields = Observation.class.getDeclaredFields();

            // 写入CSV表头
            String header = Stream.of(fields).map(Field::getName).collect(Collectors.joining(","));
            writer.println(header);

            // 写入数据行
            for (Observation obs : list) {
                for (int i = 0; i < fields.length; i++) {
                    try {
                        // 允许访问私有字段
                        fields[i].setAccessible(true);
                        Object value = fields[i].get(obs);
                        // 处理null值和包含逗号的字符串
                        String formattedValue = (value == null) ? "" : "\"" + value.toString().replace("\"", "\"\"") + "\"";
                        writer.print(formattedValue);
                    } catch (IllegalAccessException e) {
                        // 异常处理，写入空值
                        writer.print("");
                    }
                    if (i < fields.length - 1) {
                        writer.print(",");
                    }
                }
                writer.println(); // 每行结束换行
            }
        }
    }



    /**
     * 处理更新观测记录的请求接口
     * @param observation 从请求体接收的、已修改的观测数据
     * @return 操作结果
     */
    @PutMapping("/update")
    public Result<?> updateObservation(@RequestBody Observation observation) {
        boolean success = observationService.updateObservation(observation);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败，未找到对应记录或数据无变化");
        }
    }


    /**
     * 获取数据可视化统计信息
     * @return 包含各种统计数据的 Map
     */
    @GetMapping("/stats")
    public Result<Map<String, Map<String, Long>>> getStats() {
        Map<String, Map<String, Long>> stats = new java.util.HashMap<>();
        stats.put("imageType", observationService.getImageTypeStats());
        stats.put("object", observationService.getObjectStats());
        stats.put("year", observationService.getYearStats());
        stats.put("month", observationService.getMonthStats());
        return Result.success(stats);
    }

}
