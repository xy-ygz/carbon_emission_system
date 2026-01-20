package com.bjfu.carbon.mapper;

import com.bjfu.carbon.domain.PlaceInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bjfu.carbon.vo.PlaceEmissionVo;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Entity com.bjfu.carbon.domain.PlaceInfo
 */
public interface PlaceInfoMapper extends BaseMapper<PlaceInfo> {
    @Select("SELECT c.category, c.consumption, c.amount, c.year as emission_year, c.month, c.place as emission_building, b.area as building_area\n" +
            "FROM carbon_emission c\n" +
            "         JOIN place_info b\n" +
            "              ON c.place = b.name\n" +
            "WHERE c.category = '电力'")
    List<PlaceEmissionVo> selectEmission();
}




