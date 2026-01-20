package com.bjfu.carbon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Describe:
 * Author：Zhang chengliang
 * Time：2023/4/26
 */
@Data
@AllArgsConstructor
public class PlaceEmissionVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 排放时间（年）
     */
    private Integer emissionYear;
    /**
     * 排放时间（月）
     */
    private Integer month;
    /**
     * 排放的相关值（电耗，人均）
     */
    private List<SubPlaceEmissionVo> subPlaceEmissionVos;

}
