package com.investview.common.response;
import java.time.Instant;


/**
 * Standard generic API response wrapper for all REST endpoints.
 *
 * @param <T> the type of data returned
 */
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    Instant timestampInstant
){
    /**
     * Convenience factory method to create a successful response with data.
     * @param data the data to return
     * @param message a human-readable message
     * @return a new ApiResponse instance
     */

    public  static <T> ApiResponse<T> success(T data){
        return  new ApiResponse<T>(true, "Operation successful", data, Instant.now());
    }

     public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

        public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }

}
