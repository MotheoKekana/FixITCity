package com.example.fixitcity.data;

public interface AuthCallback<T> {
    void onSuccess(T result);
    void onError(String message);
}