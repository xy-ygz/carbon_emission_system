package com.bjfu.carbon.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @author xgy
 * @since 2023-02-13
 */
@Data
public class Result<T> implements Serializable {

    /**
     * 状态码
     */
    private int code;

    /**
     * 数据
     */
    private T data;

    /**
     * 消息
     */
    private String message;

    /**
     * 描述（详情）
     */
    private String description;

    public Result(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    public Result(int code, T data, String message) {
        this(code, data, message, "");
    }

    public Result(int code, T data) {
        this(code, data, "");
    }

    public Result(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage(), errorCode.getDescription());
    }
}
