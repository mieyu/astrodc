package mie.astronomy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import mie.astronomy.dto.ImageOwnQueryDto;
import mie.astronomy.entity.ImageOwn;
import mie.astronomy.mapper.ImageOwnMapper;
import mie.astronomy.service.ImageOwnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageOwnServiceImpl extends ServiceImpl<ImageOwnMapper, ImageOwn> implements ImageOwnService {
    @Autowired
    private ImageOwnMapper imageOwnMapper;

    @Override
    public List<ImageOwn> getAllImageOwn() {
        return this.list(new LambdaQueryWrapper<>());
    }

    @Override
    public List<ImageOwn> get100ImageOwn() {
        LambdaQueryWrapper<ImageOwn> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.last("LIMIT 100");
        return this.list(queryWrapper);
    }

    @Override
    public Map<String, List<Object>> getDropdownOptions() {
        List<String> columns = Arrays.asList("object", "imagetyp", "tele", "teleap", "telefl", "filter", "bitpix");

        Map<String, List<Object>> options = columns.parallelStream()
                .collect(Collectors.toMap(
                        column -> column,
                        this::findDistinctValuesForColumn
                ));

        options.put("naxis", (List) getNaxisOptions());
        return options;
    }

    @Override
    public List<String> getNaxisOptions() {
        QueryWrapper<ImageOwn> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT naxis1, naxis2").isNotNull("naxis1").isNotNull("naxis2");
        List<Map<String, Object>> maps = this.listMaps(queryWrapper);
        return maps.stream()
                .map(map -> map.get("naxis1").toString() + "x" + map.get("naxis2").toString())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private List<Object> findDistinctValuesForColumn(String columnName) {
        QueryWrapper<ImageOwn> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT " + columnName);
        queryWrapper.isNotNull(columnName).ne(columnName, "");
        return this.listObjs(queryWrapper);
    }

    @Override
    public IPage<ImageOwn> search(ImageOwnQueryDto query, long pageNum, long pageSize, String sortField, String sortOrder) {
        IPage<ImageOwn> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ImageOwn> wrapper = new LambdaQueryWrapper<>();

        buildQueryConditions(wrapper, query);

        if (StringUtils.isNotBlank(sortField) && StringUtils.isNotBlank(sortOrder)) {
            SFunction<ImageOwn, ?> columnGetter = getColumnGetter(sortField);
            if (columnGetter != null) {
                boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
                wrapper.orderBy(true, isAsc, columnGetter);
            }
        }

        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<ImageOwn> listByQuery(ImageOwnQueryDto query) {
        LambdaQueryWrapper<ImageOwn> wrapper = new LambdaQueryWrapper<>();
        buildQueryConditions(wrapper, query);
        return baseMapper.selectList(wrapper);
    }

    private void buildQueryConditions(LambdaQueryWrapper<ImageOwn> wrapper, ImageOwnQueryDto query) {
        wrapper.like(StringUtils.isNotBlank(query.getPwd()), ImageOwn::getPwd, query.getPwd());
        wrapper.like(StringUtils.isNotBlank(query.getFitName()), ImageOwn::getFitName, query.getFitName());
        wrapper.eq(query.getSimple() != null, ImageOwn::getSimple, query.getSimple());
        wrapper.eq(StringUtils.isNotBlank(query.getObject()), ImageOwn::getObject, query.getObject());
        wrapper.eq(StringUtils.isNotBlank(query.getImagetyp()), ImageOwn::getImagetyp, query.getImagetyp());
        wrapper.eq(query.getExptime() != null, ImageOwn::getExptime, query.getExptime());
        wrapper.eq(query.getOtcd() != null, ImageOwn::getOtcd, query.getOtcd());
        wrapper.eq(StringUtils.isNotBlank(query.getTele()), ImageOwn::getTele, query.getTele());
        wrapper.eq(query.getTeleap() != null, ImageOwn::getTeleap, query.getTeleap());
        wrapper.eq(query.getTelefl() != null, ImageOwn::getTelefl, query.getTelefl());
        wrapper.eq(StringUtils.isNotBlank(query.getFilter()), ImageOwn::getFilter, query.getFilter());
        wrapper.eq(query.getFlipx() != null, ImageOwn::getFlipx, query.getFlipx());
        wrapper.eq(query.getFlipy() != null, ImageOwn::getFlipy, query.getFlipy());
        wrapper.eq(query.getRotCode() != null, ImageOwn::getRotCode, query.getRotCode());
        wrapper.eq(query.getRCenter() != null, ImageOwn::getRCenter, query.getRCenter());
        wrapper.eq(query.getDCenter() != null, ImageOwn::getDCenter, query.getDCenter());

        if (StringUtils.isNotBlank(query.getNaxis())) {
            String[] naxisValues = query.getNaxis().split("x");
            if (naxisValues.length == 2) {
                try {
                    wrapper.eq(ImageOwn::getNaxis1, Integer.parseInt(naxisValues[0].trim()));
                    wrapper.eq(ImageOwn::getNaxis2, Integer.parseInt(naxisValues[1].trim()));
                } catch (NumberFormatException e) {
                    // ignore malformed input
                }
            }
        }

        wrapper.ge(query.getBitpixMin() != null, ImageOwn::getBitpix, query.getBitpixMin());
        wrapper.le(query.getBitpixMax() != null, ImageOwn::getBitpix, query.getBitpixMax());
        wrapper.ge(query.getNaxisMin() != null, ImageOwn::getNaxis, query.getNaxisMin());
        wrapper.le(query.getNaxisMax() != null, ImageOwn::getNaxis, query.getNaxisMax());
        wrapper.ge(query.getNaxis1Min() != null, ImageOwn::getNaxis1, query.getNaxis1Min());
        wrapper.le(query.getNaxis1Max() != null, ImageOwn::getNaxis1, query.getNaxis1Max());
        wrapper.ge(query.getNaxis2Min() != null, ImageOwn::getNaxis2, query.getNaxis2Min());
        wrapper.le(query.getNaxis2Max() != null, ImageOwn::getNaxis2, query.getNaxis2Max());
        wrapper.ge(query.getBscaleMin() != null, ImageOwn::getBscale, query.getBscaleMin());
        wrapper.le(query.getBscaleMax() != null, ImageOwn::getBscale, query.getBscaleMax());
        wrapper.ge(query.getBzeroMin() != null, ImageOwn::getBzero, query.getBzeroMin());
        wrapper.le(query.getBzeroMax() != null, ImageOwn::getBzero, query.getBzeroMax());
        wrapper.ge(query.getDateObsMin() != null, ImageOwn::getDateObs, query.getDateObsMin());
        wrapper.le(query.getDateObsMax() != null, ImageOwn::getDateObs, query.getDateObsMax());
        wrapper.ge(query.getXpixszMin() != null, ImageOwn::getXpixsz, query.getXpixszMin());
        wrapper.le(query.getXpixszMax() != null, ImageOwn::getXpixsz, query.getXpixszMax());
        wrapper.ge(query.getYpixszMin() != null, ImageOwn::getYpixsz, query.getYpixszMin());
        wrapper.le(query.getYpixszMax() != null, ImageOwn::getYpixsz, query.getYpixszMax());
        wrapper.ge(query.getXbinningMin() != null, ImageOwn::getXbinning, query.getXbinningMin());
        wrapper.le(query.getXbinningMax() != null, ImageOwn::getXbinning, query.getXbinningMax());
        wrapper.ge(query.getYbinningMin() != null, ImageOwn::getYbinning, query.getYbinningMin());
        wrapper.le(query.getYbinningMax() != null, ImageOwn::getYbinning, query.getYbinningMax());
        wrapper.ge(query.getRtAngleMin() != null, ImageOwn::getRtAngle, query.getRtAngleMin());
        wrapper.le(query.getRtAngleMax() != null, ImageOwn::getRtAngle, query.getRtAngleMax());
    }

    private SFunction<ImageOwn, ?> getColumnGetter(String fieldName) {
        return switch (fieldName) {
            case "pwd" -> ImageOwn::getPwd;
            case "fitName" -> ImageOwn::getFitName;
            case "simple" -> ImageOwn::getSimple;
            case "bitpix" -> ImageOwn::getBitpix;
            case "naxis" -> ImageOwn::getNaxis;
            case "naxis1" -> ImageOwn::getNaxis1;
            case "naxis2" -> ImageOwn::getNaxis2;
            case "bscale" -> ImageOwn::getBscale;
            case "bzero" -> ImageOwn::getBzero;
            case "object" -> ImageOwn::getObject;
            case "imagetyp" -> ImageOwn::getImagetyp;
            case "dateObs" -> ImageOwn::getDateObs;
            case "exptime" -> ImageOwn::getExptime;
            case "otcd" -> ImageOwn::getOtcd;
            case "tele" -> ImageOwn::getTele;
            case "teleap" -> ImageOwn::getTeleap;
            case "telefl" -> ImageOwn::getTelefl;
            case "rCenter" -> ImageOwn::getRCenter;
            case "dCenter" -> ImageOwn::getDCenter;
            case "xpixsz" -> ImageOwn::getXpixsz;
            case "ypixsz" -> ImageOwn::getYpixsz;
            case "xbinning" -> ImageOwn::getXbinning;
            case "ybinning" -> ImageOwn::getYbinning;
            case "filter" -> ImageOwn::getFilter;
            case "rtAngle" -> ImageOwn::getRtAngle;
            case "flipx" -> ImageOwn::getFlipx;
            case "flipy" -> ImageOwn::getFlipy;
            case "rotCode" -> ImageOwn::getRotCode;
            default -> null;
        };
    }

    @Override
    public boolean updateImageOwn(ImageOwn imageOwn) {
        return false;
    }

    @Override
    public Map<String, Long> getImageTypeStats() {
        List<ImageOwn> list = this.list(new LambdaQueryWrapper<ImageOwn>()
                .isNotNull(ImageOwn::getImagetyp)
                .ne(ImageOwn::getImagetyp, ""));

        return list.stream()
                .collect(Collectors.groupingBy(
                        ImageOwn::getImagetyp,
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getObjectStats() {
        List<ImageOwn> list = this.list(new LambdaQueryWrapper<ImageOwn>()
                .isNotNull(ImageOwn::getObject)
                .ne(ImageOwn::getObject, ""));

        return list.stream()
                .collect(Collectors.groupingBy(
                        ImageOwn::getObject,
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getYearStats() {
        List<ImageOwn> list = this.list(new LambdaQueryWrapper<ImageOwn>()
                .isNotNull(ImageOwn::getDateObs));

        return list.stream()
                .filter(obs -> obs.getDateObs() != null)
                .collect(Collectors.groupingBy(
                        obs -> {
                            String dateStr = obs.getDateObs().toString();
                            return dateStr.substring(0, 4);
                        },
                        java.util.LinkedHashMap::new,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        java.util.LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Long> getMonthStats() {
        List<ImageOwn> list = this.list(new LambdaQueryWrapper<ImageOwn>()
                .isNotNull(ImageOwn::getDateObs));

        Map<String, Long> monthMap = list.stream()
                .filter(obs -> obs.getDateObs() != null)
                .collect(Collectors.groupingBy(
                        obs -> {
                            String dateStr = obs.getDateObs().toString();
                            if (dateStr.length() >= 7) {
                                return dateStr.substring(5, 7);
                            }
                            return "00";
                        },
                        Collectors.counting()
                ));

        return monthMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        entry -> {
                            int month = Integer.parseInt(entry.getKey());
                            return month + "月";
                        },
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        java.util.LinkedHashMap::new
                ));
    }
}
