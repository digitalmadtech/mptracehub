package com.mp.mptracehub;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MagicPixelTraceHub {

    private static LinkedList<String> list = new LinkedList<>();
    private static final String ANDROID_LOG_TIME_FORMAT = "MM-dd kk:mm:ss.SSS";
    private SimpleDateFormat logCatDate = new SimpleDateFormat(ANDROID_LOG_TIME_FORMAT);
    private static int pid;

    private static boolean isRunning = true;
    private static long lastReadTime = 0;
    private static ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private static ScheduledExecutorService senderScheduler =
            Executors.newScheduledThreadPool(1);
    private static ScheduledExecutorService stopScheduler =
            Executors.newScheduledThreadPool(1);

    private static MagicPixelTraceHub _instance;
    //private static MPRestClient restClient;
    private static MPWebSocketClient wsClient;
    private static String URI = null;
    private static Calendar lastRunTime;
    private static String configAssetName;
    private static String vendorId;
    private static String projectId;
    private static String prefix;
    private static String debugId;
    private static Properties properties;
    private static LogParser parser;

    public static void initCollector(Context context, String configAssetName) throws Exception {
        if (_instance == null)
            _instance = new MagicPixelTraceHub(context, configAssetName);
        if(debugId==null)debugId = prefix+"_"+random();
        parser = new LogParser();
        Log.println(Log.DEBUG,"TRACEHUBLOG","Random ID:"+debugId);
    }

    public static String getDebugSessionId(){
        return debugId;
    }

    public static void startCollector() {
        if(_instance==null)throw new RuntimeException("LogCollector init must be called first");
        _instance.start();
    }

    public static void registerWebView(WebView webView){
        if(_instance==null)throw new RuntimeException("LogCollector init must be called first");
        webView.setWebViewClient(new MPWebviewClient(_instance));
    }

    public static void stopCollector() {
        if(_instance==null)throw new RuntimeException("LogCollector init must be called first");
        _instance.stop();
    }

    private static void loadProperty(Context context) throws IOException {
        properties = new Properties();
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(configAssetName);
        properties.load(inputStream);
    }

    private MagicPixelTraceHub(Context context, String configAssetName) throws Exception {
        this.configAssetName = configAssetName;
        loadProperty(context);
        pid = android.os.Process.myPid();
        java.net.URI uri = new URI(properties.containsKey("mpLogUrl")?properties.getProperty("mpLogUrl"):"wss://thub-pub.magicpixel.io");
        String key = properties.getProperty("apiKey");
        vendorId = properties.getProperty("vendorId");
        projectId = properties.getProperty("projectId");
        prefix = properties.getProperty("prefix");
        Map<String, String> header = new HashMap<>();
        header.put("x-api-key",key);
        wsClient = new MPWebSocketClient(uri,header);
        wsClient.connect();
    }

    public static void log(String tag, String message){
        try {
            String jsonData = "";
            try {
                Log.i("TRACEHUBLOG:"+tag,message);
                jsonData = buildJsonRequest(new String[]{message}, tag);
            } catch (JSONException e) {
                Log.println(Log.DEBUG, "TRACEHUBLOG", Log.getStackTraceString(e));
            }
            wsClient.send(jsonData);
            Log.println(Log.DEBUG, "TRACEHUBLOG", "1 line sent");
        }catch(Exception ex){}
    }

    private void start() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                MagicPixelTraceHub.this.run();
            }
        };
        scheduler.scheduleAtFixedRate(r, 1, 5, TimeUnit.SECONDS);
        Runnable stopper = new Runnable() {
            @Override
            public void run() {
                isRunning=false;
            }
        };
        stopScheduler.scheduleWithFixedDelay(stopper,24,24,TimeUnit.HOURS);
        Thread tt = new Thread(){
            public void run(){
               while(isRunning){
                   try {
                   r.run();
                   Thread.sleep(5000);
                   } catch (Exception e) {
                   }
               }
            }
        };
        tt.start();
        Runnable r1 = new Runnable() {
            @Override
            public void run() {
                try {
                    String message = null;
                    int count = 0;
                    List<String> dataLines = new ArrayList<>();
                    while ((message = getMessage()) != null && !message.contains("TRACEHUBLOG") && count < 50) {
                        dataLines.add(message);
                        count++;
                    }
                    if (dataLines.isEmpty()) {
                        Log.println(Log.DEBUG, "TRACEHUBLOG", "No log to send");
                        if (lastRunTime != null) {
                            senderScheduler.shutdown();
                        }
                        return;
                    }
                    String jsonData = "";
                    try {
                        jsonData = buildJsonRequest(dataLines.toArray(new String[0]),null);
                    } catch (JSONException e) {
                        Log.println(Log.DEBUG, "TRACEHUBLOG", Log.getStackTraceString(e));
                    }
                    wsClient.send(jsonData);
                    Log.println(Log.DEBUG, "TRACEHUBLOG", dataLines.size() + " lines sent");
                }catch(Exception ex){}
            }
        };
        senderScheduler.scheduleAtFixedRate(r1, 5, 2, TimeUnit.SECONDS);
    }

    void send(String data){
        try {
            wsClient.send(buildJsonRequest(new String[]{data},null));
        }catch(JSONException je){
            Log.println(Log.DEBUG, "TRACEHUBLOG", Log.getStackTraceString(je));
        }
    }

    private static String buildJsonRequest(String[] data, String tag) throws JSONException{
        JSONArray lines = new JSONArray();
        for(String aLineData:data){
            JSONObject dataObject = new JSONObject();
            if(tag==null)
                dataObject.put("tag",parser.getTag(aLineData));
            else dataObject.put("tag",tag);
            dataObject.put("log", aLineData);
            lines.put(dataObject);
        }
        JSONObject obj = new JSONObject();
        obj.put("message", "publish");
        JSONObject inner = new JSONObject();
        inner.put("vid", vendorId);
        inner.put("pid", projectId);
        inner.put("did", debugId);
        inner.put("data", lines);
        obj.put("data", inner);
        return obj.toString();
    }

    private void stop() {
        scheduler.shutdown();
    }

    private static String getMessage() {
        if (list.peek() != null)
            return list.pop();
        else return null;
    }

    private static String random() {
        int leftLimit = 97;
        int rightLimit = 122;
        int targetStringLength = 5;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
       return buffer.toString().toUpperCase();
    }

    private void run() {
        BufferedReader reader = null;
        try {
            List<String> sCommand = new ArrayList<String>();
            sCommand.add("logcat");
            sCommand.add("-bmain");
            sCommand.add("-vtime");
            sCommand.add("--pid=" + pid);
            if (lastReadTime != 0) {
                sCommand.add("-t" + logCatDate.format(new Date(lastReadTime)));
            } else sCommand.add("-d");
            Process process = new ProcessBuilder().command(sCommand).start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                long when = logCatDate.parse(line).getTime();
                if (when > lastReadTime) {
                    list.add(line);
                    lastReadTime = when;
                }
            }
        } catch (Exception ex) {

        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException ex) {
            }
        }
    }
}
class LogParser{
    private final Pattern sLogHeaderPattern = Pattern.compile(
            "^\\[\\s(\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d+)"
                    + "\\s+(\\d*):\\s*(\\S+)\\s([VDIWEAF])/(.*)\\]$");


    public String getTag(String line){
        try {
            String[] parts = line.split(" ");
            String tagPart = parts[2];
            String[] innerParts = tagPart.split("/");
            String tag = innerParts[1];
            innerParts = tag.split("\\(");
            tag=innerParts[0];
            return tag.replace(":", "");
        }catch(Exception ex){
            return "NoTag";
        }
    }
}
