package com.example.ellibrodelcuco.data.callback;

import java.util.List;

public interface SnapshotCallback<T> {
    void onChange(List<T> result);
    void onError(String errorMessage);
}
