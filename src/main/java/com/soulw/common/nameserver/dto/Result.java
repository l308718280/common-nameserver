package com.soulw.common.nameserver.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 14:14
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {
    private Boolean success;
    private Integer code;
    private String message;
    private T data;

    /**
     * 创建成功的结果对象
     *
     * @param data 结果数据
     * @return Result<T> 带有成功标志和数据的结果对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>().setSuccess(true)
                .setMessage("success")
                .setData(data);
    }

    /**
     * 创建一个失败的结果对象，设置成功状态为false，并设置消息
     *
     * @param message 消息内容
     * @return Result<T> 结果对象，包含成功状态和消息
     */
    public static <T> Result<T> failed(String message) {
        return new Result<T>().setSuccess(false)
                .setMessage(message);
    }
}
