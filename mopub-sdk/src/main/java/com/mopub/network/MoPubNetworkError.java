package com.mopub.network;

import android.support.annotation.NonNull;

import com.mopub.volley.NetworkResponse;
import com.mopub.volley.VolleyError;

public class MoPubNetworkError extends VolleyError {
    public static enum Reason {
        WARMING_UP,
        NO_FILL,
        BAD_HEADER_DATA,
        BAD_BODY,
        TRACKING_FAILURE,
        UNSPECIFIED
    }

    @NonNull private final Reason mReason;

    public MoPubNetworkError(@NonNull Reason reason) {
        super();
        mReason = reason;
    }

    public MoPubNetworkError(@NonNull NetworkResponse networkResponse, @NonNull Reason reason) {
        super(networkResponse);
        mReason = reason;
    }

    public MoPubNetworkError(@NonNull Throwable cause, @NonNull Reason reason) {
        super(cause);
        mReason = reason;
    }

    public MoPubNetworkError(@NonNull String message, @NonNull Reason reason) {
        super(message);
        mReason = reason;
    }

    public MoPubNetworkError(@NonNull String message, @NonNull Throwable cause, @NonNull Reason reason) {
        super(message, cause);
        mReason = reason;
    }

    @NonNull
    public Reason getReason() {
        return mReason;
    }
}
