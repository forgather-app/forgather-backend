package com.forgather.global.response;

public record ApiResponse<T>(
    ResponseCode code,
    String message,
    T data
) {

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ResponseCode.SUCCESS, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS, null, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS, message, data);
    }

    public static ApiResponse<Void> error(ResponseCode code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
