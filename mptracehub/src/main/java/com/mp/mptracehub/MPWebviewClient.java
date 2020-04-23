package com.mp.mptracehub;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Map;

import androidx.annotation.Nullable;

public class MPWebviewClient extends WebViewClient {
    LogCollector logCollector;

    public MPWebviewClient(LogCollector logCollector){
        this.logCollector = logCollector;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        String method = request.getMethod();
        Map<String,String> headerMap = request.getRequestHeaders();
        String requestHeaders = headerMap.toString();
        WebResourceResponse response = super.shouldInterceptRequest(view, request);
        int statusCode=0;
        String responseHeaders="";
        if(response!=null){
            statusCode = response.getStatusCode();
            Map<String,String> responseHeaderMap = response.getResponseHeaders();
            responseHeaders = responseHeaderMap.toString();
            String mimeType = response.getMimeType();
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[URL:");builder.append(url);
        builder.append(",");
        builder.append("req.headers:["+requestHeaders+"],");
        builder.append("res.code:"+statusCode+",");
        builder.append("res.headers:["+responseHeaders+"],");
        logCollector.send(builder.toString());
        return response;
    }
}
