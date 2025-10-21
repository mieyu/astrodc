package mie.astronomy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import mie.astronomy.dto.ObservationQueryDto;
import mie.astronomy.entity.Observation;
import mie.astronomy.mapper.ObservationMapper;
import mie.astronomy.service.ObservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ObservationServiceImpl extends ServiceImpl<ObservationMapper, Observation> implements ObservationService {
    @Autowired
    private ObservationMapper observationMapper;

    @Override
    public List<Observation> getAllObservation() {
        return this.list(new LambdaQueryWrapper<>());
    }

    @Override
    public List<Observation> get100Observation() {
        LambdaQueryWrapper<Observation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.last("LIMIT 100");
        return this.list(queryWrapper);
    }

    /**
     * 实现获取下拉选项的方法
     */
    @Override
    public Map<String, List<Object>> getDropdownOptions() {
        List<String> columns = Arrays.asList("object", "imagetyp", "tele", "teleap", "telefl", "filter", "bitpix");

        Map<String, List<Object>> options = columns.parallelStream()
                .collect(Collectors.toMap(
                        column -> column,
                        this::findDistinctValuesForColumn
                ));

        //单独调用方法获取 naxis 的组合选项
        options.put("naxis", (List)getNaxisOptions());
        return options;
    }

    /**
     * 为 naxis1/naxis2 组合查询不重复值
     * @return 格式为 "naxis1Value x naxis2Value" 的字符串列表
     */
    public List<String> getNaxisOptions() {
        QueryWrapper<Observation> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT naxis1, naxis2").isNotNull("naxis1").isNotNull("naxis2");
        List<Map<String, Object>> maps = this.listMaps(queryWrapper);
        return maps.stream()
                .map(map -> map.get("naxis1").toString() + "x" + map.get("naxis2").toString())
                .distinct() // 确保组合后的值也是唯一的
                .sorted()   // 对结果进行排序
                .collect(Collectors.toList());
    }

    private List<Object> findDistinctValuesForColumn(String columnName) {
        QueryWrapper<Observation> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT " + columnName);
        queryWrapper.isNotNull(columnName).ne(columnName, "");
        return this.listObjs(queryWrapper);
    }

    @Override
    public IPage<Observation> search(ObservationQueryDto query, long pageNum, long pageSize, String sortField, String sortOrder) {
        IPage<Observation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Observation> wrapper = new LambdaQueryWrapper<>();

        // 调用一个统一的、完整的方法来构建查询条件
        buildQueryConditions(wrapper, query);

        if (StringUtils.isNotBlank(sortField) && StringUtils.isNotBlank(sortOrder)) {
            SFunction<Observation, ?> columnGetter = getColumnGetter(sortField);
            if (columnGetter != null) {
                boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
                wrapper.orderBy(true, isAsc, columnGetter);
            }
        }

        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<Observation> listByQuery(ObservationQueryDto query) {
        LambdaQueryWrapper<Observation> wrapper = new LambdaQueryWrapper<>();
        // 同样调用统一的方法，确保下载功能和搜索功能的查询逻辑一致
        buildQueryConditions(wrapper, query);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 提取并统一所有查询条件的构建逻辑
     */
    private void buildQueryConditions(LambdaQueryWrapper<Observation> wrapper, ObservationQueryDto query) {
        // 精确匹配
        wrapper.like(StringUtils.isNotBlank(query.getPwd()), Observation::getPwd, query.getPwd());
        wrapper.like(StringUtils.isNotBlank(query.getFitName()), Observation::getFitName, query.getFitName());
        wrapper.eq(query.getSimple() != null, Observation::getSimple, query.getSimple());
        wrapper.eq(StringUtils.isNotBlank(query.getObject()), Observation::getObject, query.getObject());
        wrapper.eq(StringUtils.isNotBlank(query.getImagetyp()), Observation::getImagetyp, query.getImagetyp());
        wrapper.eq(query.getExptime() != null, Observation::getExptime, query.getExptime());
        wrapper.eq(query.getOtcd() != null, Observation::getOtcd, query.getOtcd());
        wrapper.eq(StringUtils.isNotBlank(query.getTele()), Observation::getTele, query.getTele());
        wrapper.eq(query.getTeleap() != null, Observation::getTeleap, query.getTeleap());
        wrapper.eq(query.getTelefl() != null, Observation::getTelefl, query.getTelefl());
        wrapper.eq(StringUtils.isNotBlank(query.getFilter()), Observation::getFilter, query.getFilter());
        wrapper.eq(query.getFlipx() != null, Observation::getFlipx, query.getFlipx());
        wrapper.eq(query.getFlipy() != null, Observation::getFlipy, query.getFlipy());
        wrapper.eq(query.getRotCode() != null, Observation::getRotCode, query.getRotCode());
        wrapper.eq(query.getRCenter() != null, Observation::getRCenter, query.getRCenter());
        wrapper.eq(query.getDCenter() != null, Observation::getDCenter, query.getDCenter());
       

        // 对 naxis 字段的查询逻辑
        if (StringUtils.isNotBlank(query.getNaxis())) {
            String[] naxisValues = query.getNaxis().split("x");
            if (naxisValues.length == 2) {
                try {
                    wrapper.eq(Observation::getNaxis1, Integer.parseInt(naxisValues[0].trim()));
                    wrapper.eq(Observation::getNaxis2, Integer.parseInt(naxisValues[1].trim()));
                } catch (NumberFormatException e) {
                    // 如果格式不正确，则忽略此条件
                }
            }
        }

        // 范围匹配
        wrapper.ge(query.getBitpixMin() != null, Observation::getBitpix, query.getBitpixMin());
        wrapper.le(query.getBitpixMax() != null, Observation::getBitpix, query.getBitpixMax());
        wrapper.ge(query.getNaxisMin() != null, Observation::getNaxis, query.getNaxisMin());
        wrapper.le(query.getNaxisMax() != null, Observation::getNaxis, query.getNaxisMax());
        wrapper.ge(query.getNaxis1Min() != null, Observation::getNaxis1, query.getNaxis1Min());
        wrapper.le(query.getNaxis1Max() != null, Observation::getNaxis1, query.getNaxis1Max());
        wrapper.ge(query.getNaxis2Min() != null, Observation::getNaxis2, query.getNaxis2Min());
        wrapper.le(query.getNaxis2Max() != null, Observation::getNaxis2, query.getNaxis2Max());
        wrapper.ge(query.getBscaleMin() != null, Observation::getBscale, query.getBscaleMin());
        wrapper.le(query.getBscaleMax() != null, Observation::getBscale, query.getBscaleMax());
        wrapper.ge(query.getBzeroMin() != null, Observation::getBzero, query.getBzeroMin());
        wrapper.le(query.getBzeroMax() != null, Observation::getBzero, query.getBzeroMax());
        wrapper.ge(query.getDateObsMin() != null, Observation::getDateObs, query.getDateObsMin());
        wrapper.le(query.getDateObsMax() != null, Observation::getDateObs, query.getDateObsMax());
        wrapper.ge(query.getXpixszMin() != null, Observation::getXpixsz, query.getXpixszMin());
        wrapper.le(query.getXpixszMax() != null, Observation::getXpixsz, query.getXpixszMax());
        wrapper.ge(query.getYpixszMin() != null, Observation::getYpixsz, query.getYpixszMin());
        wrapper.le(query.getYpixszMax() != null, Observation::getYpixsz, query.getYpixszMax());
        wrapper.ge(query.getXbinningMin() != null, Observation::getXbinning, query.getXbinningMin());
        wrapper.le(query.getXbinningMax() != null, Observation::getXbinning, query.getXbinningMax());
        wrapper.ge(query.getYbinningMin() != null, Observation::getYbinning, query.getYbinningMin());
        wrapper.le(query.getYbinningMax() != null, Observation::getYbinning, query.getYbinningMax());
        wrapper.ge(query.getRtAngleMin() != null, Observation::getRtAngle, query.getRtAngleMin());
        wrapper.le(query.getRtAngleMax() != null, Observation::getRtAngle, query.getRtAngleMax());
    }

    private SFunction<Observation, ?> getColumnGetter(String fieldName) {
        return switch (fieldName) {
            case "pwd" -> Observation::getPwd;
            case "fitName" -> Observation::getFitName;
            case "simple" -> Observation::getSimple;
            case "bitpix" -> Observation::getBitpix;
            case "naxis" -> Observation::getNaxis;
            case "naxis1" -> Observation::getNaxis1;
            case "naxis2" -> Observation::getNaxis2;
            case "bscale" -> Observation::getBscale;
            case "bzero" -> Observation::getBzero;
            case "object" -> Observation::getObject;
            case "imagetyp" -> Observation::getImagetyp;
            case "dateObs" -> Observation::getDateObs;
            case "exptime" -> Observation::getExptime;
            case "otcd" -> Observation::getOtcd;
            case "tele" -> Observation::getTele;
            case "teleap" -> Observation::getTeleap;
            case "telefl" -> Observation::getTelefl;
            case "rCenter" -> Observation::getRCenter;
            case "dCenter" -> Observation::getDCenter;
            case "xpixsz" -> Observation::getXpixsz;
            case "ypixsz" -> Observation::getYpixsz;
            case "xbinning" -> Observation::getXbinning;
            case "ybinning" -> Observation::getYbinning;
            case "filter" -> Observation::getFilter;
            case "rtAngle" -> Observation::getRtAngle;
            case "flipx" -> Observation::getFlipx;
            case "flipy" -> Observation::getFlipy;
            case "rotCode" -> Observation::getRotCode;
            default -> null;
        };
    }

    @Override
    public boolean updateObservation(Observation observation) {
        return this.updateById(observation);
    }

    @Override
    public Map<String, Long> getImageTypeStats() {
        List<Observation> list = this.list(new LambdaQueryWrapper<Observation>()
                .isNotNull(Observation::getImagetyp)
                .ne(Observation::getImagetyp, ""));
        
        return list.stream()
                .collect(Collectors.groupingBy(
                        Observation::getImagetyp,
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getObjectStats() {
        List<Observation> list = this.list(new LambdaQueryWrapper<Observation>()
                .isNotNull(Observation::getObject)
                .ne(Observation::getObject, ""));
        
        return list.stream()
                .collect(Collectors.groupingBy(
                        Observation::getObject,
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getYearStats() {
        List<Observation> list = this.list(new LambdaQueryWrapper<Observation>()
                .isNotNull(Observation::getDateObs));
        
        return list.stream()
                .filter(obs -> obs.getDateObs() != null)
                .collect(Collectors.groupingBy(
                        obs -> {
                            String dateStr = obs.getDateObs().toString();
                            return dateStr.substring(0, 4); // 提取年份
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
        List<Observation> list = this.list(new LambdaQueryWrapper<Observation>()
                .isNotNull(Observation::getDateObs));
        
        Map<String, Long> monthMap = list.stream()
                .filter(obs -> obs.getDateObs() != null)
                .collect(Collectors.groupingBy(
                        obs -> {
                            String dateStr = obs.getDateObs().toString();
                            if (dateStr.length() >= 7) {
                                return dateStr.substring(5, 7); // 提取月份
                            }
                            return "00";
                        },
                        Collectors.counting()
                ));
        
        // 转换为 "1月"、"2月" 格式并排序
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