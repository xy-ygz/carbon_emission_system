package com.bjfu.carbon.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotNull;

/**
 *
 * @TableName carbon_emission
 */
@TableName(value ="carbon_emission")
@Data
public class CarbonEmission implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 本次排放录入的名称
     */
    @NotNull(message = "名称不能为空")
    private String name;

    /**
     * 分类(如电力/热力/食物等)
     */
    @NotNull(message = "分类不能为空")
    private String category;

    /**
     * 消耗量
     */
    @NotNull(message = "消耗量不能为空")
    private Double consumption;

    /**
     * 用途
     */
    private String purpose;

    /**
     * 年份
     */
    @NotNull(message = "年份不能为空")
    private Integer year;

    /**
     * 月份
     */
    @NotNull(message = "月份不能为空")
    private Integer month;

    /**
     * 碳排放量（根据消耗量和转化系数自动计算：consumption * exchangeCoefficient）
     * 转化系数从 exchange_setting 表根据 category 查询获取
     */
    private Double amount;

    /**
     * 排放地点（楼宇）
     */
    private String place;

    /**
     * 排放类型（0/1/2:直接排放/间接排放/其他排放）
     * 保留此字段以兼容现有业务逻辑
     */
    @NotNull(message = "排放类型不能为空")
    private Integer emissionType;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 修改时间
     */
    private Date modifiedTime;

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
        CarbonEmission other = (CarbonEmission) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getCategory() == null ? other.getCategory() == null : this.getCategory().equals(other.getCategory()))
            && (this.getConsumption() == null ? other.getConsumption() == null : this.getConsumption().equals(other.getConsumption()))
            && (this.getPurpose() == null ? other.getPurpose() == null : this.getPurpose().equals(other.getPurpose()))
            && (this.getYear() == null ? other.getYear() == null : this.getYear().equals(other.getYear()))
            && (this.getMonth() == null ? other.getMonth() == null : this.getMonth().equals(other.getMonth()))
            && (this.getAmount() == null ? other.getAmount() == null : this.getAmount().equals(other.getAmount()))
            && (this.getPlace() == null ? other.getPlace() == null : this.getPlace().equals(other.getPlace()))
            && (this.getEmissionType() == null ? other.getEmissionType() == null : this.getEmissionType().equals(other.getEmissionType()))
            && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
            && (this.getModifiedTime() == null ? other.getModifiedTime() == null : this.getModifiedTime().equals(other.getModifiedTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getCategory() == null) ? 0 : getCategory().hashCode());
        result = prime * result + ((getConsumption() == null) ? 0 : getConsumption().hashCode());
        result = prime * result + ((getPurpose() == null) ? 0 : getPurpose().hashCode());
        result = prime * result + ((getYear() == null) ? 0 : getYear().hashCode());
        result = prime * result + ((getMonth() == null) ? 0 : getMonth().hashCode());
        result = prime * result + ((getAmount() == null) ? 0 : getAmount().hashCode());
        result = prime * result + ((getPlace() == null) ? 0 : getPlace().hashCode());
        result = prime * result + ((getEmissionType() == null) ? 0 : getEmissionType().hashCode());
        result = prime * result + ((getCreatedTime() == null) ? 0 : getCreatedTime().hashCode());
        result = prime * result + ((getModifiedTime() == null) ? 0 : getModifiedTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", category=").append(category);
        sb.append(", consumption=").append(consumption);
        sb.append(", purpose=").append(purpose);
        sb.append(", year=").append(year);
        sb.append(", month=").append(month);
        sb.append(", amount=").append(amount);
        sb.append(", place=").append(place);
        sb.append(", emissionType=").append(emissionType);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", modifiedTime=").append(modifiedTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
