package com.bjfu.carbon.domain;

import lombok.Data;

/**
 * Describe:
 * Author：Zhang chengliang
 * Time：2023/9/23
 */
@Data
public class EmissionAndConsume {
    private String objectCategory;
    private Double objectConsumption;
    private Double emissionAmount;
    private byte emissionType;
    private String unit;
    private String objectCategoryMax;
    private String objectCategoryMin;
    private int emissionMonthMax;
    private int emissionMonthMin;
    private Double emiMaxNumber;
    private Double emiMinNumber;

}
