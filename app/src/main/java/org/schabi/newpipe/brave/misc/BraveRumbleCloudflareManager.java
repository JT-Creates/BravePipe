package org.schabi.newpipe.brave.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.ead.lib.cloudflare_bypass.BypassClient;

import org.schabi.newpipe.DownloaderImpl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Wrap around BypassClient to tell cloudflare, that a human is using the app.
 */
@SuppressLint("StaticFieldLeak")
public final class BraveRumbleCloudflareManager {

    public static final boolean DBG_CF = false; // enable to see some debug messages

    public record BypassResult(
            boolean success,
            String content,
            String cookies
    ) { }


    private static volatile BraveRumbleCloudflareManager instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String[] cookieDomains = {
            "https://rumble.com",
            "rumble.com",
            ".rumble.com",
            "https://www.rumble.com",
            "www.rumble.com"
    };
    private WebView webView;
    private volatile String currentCookies = "";

    private BraveRumbleCloudflareManager(final Context context) {
        final Context appContext = context.getApplicationContext();

        if (Looper.myLooper() != Looper.getMainLooper()) {
            final CountDownLatch initLatch = new CountDownLatch(1);
            mainHandler.post(() -> {
                createWebView(appContext);
                initLatch.countDown();
            });
            try {
                initLatch.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            createWebView(appContext);
        }
    }

    public static BraveRumbleCloudflareManager getInstance(final Context context) {
        if (instance == null) {
            synchronized (BraveRumbleCloudflareManager.class) {
                if (instance == null) {
                    instance = new BraveRumbleCloudflareManager(context);
                }
            }
        }
        return instance;
    }

    private void createWebView(final Context context) {
        webView = new WebView(context);
        webView.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        webView.setVisibility(View.GONE);

        final WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUserAgentString(DownloaderImpl.USER_AGENT);
    }

    public synchronized BypassResult fetchContentViaWebView(
            final String url,
            final long timeoutMs) {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] resultContent = {null};
        final String[] resultCookies = {""};
        final boolean[] success = {false};

        try {
            mainHandler.post(() -> {
                webView.setWebViewClient(new BypassClient() {
                    @Override
                    public void onPageFinishedByPassed(
                            final WebView view,
                            final String loadedUrl) {
                        super.onPageFinishedByPassed(view, loadedUrl);

                        final boolean isResultJson = loadedUrl.contains("embedJS");
                        final String script = isResultJson
                                ? "document.body.textContent || document.body.innerText"
                                : "document.documentElement.outerHTML";

                        webView.evaluateJavascript(
                                "(function() { return " + script + "; })();",
                                value -> {
                                    if (value != null && !value.equals("null")) {
                                        // Remove the surrounding quotation marks
                                        String raw = value.substring(1, value.length() - 1);
                                        if (!isResultJson) {
                                            raw = BraveStringEscapeUtils.unescapeJava(raw);
                                        }
                                        resultContent[0] = raw.trim();
                                    }
                                    success[0] = true;
                                    resultCookies[0] = updateAndGetCookies();
                                    latch.countDown();
                                }
                        );
                    }

                    /**
                     *  We use the deprecated version as we want only main page errors.
                     *
                     * @param view The WebView that is initiating the callback.
                     * @param errorCode The error code corresponding to an ERROR_* value.
                     * @param description A String describing the error.
                     * @param failingUrl The url that failed to load.
                     */
                    @Override
                    public void onReceivedError(
                            final WebView view,
                            final int errorCode,
                            final String description,
                            final String failingUrl) {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                        latch.countDown();
                    }
                });
                webView.loadUrl(url);
            });

            dumpCookiesForKnownDomains();

            final boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            return new BypassResult(completed &&  success[0], resultContent[0], resultCookies[0]);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BypassResult(false, null, currentCookies);
        }
    }

    private String updateAndGetCookies() {
        String selectedCookies = null;

        for (final String domain : cookieDomains) {
            final String cookies = CookieManager.getInstance().getCookie(domain);
            if (cookies != null && !cookies.isEmpty()) {
                if (cookies.contains("__cf")) {
                    selectedCookies = cookies;
                    break;  // cancel we found CF-Cookie
                }
                if (selectedCookies == null) {
                    selectedCookies = cookies;  // fallback: first none empty cookie
                }
            }
        }

        currentCookies = (selectedCookies != null) ? selectedCookies : "";
        return currentCookies;
    }

    private void dumpCookiesForKnownDomains() {
        if (!DBG_CF) {
            return;
        }
        final CookieManager manager = CookieManager.getInstance();
        manager.flush();

        for (final String domain : cookieDomains) {
            final String cookies = manager.getCookie(domain);
            if (cookies != null && !cookies.isEmpty()) {
                Log.d("CF_DBG COOKIES", domain + " => " + cookies);
            }
        }
    }

    public String getCurrentCookies() {
        return currentCookies;
    }

    public void destroy() {
        mainHandler.post(() -> {
            if (webView != null) {
                webView.destroy();
            }
            instance = null;
        });
    }
}
