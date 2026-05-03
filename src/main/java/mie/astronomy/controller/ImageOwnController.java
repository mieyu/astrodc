package mie.astronomy.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import mie.astronomy.common.Result;
import mie.astronomy.dto.ImageOwnQueryDto;
import mie.astronomy.entity.ImageOwn;
import mie.astronomy.service.ImageOwnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@CrossOrigin
@RequestMapping("/api/image/own")
public class ImageOwnController {
    @Autowired
    private ImageOwnService imageOwnService;

    @GetMapping("/queryAll")
    public Result queryAll() {
        List<ImageOwn> list = imageOwnService.getAllImageOwn();
        return Result.success(list);
    }

    @GetMapping("/query100")
    public Result query100() {
        List<ImageOwn> list = imageOwnService.get100ImageOwn();
        return Result.success(list);
    }

    @GetMapping("/options")
    public Result<Map<String, List<Object>>> getOptions() {
        Map<String, List<Object>> options = imageOwnService.getDropdownOptions();
        return Result.success(options);
    }

    @PostMapping("/search")
    public Result<IPage<ImageOwn>> search(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            @RequestBody ImageOwnQueryDto query) {
        long size = 100;
        IPage<ImageOwn> pageResult = imageOwnService.search(query, page, size, sortField, sortOrder);
        return Result.success(pageResult);
    }

    @PostMapping("/download")
    public void download(@RequestBody ImageOwnQueryDto query, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=utf-8");
        response.getWriter().write(new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}));
        response.setHeader("Content-Disposition", "attachment; filename=\"image_own.csv\"");

        List<ImageOwn> list = imageOwnService.listByQuery(query);

        try (PrintWriter writer = response.getWriter()) {
            Field[] fields = ImageOwn.class.getDeclaredFields();

            String header = Stream.of(fields).map(Field::getName).collect(Collectors.joining(","));
            writer.println(header);

            for (ImageOwn obs : list) {
                for (int i = 0; i < fields.length; i++) {
                    try {
                        fields[i].setAccessible(true);
                        Object value = fields[i].get(obs);
                        String formattedValue = (value == null) ? "" : "\"" + value.toString().replace("\"", "\"\"") + "\"";
                        writer.print(formattedValue);
                    } catch (IllegalAccessException e) {
                        writer.print("");
                    }
                    if (i < fields.length - 1) {
                        writer.print(",");
                    }
                }
                writer.println();
            }
        }
    }

    @PutMapping("/update")
    public Result<?> update(@RequestBody ImageOwn imageOwn) {
        boolean success = imageOwnService.updateImageOwn(imageOwn);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败，未找到对应记录或数据无变化");
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = imageOwnService.count();
        stats.put("total", total);
        stats.put("imageType", imageOwnService.getImageTypeStats());
        stats.put("object", imageOwnService.getObjectStats());
        stats.put("year", imageOwnService.getYearStats());
        stats.put("month", imageOwnService.getMonthStats());
        return Result.success(stats);
    }
}
