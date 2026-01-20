package com.bjfu.carbon.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;
import lombok.NonNull;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 
 * @TableName exchange_setting
 */
@TableName(value ="exchange_setting")
@Data
public class ExchangeSetting implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 物品分类
     */
    @NotNull(message = "物品分类为空")
    private String objectCategory;

    /**
     * 转化系数
     */
    @NotNull(message = "转化系数为空")
    private Double exchangeCoefficient;

    /**
     * 种类单位
     */
    @NotNull(message = "转化系数单位为空")
    private String unit;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        ExchangeSetting other = (ExchangeSetting) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getObjectCategory() == null ? other.getObjectCategory() == null : this.getObjectCategory().equals(other.getObjectCategory()))
            && (this.getExchangeCoefficient() == null ? other.getExchangeCoefficient() == null : this.getExchangeCoefficient().equals(other.getExchangeCoefficient()))
            && (this.getUnit() == null ? other.getUnit() == null : this.getUnit().equals(other.getUnit()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getObjectCategory() == null) ? 0 : getObjectCategory().hashCode());
        result = prime * result + ((getExchangeCoefficient() == null) ? 0 : getExchangeCoefficient().hashCode());
        result = prime * result + ((getUnit() == null) ? 0 : getUnit().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", objectCategory=").append(objectCategory);
        sb.append(", exchangeCoefficient=").append(exchangeCoefficient);
        sb.append(", unit=").append(unit);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}