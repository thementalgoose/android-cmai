package com.mopub.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.webkit.WebView;

import com.mopub.common.ClientMetadata;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.DeviceUtils;
import com.mopub.volley.Cache;
import com.mopub.volley.Network;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.toolbox.BasicNetwork;
import com.mopub.volley.toolbox.DiskBasedCache;
import com.mopub.volley.toolbox.HttpStack;
import com.mopub.volley.toolbox.HurlStack;
import com.mopub.volley.toolbox.ImageLoader;

import java.io.File;

public class Networking {
    private static final String CACHE_DIRECTORY_NAME = "mopub-volley-cache";
    private static final int TEN_MB = 10 * 1024 * 1024;

    private static RequestQueue sRequestQueue;
    private static String sUserAgent;
    private static MaxWidthImageLoader sMaxWidthImageLoader;

    @NonNull
    public static RequestQueue getRequestQueue(@NonNull Context context) {
        RequestQueue requestQueue = sRequestQueue;
        // Double-check locking to initialize.
        if (requestQueue == null) {
            synchronized (Networking.class) {
                requestQueue = sRequestQueue;
                if (requestQueue == null) {
                    // Guarantee ClientMetadata is set up.
                    final ClientMetadata clientMetadata = ClientMetadata.getInstance(context);
                    HurlStack.UrlRewriter urlRewriter = new PlayServicesUrlRewriter(clientMetadata.getDeviceId(), context);

                    final String userAgent = Networking.getUserAgent(context.getApplicationContext());
                    HttpStack httpStack = new RequestQueueHttpStack(userAgent, urlRewriter);

                    Network network = new BasicNetwork(httpStack);
                    File volleyCacheDir = new File(context.getCacheDir().getPath() + File.separator
                            + CACHE_DIRECTORY_NAME);
                    Cache cache = new DiskBasedCache(volleyCacheDir, (int) DeviceUtils.diskCacheSizeBytes(volleyCacheDir, TEN_MB));
                    requestQueue = new RequestQueue(cache, network);
                    sRequestQueue = requestQueue;
                    sRequestQueue.start();
                }
            }
        }

        return requestQueue;
    }

    @NonNull
    public static ImageLoader getImageLoader(@NonNull Context context) {
        MaxWidthImageLoader imageLoader = sMaxWidthImageLoader;
        // Double-check locking to initialize.
        if (imageLoader == null) {
            synchronized (Networking.class) {
                imageLoader = sMaxWidthImageLoader;
                if (imageLoader == null) {
                    RequestQueue queue = getRequestQueue(context);
                    int cacheSize = DeviceUtils.memoryCacheSizeBytes(context);
                    final LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(cacheSize) {
                        @Override
                        protected int sizeOf(String key, Bitmap value) {
                            if (value != null) {
                                return value.getRowBytes() * value.getHeight();
                            }

                            return super.sizeOf(key, value);
                        }
                    };
                    imageLoader = new MaxWidthImageLoader(queue, context, new MaxWidthImageLoader.ImageCache() {
                        @Override
                        public Bitmap getBitmap(final String key) {
                            return imageCache.get(key);
                        }

                        @Override
                        public void putBitmap(final String key, final Bitmap bitmap) {
                            imageCache.put(key, bitmap);
                        }
                    });
                    sMaxWidthImageLoader = imageLoader;
                }
            }
        }
        return imageLoader;
    }

    /**
     * Caches and returns the WebView user agent to be used across all SDK requests. This is
     * important because advertisers expect the same user agent across all request, impression, and
     * click events.
     */
    @NonNull
    public static String getUserAgent(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        String userAgent = sUserAgent;
        if (userAgent == null) {
            synchronized (Networking.class) {
                userAgent = sUserAgent;
                if (userAgent == null) {
                    // As of Android 4.4, WebViews may only be instantiated on the UI thread
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        sUserAgent = new WebView(context).getSettings().getUserAgentString();
                    } else {
                        // In the exceptional case where we can't access the WebView user agent,
                        // fall back to the System-specific user agent.
                        sUserAgent = System.getProperty("http.agent");
                    }
                }
            }
        }

        return sUserAgent;
    }

    @VisibleForTesting
    public static synchronized void clearForTesting() {
        sRequestQueue = null;
        sMaxWidthImageLoader = null;
        sUserAgent = null;
    }

    @VisibleForTesting
    public static synchronized void setRequestQueueForTesting(RequestQueue queue) {
        sRequestQueue = queue;
    }

    @VisibleForTesting
    public static synchronized void setImageLoaderForTesting(MaxWidthImageLoader imageLoader) {
        sMaxWidthImageLoader = imageLoader;
    }

    @VisibleForTesting
    public static synchronized void setUserAgentForTesting(String userAgent) {
        sUserAgent = userAgent;
    }
}
