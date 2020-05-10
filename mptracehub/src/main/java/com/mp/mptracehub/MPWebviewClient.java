package com.mp.mptracehub;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Map;

import androidx.annotation.Nullable;

public class MPWebviewClient extends WebViewClient {
    MagicPixelTraceHub logCollector;

    public MPWebviewClient(MagicPixelTraceHub logCollector){
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
        String httpresponse = "";
        String mimeType="";
        if(response!=null){
            statusCode = response.getStatusCode();
            Map<String,String> responseHeaderMap = response.getResponseHeaders();
            responseHeaders = responseHeaderMap.toString();
            mimeType = response.getMimeType();
            httpresponse = getTextFromStream(response.getData());
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("typ", "REQ");
            jsonObject.put("url",url);
            jsonObject.put("headers",requestHeaders);
            jsonObject.put("method", method);
            jsonObject.put("mime", mimeType);
            jsonObject.put("body", "<Can't resolve body>");
            logCollector.send(jsonObject.toString());
            jsonObject.put("typ", "RES");
            jsonObject.put("url",url);
            jsonObject.put("headers",responseHeaders);
            jsonObject.put("method", method);
            jsonObject.put("mime", mimeType);
            jsonObject.put("body", httpresponse);
            logCollector.send(jsonObject.toString());
        }catch(Exception ex){}
        return response;
    }

    private String getTextFromStream(InputStream inputStream) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder data = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                data.append(line).append('\n');
            }
            return data.toString();
        }catch (Exception ex){return "<couldn't read response data>";}
    }
}
