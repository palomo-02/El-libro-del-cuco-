package com.example.ellibrodelcuco.data.callback;

public interface Callback<T> {
    void onSuccess(T result);
    void onError(String errorMessage);
}
