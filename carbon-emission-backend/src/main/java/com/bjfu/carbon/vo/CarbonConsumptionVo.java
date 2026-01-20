package com.bjfu.carbon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarbonConsumptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 物品分类(如电力/热力/食物等)
     */
    private String category;

    /**
     * 分类对应消耗量
     */
    private Double consumptionCount;

}

