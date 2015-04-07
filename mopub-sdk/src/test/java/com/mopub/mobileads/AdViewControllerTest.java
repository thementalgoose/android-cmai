package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.test.support.ThreadUtils;
import com.mopub.network.AdResponse;
import com.mopub.network.Networking;
import com.mopub.volley.Request;
import com.mopub.volley.RequestQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import edu.emory.mathcs.backport.java.util.Collections;

import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class AdViewControllerTest {
    private AdViewController subject;
    @Mock
    private MoPubView mockMoPubView;
    @Mock
    private RequestQueue mockRequestQueue;
    private Reflection.MethodBuilder methodBuilder;
    private Activity context;
    private AdResponse response;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        shadowOf(context).grantPermissions(android.Manifest.permission.ACCESS_NETWORK_STATE);


        when(mockMoPubView.getContext()).thenReturn(context);
        Networking.setRequestQueueForTesting(mockRequestQueue);

        subject = new AdViewController(context, mockMoPubView);

        methodBuilder = TestMethodBuilderFactory.getSingletonMock();
        reset(methodBuilder);
        response = new AdResponse.Builder()
                .setCustomEventClassName("customEvent")
                .setClickTrackingUrl("clickUrl")
                .setImpressionTrackingUrl("impressionUrl")
                .setRedirectUrl("redirectUrl")
                .setScrollable(false)
                .setDimensions(320, 50)
                .setAdType("html")
                .setFailoverUrl("failUrl")
                .setResponseBody("testResponseBody")
                .setServerExtras(Collections.emptyMap())
                .build();
    }

    @After
    public void tearDown() throws Exception {
        reset(methodBuilder);
    }

    @Test
    public void scheduleRefreshTimer_shouldNotScheduleIfRefreshTimeIsZero() {
        response = response.toBuilder().setRefreshTimeMilliseconds(0).build();
        subject.onAdLoadSuccess(response);
        Robolectric.pauseMainLooper();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
    }

    @Test
    public void scheduleRefreshTimerIfEnabled_shouldCancelOldRefreshAndScheduleANewOne() throws Exception {
        response = response.toBuilder().setRefreshTimeMilliseconds(30).build();
        subject.onAdLoadSuccess(response);
        Robolectric.pauseMainLooper();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);
    }

    @Test
    public void scheduleRefreshTimer_shouldNotScheduleRefreshIfAutorefreshIsOff() throws Exception {
        response = response.toBuilder().setRefreshTimeMilliseconds(30).build();
        subject.onAdLoadSuccess(response);

        Robolectric.pauseMainLooper();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        subject.forceSetAutorefreshEnabled(false);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
    }

    @Test
    public void scheduleRefreshTimer_whenAdViewControllerNotConfiguredByResponse_shouldHaveDefaultRefreshTime() throws Exception {
        Robolectric.pauseMainLooper();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);

        subject.scheduleRefreshTimerIfEnabled();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        Robolectric.idleMainLooper(AdViewController.DEFAULT_REFRESH_TIME_MILLISECONDS - 1);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        Robolectric.idleMainLooper(1);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
    }

    @Test
    public void forceSetAutoRefreshEnabled_shouldSetAutoRefreshSetting() throws Exception {
        assertThat(subject.getAutorefreshEnabled()).isTrue();

        subject.forceSetAutorefreshEnabled(false);
        assertThat(subject.getAutorefreshEnabled()).isFalse();

        subject.forceSetAutorefreshEnabled(true);
        assertThat(subject.getAutorefreshEnabled()).isTrue();
    }

    @Test
    public void pauseRefresh_shouldDisableAutorefresh() throws Exception {
        assertThat(subject.getAutorefreshEnabled()).isTrue();

        subject.pauseRefresh();
        assertThat(subject.getAutorefreshEnabled()).isFalse();
    }

    @Test
    public void unpauseRefresh_afterUnpauseRefresh_shouldEnableRefresh() throws Exception {
        subject.pauseRefresh();

        subject.unpauseRefresh();
        assertThat(subject.getAutorefreshEnabled()).isTrue();
    }

    @Test
    public void pauseAndUnpauseRefresh_withRefreshForceDisabled_shouldAlwaysHaveRefreshFalse() throws Exception {
        subject.forceSetAutorefreshEnabled(false);
        assertThat(subject.getAutorefreshEnabled()).isFalse();

        subject.pauseRefresh();
        assertThat(subject.getAutorefreshEnabled()).isFalse();

        subject.unpauseRefresh();
        assertThat(subject.getAutorefreshEnabled()).isFalse();
    }

    @Test
    public void enablingAutoRefresh_afterLoadAd_shouldScheduleNewRefreshTimer() throws Exception {

        final AdViewController adViewControllerSpy = spy(subject);

        adViewControllerSpy.loadAd();
        adViewControllerSpy.forceSetAutorefreshEnabled(true);
        verify(adViewControllerSpy).scheduleRefreshTimerIfEnabled();
    }

    @Test
    public void enablingAutoRefresh_withoutCallingLoadAd_shouldNotScheduleNewRefreshTimer() throws Exception {
        final AdViewController adViewControllerSpy = spy(subject);

        adViewControllerSpy.forceSetAutorefreshEnabled(true);
        verify(adViewControllerSpy, never()).scheduleRefreshTimerIfEnabled();
    }

    @Test
    public void disablingAutoRefresh_shouldCancelRefreshTimers() throws Exception {
        response = response.toBuilder().setRefreshTimeMilliseconds(30).build();
        subject.onAdLoadSuccess(response);
        Robolectric.pauseMainLooper();

        subject.loadAd();
        subject.forceSetAutorefreshEnabled(true);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        subject.forceSetAutorefreshEnabled(false);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
    }

    @Test
    public void trackImpression_shouldAddToRequestQueue() throws Exception {
        subject.onAdLoadSuccess(response);
        subject.trackImpression();

        verify(mockRequestQueue).add(argThat(isUrl("impressionUrl")));
    }

    @Test
    public void trackImpression_noAdResponse_shouldNotAddToQueue() {
        subject.trackImpression();

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void registerClick_shouldHttpGetTheClickthroughUrl() throws Exception {
        subject.onAdLoadSuccess(response);

        subject.registerClick();
        verify(mockRequestQueue).add(argThat(isUrl("clickUrl")));
    }

    @Test
    public void registerClick_NoAdResponse_shouldNotAddToQueue() {
        subject.registerClick();
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void loadAd_shouldNotLoadWithoutConnectivity() throws Exception {
        ConnectivityManager connectivityManager = (ConnectivityManager) Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);
        shadowOf(connectivityManager.getActiveNetworkInfo()).setConnectionStatus(false);

        subject.loadAd();
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void loadAd_shouldNotLoadUrlIfAdUnitIdIsNull() throws Exception {
        subject.loadAd();

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void loadNonJavascript_shouldFetchAd() throws Exception {
        String url = "http://www.guy.com";
        subject.loadNonJavascript(url);

        verify(mockRequestQueue).add(argThat(isUrl(url)));
    }

    @Test
    public void loadNonJavascript_whenAlreadyLoading_shouldNotFetchAd() throws Exception {
        String url = "http://www.guy.com";
        subject.loadNonJavascript(url);
        reset(mockRequestQueue);
        subject.loadNonJavascript(url);

        verify(mockRequestQueue, never()).add(any(Request.class));
    }

    @Test
    public void loadNonJavascript_shouldAcceptNullParameter() throws Exception {
        subject.loadNonJavascript(null);
        // pass
    }

    @Test
    public void reload_shouldReuseOldUrl() throws Exception {
        String url = "http://www.guy.com";
        subject.loadNonJavascript(url);
        subject.setNotLoading();
        reset(mockRequestQueue);
        subject.reload();

        verify(mockRequestQueue).add(argThat(isUrl(url)));
    }

    @Test
    public void loadFailUrl_shouldLoadFailUrl() throws Exception {
        subject.onAdLoadSuccess(response);
        subject.loadFailUrl(MoPubErrorCode.INTERNAL_ERROR);

        verify(mockRequestQueue).add(argThat(isUrl("failUrl")));
        verify(mockMoPubView, never()).adFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void loadFailUrl_shouldAcceptNullErrorCode() throws Exception {
        subject.loadFailUrl(null);
        // pass
    }

    @Test
    public void loadFailUrl_whenFailUrlIsNull_shouldCallAdDidFail() throws Exception {
        response.toBuilder().setFailoverUrl(null).build();
        subject.loadFailUrl(MoPubErrorCode.INTERNAL_ERROR);

        verify(mockMoPubView).adFailed(eq(MoPubErrorCode.NO_FILL));
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void setAdContentView_whenCalledFromWrongUiThread_shouldStillSetContentView() throws Exception {
        final View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.onAdLoadSuccess(response);

        new Thread(new Runnable() {
            @Override
            public void run() {
                subject.setAdContentView(view);
            }
        }).start();
        ThreadUtils.pause(100);
        Robolectric.runUiThreadTasks();

        verify(mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(320);
        assertThat(layoutParams.height).isEqualTo(50);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenCalledAfterCleanUp_shouldNotRemoveViewsAndAddView() throws Exception {
        final View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.onAdLoadSuccess(response);

        subject.cleanup();
        new Thread(new Runnable() {
            @Override
            public void run() {
                subject.setAdContentView(view);
            }
        }).start();
        ThreadUtils.pause(10);
        Robolectric.runUiThreadTasks();

        verify(mockMoPubView, never()).removeAllViews();
        verify(mockMoPubView, never()).addView(any(View.class), any(FrameLayout.LayoutParams.class));
    }

    @Test
    public void setAdContentView_whenHonorServerDimensionsAndHasDimensions_shouldSizeAndCenterView() throws Exception {
        View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.onAdLoadSuccess(response);

        subject.setAdContentView(view);

        verify(mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(320);
        assertThat(layoutParams.height).isEqualTo(50);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenHonorServerDimensionsAndDoesntHaveDimensions_shouldWrapAndCenterView() throws Exception {
        response = response.toBuilder().setDimensions(null, null).build();
        View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.onAdLoadSuccess(response);

        subject.setAdContentView(view);

        verify(mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.height).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenNotServerDimensions_shouldWrapAndCenterView() throws Exception {
        subject.onAdLoadSuccess(response);
        View view = mock(View.class);

        subject.setAdContentView(view);

        verify(mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.height).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }
}
