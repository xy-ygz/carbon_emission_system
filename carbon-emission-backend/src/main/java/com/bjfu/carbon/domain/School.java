package com.bjfu.carbon.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 学校实体类
 *
 * @author xgy
 * @since 2023-02-13
 */
@TableName(value ="school")
@Data
public class School implements Serializable {
    /**
     * 
     */
    @TableId
    private Integer id;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 学校总人数
     */
    private Long totalNumber;

    /**
     * 在校学生人数
     */
    private Integer studentNumber;

    /**
     * 在校教师人数
     */
    private Integer teacherNumber;

    /**
     * 学校绿化总面积
     */
    private Long greenArea;

    /**
     * 学校建筑物总占地面积
     */
    private Long buildingArea;

    /**
     * 学校总占地面积
     */
    private Long totalArea;

    /**
     * 学校图片地址
     */
    private String imageUrl;

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
        School other = (School) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getSchoolName() == null ? other.getSchoolName() == null : this.getSchoolName().equals(other.getSchoolName()))
            && (this.getTotalNumber() == null ? other.getTotalNumber() == null : this.getTotalNumber().equals(other.getTotalNumber()))
            && (this.getStudentNumber() == null ? other.getStudentNumber() == null : this.getStudentNumber().equals(other.getStudentNumber()))
            && (this.getTeacherNumber() == null ? other.getTeacherNumber() == null : this.getTeacherNumber().equals(other.getTeacherNumber()))
            && (this.getGreenArea() == null ? other.getGreenArea() == null : this.getGreenArea().equals(other.getGreenArea()))
            && (this.getBuildingArea() == null ? other.getBuildingArea() == null : this.getBuildingArea().equals(other.getBuildingArea()))
            && (this.getTotalArea() == null ? other.getTotalArea() == null : this.getTotalArea().equals(other.getTotalArea()))
            && (this.getImageUrl() == null ? other.getImageUrl() == null : this.getImageUrl().equals(other.getImageUrl()))
            && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
            && (this.getModifiedTime() == null ? other.getModifiedTime() == null : this.getModifiedTime().equals(other.getModifiedTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getSchoolName() == null) ? 0 : getSchoolName().hashCode());
        result = prime * result + ((getTotalNumber() == null) ? 0 : getTotalNumber().hashCode());
        result = prime * result + ((getStudentNumber() == null) ? 0 : getStudentNumber().hashCode());
        result = prime * result + ((getTeacherNumber() == null) ? 0 : getTeacherNumber().hashCode());
        result = prime * result + ((getGreenArea() == null) ? 0 : getGreenArea().hashCode());
        result = prime * result + ((getBuildingArea() == null) ? 0 : getBuildingArea().hashCode());
        result = prime * result + ((getTotalArea() == null) ? 0 : getTotalArea().hashCode());
        result = prime * result + ((getImageUrl() == null) ? 0 : getImageUrl().hashCode());
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
        sb.append(", schoolName=").append(schoolName);
        sb.append(", totalNumber=").append(totalNumber);
        sb.append(", studentNumber=").append(studentNumber);
        sb.append(", teacherNumber=").append(teacherNumber);
        sb.append(", greenArea=").append(greenArea);
        sb.append(", buildingArea=").append(buildingArea);
        sb.append(", totalArea=").append(totalArea);
        sb.append(", imageUrl=").append(imageUrl);
        sb.append(", createdTime=").append(createdTime);
        sb.append(", modifiedTime=").append(modifiedTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}