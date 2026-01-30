package com.bjfu.carbon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bjfu.carbon.common.Result;
import com.bjfu.carbon.domain.CarbonEmission;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bjfu.carbon.domain.EmissionAndConsume;
import com.bjfu.carbon.vo.CarbonBuildingBarVo;
import com.bjfu.carbon.vo.CarbonBuildingVo;
import com.bjfu.carbon.vo.EmissionCategoryTimePeriodVo;
import com.bjfu.carbon.vo.PlaceEmissionVo;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 *
 */
public interface CarbonEmissionService extends IService<CarbonEmission> {
    /**
     * 通过该接口将得到总碳排放量，人均，地均碳排放量
     */
    Map<String,Double> collegeCarbonEmission(int year, Integer month);

    /**
     * 通过年、月（可为空）筛查所有碳排放记录
     * @param year 年份
     * @param month 月份（可为空）
     * @param buildings 楼宇名称列表（可为空，为空时返回所有楼宇）
     */
    List<CarbonEmission> selectAllCarbonEmission(int year, Integer month, List<String> buildings);

    /**
     * 筛查年月之间的碳排放记录
     */
    List<CarbonEmission> selectAllCarbonEmissionByTimeRange(int startYear, int startMonth, int endYear, int endMonth);

    /**
     * 条件分页查询所有碳排放记录
     * @param current
     * @param size
     * @param name 可为空
     * @param year 可为空
     * @param month 可为空
     * @return
     */
    IPage<CarbonEmission> pageByObjectName(int current, int size, String name, String year, String month);

    Result<Map<String, Object>> getMulberryData(int actualYear, Integer month);

    /**
     * 获取柱状堆叠图数据：指定起始年份-起始月份~终止年份-终止月份对应的每个月份不同物品分类对应的排放量
     */
    Result<List<EmissionCategoryTimePeriodVo>> getEmissionCategoryData(Integer startYear, Integer startMonth, Integer endYear, Integer endMonth);


    Result<Map<String, Object>> getSpeciesConsumptionData(Integer year, Integer month);


    Result<Map<String, Object>> getSpeciesCarbonData(Integer year, Integer month);

    /**
     * 获取楼宇碳排放折线图数据：每年的不同楼宇对应的碳排放
     * @param year 年份
     * @param area 是否计算地均排放（true：地均排放，false：总排放）
     * @param buildings 楼宇名称列表（可为空，为空时返回所有楼宇）
     * @return 楼宇碳排放数据列表
     */
    Result<List<CarbonBuildingVo>> getBuildingCarbonLineData(Integer year, boolean area, List<String> buildings);

    /**
     * 获取楼宇碳排放柱状图数据：指定年份月份的不同楼宇对应的碳排放
     * @param year 年份（可为空，为空时使用实际年份）
     * @param month 月份（可为空，为空时使用当前月份）
     * @param area 是否计算地均排放（true：地均排放，false：总排放）
     * @param buildings 楼宇名称列表（可为空，为空时返回所有楼宇）
     * @return 楼宇碳排放数据列表
     */
    Result<List<CarbonBuildingBarVo>> getBuildingCarbonBarData(Integer year, Integer month, boolean area, List<String> buildings);

    /**
     * 获取楼宇信息数据：不同楼宇的电耗、碳排放量、单位面积排放量
     * @param year 年份（可为空，为空时使用实际年份）
     * @param month 月份（可为空，为空时使用当前月份）
     * @param buildings 楼宇名称列表（可为空，为空时返回所有楼宇）
     * @return 楼宇信息数据列表
     */
    Result<List<PlaceEmissionVo>> getBuildingInfoData(Integer year, Integer month, List<String> buildings);

    /**
     * 通过该接口得到各种碳排放值和物品消耗值
     * @param year
     * @return
     */
    List<EmissionAndConsume> selectEmiAndConsume(int year);

    /**
     * 通过该接口得到一年中每个月份的不同的二氧化碳消耗值
     * @param year
     * @return
     */
    List<CarbonEmission> selectEmissionType(int year);

    /**
     * 添加碳排放数据
     * @param carbonEmission 碳排放数据
     * @return 操作结果
     */
    Result<String> addCarbonEmission(CarbonEmission carbonEmission);

    /**
     * 更新碳排放数据
     * @param carbonEmission 碳排放数据
     * @return 操作结果
     */
    Result<String> updateCarbonEmission(CarbonEmission carbonEmission);

    /**
     * 根据ID删除碳排放数据
     * @param carbonEmissionId 碳排放数据ID
     * @return 操作结果
     */
    Result<String> deleteCarbonEmissionById(String carbonEmissionId);

    /**
     * 批量导入碳排放数据（支持多种文件格式：Excel、CSV等）
     * @param fileInputStream 文件输入流
     * @param filename 文件名（用于识别文件格式）
     * @return 操作结果
     */
    Result<String> batchImportFromFile(InputStream fileInputStream, String filename);

    /**
     * 获取年度报告数据（用于导出报告）
     * @param year 年份（可为空，为空时使用实际年份）
     * @return 年度报告数据Map
     */
    Map<String, String> getExportReportDataByYear(Integer year);
}
