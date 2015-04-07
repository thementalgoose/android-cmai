package com.mopub.network;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Response;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class AdRequestTest {

    @Mock
    private AdRequest.Listener mockListener;
    @Mock
    private AdResponse mockAdResponse;

    private AdRequest subject;
    private HashMap<String, String> defaultHeaders;


    @Before
    public void setup() {
        subject = new AdRequest("testUrl", AdFormat.NATIVE, mockListener);
        defaultHeaders = new HashMap<String, String>();
        defaultHeaders.put(ResponseHeader.SCROLLABLE.getKey(), "0");
        defaultHeaders.put(ResponseHeader.REDIRECT_URL.getKey(), "redirect");
        defaultHeaders.put(ResponseHeader.CLICK_TRACKING_URL.getKey(), "click_tracking");
        defaultHeaders.put(ResponseHeader.IMPRESSION_URL.getKey(), "impression");
        defaultHeaders.put(ResponseHeader.FAIL_URL.getKey(), "fail_url");
        defaultHeaders.put(ResponseHeader.REFRESH_TIME.getKey(), "30");
    }

    @Test
    public void parseNetworkResponse_stringBody_shouldSucceed() throws Exception {
        defaultHeaders.put(ResponseHeader.AD_TYPE.getKey(), AdType.HTML);
        NetworkResponse testResponse =
                new NetworkResponse(200, "abc".getBytes(Charset.defaultCharset()), defaultHeaders, false);
        final Response<AdResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response.result).isNotNull();
        assertThat(response.result.getStringBody()).isEqualTo("abc");
    }

    @Test
    public void parseNetworkResponse_withServerExtrasInResponseBody_shouldSucceed() throws Exception {
        defaultHeaders.put(ResponseHeader.AD_TYPE.getKey(), AdType.HTML);
        defaultHeaders.put(ResponseHeader.FULL_AD_TYPE.getKey(), "anything");
        NetworkResponse testResponse =
                new NetworkResponse(200, "abc".getBytes(Charset.defaultCharset()), defaultHeaders, false);
        final Response<AdResponse> response = subject.parseNetworkResponse(testResponse);

        // Check the server extras
        final Map<String, String> serverExtras = response.result.getServerExtras();
        assertThat(serverExtras).isNotNull();
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.SCROLLABLE_KEY)).isEqualToIgnoringCase("false");
        assertThat(serverExtras.get(DataKeys.REDIRECT_URL_KEY)).isEqualToIgnoringCase("redirect");
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualToIgnoringCase("click_tracking");
    }

    @Test
    public void parseNetworkResponse_nonJsonStringBodyForNative_jsonParseShouldFail() {
        defaultHeaders.put(ResponseHeader.AD_TYPE.getKey(), AdType.NATIVE);
        NetworkResponse testResponse =
                new NetworkResponse(200, "abc".getBytes(Charset.defaultCharset()), defaultHeaders, false);

        final Response<AdResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response.error).isNotNull();
        assertThat(response.error).isExactlyInstanceOf(MoPubNetworkError.class);
        assertThat(((MoPubNetworkError) response.error).getReason()).isEqualTo(MoPubNetworkError.Reason.BAD_BODY);
    }

    @Test
    public void parseNetworkResponse_withWarmupHeaderTrue_shouldError() {
        defaultHeaders.put(ResponseHeader.AD_TYPE.getKey(), AdType.NATIVE);
        defaultHeaders.put(ResponseHeader.WARMUP.getKey(), "1");
        NetworkResponse testResponse =
                new NetworkResponse(200, "abc".getBytes(Charset.defaultCharset()), defaultHeaders, false);
        final Response<AdResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response.error).isNotNull();
        assertThat(response.error).isInstanceOf(MoPubNetworkError.class);
        assertThat(((MoPubNetworkError) response.error).getReason()).isEqualTo(MoPubNetworkError.Reason.WARMING_UP);
    }

    @Test
    public void parseNetworkResponse_withClearAdType_shouldError() {
        defaultHeaders.put(ResponseHeader.AD_TYPE.getKey(), AdType.CLEAR);

        NetworkResponse testResponse =
                new NetworkResponse(200, "abc".getBytes(Charset.defaultCharset()), defaultHeaders, false);
        final Response<AdResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response.error).isNotNull();
        assertThat(response.error).isInstanceOf(MoPubNetworkError.class);
        assertThat(((MoPubNetworkError) response.error).getReason()).isEqualTo(MoPubNetworkError.Reason.NO_FILL);
    }

    @Test
    public void deliverResponse_shouldCallListenerOnSuccess() throws Exception {
        subject.deliverResponse(mockAdResponse);
        verify(mockListener).onSuccess(mockAdResponse);
    }

    @Test
    public void parseNetworkResponse_withBadJSON_shouldReturnError() {
        defaultHeaders.put(ResponseHeader.AD_TYPE.getKey(), AdType.NATIVE);
        NetworkResponse badNativeNetworkResponse = new NetworkResponse(200,
                "{[abc}".getBytes(Charset.defaultCharset()),
                defaultHeaders, false);
        subject = new AdRequest("testUrl", AdFormat.NATIVE, mockListener);

        final Response<AdResponse> response = subject.parseNetworkResponse(badNativeNetworkResponse);

        assertThat(response.error).isNotNull();
        assertThat(response.error.getCause()).isExactlyInstanceOf(JSONException.class);
    }
}
