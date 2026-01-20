package com.bjfu.carbon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class EmissionCategoryVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 物品分类(如电力/热力/食物等)
     */
    private String objectCategory;
    private int year;
    /**
     * 代表该楼宇该年每个月的排放总额
     */
    private List<MonthEmissionVo> emissionMonthAmount;
}
