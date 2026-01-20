package com.bjfu.carbon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class EmissionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 物品分类(如电力/热力/食物等)
     */
    private String objectCategory;

    /**
     * 分类对应总碳排放量
     */
    private Double emissionAmount;
}

