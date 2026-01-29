package org.schabi.newpipe.brave.misc;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.schabi.newpipe.App;

import java.io.IOException;
import java.util.Optional;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Handle 403 in case cloudflare questions a human using the app.
 */
public class BraveRumble403Interceptor implements Interceptor {

    private static final MediaType HTML = MediaType.get("text/html; charset=utf-8");
    private final BraveRumbleCloudflareManager bypassManager;

    public BraveRumble403Interceptor(final BraveRumbleCloudflareManager manager) {
        this.bypassManager = manager;
    }

    public static Optional<Interceptor> getInterceptor(
            final OkHttpClient.Builder builder) {
        return builder.interceptors().stream().filter(
                BraveRumble403Interceptor.class::isInstance).findFirst();
    }

    @NonNull
    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();

        if (!request.url().host().contains("rumble.com")) {
            return chain.proceed(request);
        }

        //USELESS->DISABLED // reuse previously retrieved cookies from the webView
        final String cookies = ""; //USELESS->DISABLED bypassManager.getCurrentCookies();
        //USELESS->DISABLED final Request.Builder builder = request.newBuilder();
        //USELESS->DISABLED if (!cookies.isEmpty()) {
        //USELESS->DISABLED     builder.header("Cookie", cookies);
        //USELESS->DISABLED }

        //USELESS->DISABLED final Response response = chain.proceed(builder.build());
        final Response response = chain.proceed(request);

        if (response.code() == 200) {
            debugMessage("CF_DBG 1.0", "", response.code(), cookies, request.url().toString());
            return response;
        }

        if (response.code() == 403) {
            final BraveRumbleCloudflareManager.BypassResult bypassResult =
                    bypassManager.fetchContentViaWebView(request.url().toString(), 30000);

            debugMessage("CF_DBG 2.0", "", response.code(),
                    bypassResult.cookies(), request.url().toString());

            if (bypassResult.success() && bypassResult.content() != null) {

                debugMessage("CF_DBG 2.1", "webview success", 200,
                        bypassResult.cookies(), request.url().toString());

                // reuse the webView's content as a proper okHttp response.
                final Request newRequest = request.newBuilder().build();
                return new Response.Builder()
                        .request(newRequest)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(HTML, bypassResult.content()))
                        .build();
            }
        }

        debugMessage("CF_DBG 3.0", "", response.code(), cookies, request.url().toString());

        return response;
    }

    private void debugMessage(
            final String tag,
            final String prefix,
            final int code,
            final String cookies,
            final String url
    ) {
        if (!BraveRumbleCloudflareManager.DBG_CF) {
            return;
        }
        Log.d(tag, prefix + " code " + code + " cookies " + cookies + " url " + url);
        new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(App.getApp().getApplicationContext(),
                            tag + " " + prefix + " code " + code + " cookies "
                                    + cookies + " url " + url,
                            Toast.LENGTH_SHORT).show();
                }
        );
    }
}
