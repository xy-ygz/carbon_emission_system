package com.bjfu.carbon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class EmissionMulberryVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer emissionType;
    private Double emissionAmount;
}
