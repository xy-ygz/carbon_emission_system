package com.bjfu.carbon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class TimeEmissionVo implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 表示排放量的月份
     */
    private String time;

    /**
     * 表示该月份的碳排放量
     */
    private Double emissionAmount;
}

