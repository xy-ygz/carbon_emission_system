package com.bjfu.carbon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class CarbonBuildingBarVo implements Serializable {

    private static final long serialVersionUID = 1L;



    /**
     * 代表该楼宇排放的年份
     */
    private Integer year;
    /**
     * 代表该楼宇排放的月份
     */
    private Integer month;
    /**
     * 代表相关楼宇的排放值
     */
    private List<SubCarbonBuildingBarVo> subCarbonBuildingBarVos;



}

