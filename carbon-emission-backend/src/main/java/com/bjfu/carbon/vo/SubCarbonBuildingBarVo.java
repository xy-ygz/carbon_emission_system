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
public class SubCarbonBuildingBarVo implements Serializable {
    /**
     * 教学楼宇(如学研/5号楼等)
     */
    private String building;
    /**
     * 代表该年月的碳排放量
     */
    private Double amount;
}
