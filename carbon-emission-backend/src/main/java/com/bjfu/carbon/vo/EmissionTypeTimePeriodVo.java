package com.bjfu.carbon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class EmissionTypeTimePeriodVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *排放类型（0/1/2:直接排放/间接排放/其他排放）
     */
    private Integer emissionType;
    /**
     * 代表该楼宇该年每个月的排放总额
     */
    private List<TimeEmissionVo> emissionMonthAmount;
}
