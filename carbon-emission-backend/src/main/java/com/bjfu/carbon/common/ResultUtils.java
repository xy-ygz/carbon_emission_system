package com.bjfu.carbon.common;

/**
 * 返回工具类
 *
 * @author xgy
 * @since 2023-02-13
 */
public class ResultUtils {

    /**
     * 成功
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, data, "success");
    }

    /**
     * 失败
     *
     * @param errorCode
     * @param <T> 返回类型（错误时data为null）
     * @return
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), null, errorCode.getMessage());
    }

    /**
     * 失败
     *
     * @param code
     * @param message
     * @param description
     * @param <T> 返回类型（错误时data为null）
     * @return
     */
    public static <T> Result<T> error(int code, String message, String description) {
        return new Result<>(code, null, message, description);
    }

    public static <T> Result<T> error(ErrorCode errorCode, String message, String description) {
        return new Result<>(errorCode.getCode(), null, message, description);
    }

    /**
     * 失败
     * message使用ErrorCode的默认message，description为自定义的错误描述
     *
     * @param errorCode
     * @param description 错误描述（详情）
     * @param <T> 返回类型（错误时data为null）
     * @return
     */
    public static <T> Result<T> error(ErrorCode errorCode, String description) {
        return new Result<>(errorCode.getCode(), null, errorCode.getMessage(), description);
    }
}
