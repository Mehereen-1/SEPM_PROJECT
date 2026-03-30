package com.example.project.admin.dto;

public class AdminApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;

    private AdminApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> AdminApiResponse<T> success(String message, T data) {
        return new AdminApiResponse<>(true, message, data);
    }

    public static <T> AdminApiResponse<T> error(String message, T data) {
        return new AdminApiResponse<>(false, message, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
