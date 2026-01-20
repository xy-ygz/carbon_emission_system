package com.bjfu.carbon.mapper;

import com.bjfu.carbon.domain.CarbonEmission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjfu.carbon.domain.EmissionAndConsume;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Entity com.bjfu.carbon.domain.CarbonEmission
 */
public interface CarbonEmissionMapper extends BaseMapper<CarbonEmission> {
    List<CarbonEmission> pageByObjectName(@Param("begin")int begin, @Param("pageSize")int pageSize, @Param("name")String name, @Param("year")String year, @Param("month")String month);


    int selectTotalCount(@Param("name")String name, @Param("year")String year, @Param("month")String month);
    List<EmissionAndConsume> selectEmiAndConsume(int year);
    List<CarbonEmission> selectEmissionType(int year);
    
    /**
     * 查询指定年份范围内有数据的年份
     * @param startYear 开始年份
     * @param endYear 结束年份
     * @return 有数据的年份列表，按年份降序排列
     */
    List<Integer> selectYearsWithData(@Param("startYear") int startYear, @Param("endYear") int endYear);
    
    /**
     * 计算指定年份和月份的总碳排放量
     * @param year 年份
     * @param month 月份（可选）
     * @return 总碳排放量
     */
    Double selectTotalEmission(@Param("year") int year, @Param("month") Integer month);

    @Select({"<script>",
            "SELECT id, name, category, consumption, purpose, year, month, amount, place, emission_type, created_time, modified_time ",
            "FROM carbon_emission ",
            "WHERE year = #{year} ",
            "<if test='month != null'>",
            "AND month = #{month} ",
            "</if>",
            "</script>"})
    List<CarbonEmission> selectAllCarbonEmission(@Param("year") int year, @Param("month") Integer month);

    @Select("SELECT id, name, category, consumption, purpose, year, month, amount, place, emission_type, created_time, modified_time " +
            "FROM carbon_emission " +
            "WHERE (year > #{startYear} OR (year = #{startYear} AND month >= #{startMonth})) " +
            "AND (year < #{endYear} OR (year = #{endYear} AND month <= #{endMonth})) " +
            "ORDER BY year DESC, month DESC")
    List<CarbonEmission> selectAllCarbonEmissionByTimeRange(@Param("startYear") int startYear, @Param("startMonth") int startMonth, @Param("endYear") int endYear, @Param("endMonth") int endMonth);
}