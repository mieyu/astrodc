package mie.astronomy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import mie.astronomy.dto.ObservationQueryDto;
import mie.astronomy.entity.Observation;

import java.util.List;
import java.util.Map;

public interface ObservationService extends IService<Observation> {
    List<Observation> getAllObservation();

    List<Observation> get100Observation();

    /**
     * 获取用于前端下拉选择框的选项数据
     * @return 一个 Map，键是字段名 (e.g., "object", "imagetyp"), 值是该字段不重复值的列表
     */
    Map<String, List<Object>> getDropdownOptions();


    /**
     * 新增：根据查询条件获取所有匹配的数据（不分页），用于下载
     * @param query 查询条件
     * @return 匹配的所有 Observation 记录列表
     */
    List<Observation> listByQuery(ObservationQueryDto query);

    /**
     * @param query 查询条件
     * @param pageNum 当前页码
     * @param pageSize 每页数量
     * @param sortField 排序字段
     * @param sortOrder 排序顺序 ('asc' 或 'desc')
     * @return 分页和排序后的结果
     */
    IPage<Observation> search(ObservationQueryDto query, long pageNum, long pageSize, String sortField, String sortOrder);

    /**
     * 更新观测记录
     * @param observation 包含更新信息的实体对象
     * @return 如果更新成功，返回 true
     */
    boolean updateObservation(Observation observation);

    /**
     * 获取用于前端下拉选择框的 naxis 选项数据
     * @return 一个 Map，键是字段名 (e.g., "naxis"), 值是该字段不重复值的列表
     */
    List<String> getNaxisOptions();

    
    /**
     * 获取图像类型统计数据
     * @return 图像类型及其数量的映射
     */
    Map<String, Long> getImageTypeStats();

    /**
     * 获取观测目标统计数据
     * @return 观测目标及其数量的映射
     */
    Map<String, Long> getObjectStats();

    /**
     * 获取年份统计数据
     * @return 年份及其数量的映射
     */
    Map<String, Long> getYearStats();

    /**
     * 获取月份统计数据
     * @return 月份及其数量的映射
     */
    Map<String, Long> getMonthStats();
}
