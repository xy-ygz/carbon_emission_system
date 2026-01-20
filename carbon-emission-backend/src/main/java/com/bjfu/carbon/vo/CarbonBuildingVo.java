package com.bjfu.carbon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class CarbonBuildingVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 教学楼宇(如学研/5号楼等)
     */
    private String building;
    /**
     * 排放时间（年）
     */
    private int year;
    /**
     * 代表该楼宇该年每个月的排放总额
     */
    private List<MonthEmissionVo> emissionMonthAmount;
}

