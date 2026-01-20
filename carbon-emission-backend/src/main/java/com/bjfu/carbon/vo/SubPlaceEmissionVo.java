package com.bjfu.carbon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Describe:
 * Author：Zhang chengliang
 * Time：2023/5/2
 */
@Data
@AllArgsConstructor
public class SubPlaceEmissionVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 地点名称
     */
    private String placeName;
    /**
     * 消耗量
     */
    private double powerConsumption;
    /**
     * 该月的碳排放量
     */
    private double emissionAmount;
    /**
     * 单位面积排放量
     */
    private double placePerArea;
}
