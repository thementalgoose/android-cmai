package com.mopub.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.DataKeys;
import com.mopub.common.util.Json;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.AdTypeTranslator;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Request;
import com.mopub.volley.Response;
import com.mopub.volley.toolbox.HttpHeaderParser;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

import static com.mopub.network.HeaderUtils.extractBooleanHeader;
import static com.mopub.network.HeaderUtils.extractHeader;
import static com.mopub.network.HeaderUtils.extractIntegerHeader;

public class AdRequest extends Request<AdResponse> {

    @NonNull private final AdRequest.Listener mListener;
    @NonNull private final AdFormat mAdFormat;

    public interface Listener extends Response.ErrorListener {
        public void onSuccess(AdResponse response);
    }

    public AdRequest(@NonNull final String url, @NonNull final AdFormat adFormat,
            @NonNull final Listener listener) {
        super(Method.GET, url, listener);
        mListener = listener;
        mAdFormat = adFormat;
        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        setRetryPolicy(retryPolicy);
        setShouldCache(false);
    }

    @Override
    protected Response<AdResponse> parseNetworkResponse(final NetworkResponse networkResponse) {
        // NOTE: We never get status codes outside of {[200, 299], 304}. Those errors are sent to the
        // error listener.

        Map<String, String> headers = networkResponse.headers;
        if (extractBooleanHeader(headers, ResponseHeader.WARMUP, false)) {
            return Response.error(new MoPubNetworkError("Ad Unit is warming up.", MoPubNetworkError.Reason.WARMING_UP));
        }



        AdResponse.Builder builder = new AdResponse.Builder();

        String adTypeString = extractHeader(headers, ResponseHeader.AD_TYPE);
        String fullAdTypeString = extractHeader(headers, ResponseHeader.FULL_AD_TYPE);

        builder.setAdType(adTypeString);
        builder.setFullAdType(fullAdTypeString);
        if (AdType.CLEAR.equals(adTypeString)) {
            return Response.error(new MoPubNetworkError("No ads found for ad unit.", MoPubNetworkError.Reason.NO_FILL));
        }

        builder.setNetworkType(extractHeader(headers, ResponseHeader.NETWORK_TYPE));
        String redirectUrl = extractHeader(headers, ResponseHeader.REDIRECT_URL);
        builder.setRedirectUrl(redirectUrl);
        String clickTrackingUrl = extractHeader(headers, ResponseHeader.CLICK_TRACKING_URL);
        builder.setClickTrackingUrl(clickTrackingUrl);
        builder.setImpressionTrackingUrl(extractHeader(headers, ResponseHeader.IMPRESSION_URL));
        builder.setFailoverUrl(extractHeader(headers, ResponseHeader.FAIL_URL));
        boolean isScrollable = extractBooleanHeader(headers, ResponseHeader.SCROLLABLE, false);
        builder.setScrollable(isScrollable);
        builder.setDimensions(extractIntegerHeader(headers, ResponseHeader.WIDTH),
                extractIntegerHeader(headers, ResponseHeader.HEIGHT));

        Integer adTimeoutDelaySeconds = extractIntegerHeader(headers, ResponseHeader.AD_TIMEOUT);
        builder.setAdTimeoutDelayMilliseconds(
                adTimeoutDelaySeconds == null ? null : adTimeoutDelaySeconds * 1000);

        Integer refreshTimeSeconds = extractIntegerHeader(headers, ResponseHeader.REFRESH_TIME);
        builder.setRefreshTimeMilliseconds(
                refreshTimeSeconds == null ? null : refreshTimeSeconds * 1000);

        // Response Body encoding / decoding
        String responseBody = parseStringBody(networkResponse);
        builder.setResponseBody(responseBody);
        if (AdType.NATIVE.equals(adTypeString)) {
            try {
                builder.setJsonBody(new JSONObject(responseBody));
            } catch (JSONException e) {
                return Response.error(
                        new MoPubNetworkError("Failed to decode body JSON for native ad format",
                                e, MoPubNetworkError.Reason.BAD_BODY));
            }
        }

        // Derive custom event fields
        String customEventClassName = AdTypeTranslator.getCustomEventName(mAdFormat, adTypeString,
                fullAdTypeString, headers);
        builder.setCustomEventClassName(customEventClassName);

        // Process server extras if they are present:
        String customEventData = extractHeader(headers, ResponseHeader.CUSTOM_EVENT_DATA);

        // Some server-supported custom events (like Millennial banners) use a different header field
        if (TextUtils.isEmpty(customEventData)) {
            customEventData = extractHeader(headers, ResponseHeader.NATIVE_PARAMS);
        }
        try {
            builder.setServerExtras(Json.jsonStringToMap(customEventData));
        } catch (JSONException e) {
            return Response.error(
                    new MoPubNetworkError("Failed to decode server extras for custom event data.",
                            e, MoPubNetworkError.Reason.BAD_HEADER_DATA));
        }

        // Some MoPub-specific custom events get their serverExtras from the response itself:
        if (eventDataIsInResponseBody(adTypeString, fullAdTypeString)) {
            Map<String, String> eventDataMap = new TreeMap<String, String>();
            eventDataMap.put(DataKeys.HTML_RESPONSE_BODY_KEY, responseBody);
            eventDataMap.put(DataKeys.SCROLLABLE_KEY, Boolean.toString(isScrollable));
            if (redirectUrl != null) {
                eventDataMap.put(DataKeys.REDIRECT_URL_KEY, redirectUrl);
            }
            if (clickTrackingUrl != null) {
                eventDataMap.put(DataKeys.CLICKTHROUGH_URL_KEY, clickTrackingUrl);
            }
            builder.setServerExtras(eventDataMap);
        }

        return Response.success(builder.build(),  // Cast needed for Response generic.
                HttpHeaderParser.parseCacheHeaders(networkResponse));
    }

    private boolean eventDataIsInResponseBody(@Nullable String adType,
            @Nullable String fullAdType) {
        return "mraid".equals(adType) || "html".equals(adType) ||
                ("interstitial".equals(adType) && "vast".equals(fullAdType));
    }

    // Based on Volley's StringResponse class.
    protected String parseStringBody(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return parsed;
    }

    @Override
    protected void deliverResponse(final AdResponse adResponse) {
        mListener.onSuccess(adResponse);
    }
}
