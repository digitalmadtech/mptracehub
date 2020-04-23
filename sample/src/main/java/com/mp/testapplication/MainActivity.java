package com.mp.testapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Analytics;
import com.adobe.marketing.mobile.Campaign;
import com.adobe.marketing.mobile.Identity;
import com.adobe.marketing.mobile.Lifecycle;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobileServices;
import com.adobe.marketing.mobile.Signal;
import com.adobe.marketing.mobile.Target;
import com.adobe.marketing.mobile.UserProfile;
import com.mp.mptracehub.LogCollector;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    LogCollector collector = null;
    private static ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileCore.setApplication(getApplication());
        MobileCore.setLogLevel(LoggingMode.VERBOSE.DEBUG);
        try {

            Campaign.registerExtension();
            UserProfile.registerExtension();
            MobileServices.registerExtension();
            Target.registerExtension();
            Analytics.registerExtension();
            Identity.registerExtension();
            Lifecycle.registerExtension();
            Signal.registerExtension();
            MobileCore.start(new AdobeCallback() {
                @Override
                public void call(Object o) {
                    MobileCore.configureWithAppID("launch-EN07ebef26657649fe8c91c50ba6ee359b-staging");
                }
            });

//            try {
//                Process process = Runtime.getRuntime().exec("logcat -d");
//                BufferedReader bufferedReader = new BufferedReader(
//                        new InputStreamReader(process.getInputStream()));
//
//                StringBuilder log=new StringBuilder();
//                String line = "";
//                while ((line = bufferedReader.readLine()) != null) {
//                    log.append(line);
//                }
//                TextView tv = (TextView)findViewById(R.id.textview1);
//                tv.setText(log.toString());
//            }
//            catch (IOException e) {}
            final ScrollView scrollview = ((ScrollView) findViewById(R.id.scrollView));
            final WebView myWebView = (WebView) findViewById(R.id.webView);

            LogCollector.initCollector(getApplicationContext(),"mplogconfig.properties");
            LogCollector.registerWebView(myWebView);
            LogCollector.startCollector();
            myWebView.loadUrl("http://www.sprint.com");
            Runnable r = new Runnable() {
                @Override
                public void run() {

                    Log.println(Log.INFO,"MPTEST",random());
                }
            };
            //scheduler.scheduleAtFixedRate(r, 1, 5, TimeUnit.SECONDS);

//            final Handler handler = new Handler(Looper.getMainLooper());
//            final Runnable r = new Runnable(){
//                public void run() {
//                    String message = LogCollector.getMessage();
//                    if(message!=null)
//                    textView.append("\n"+message);
//                    handler.postDelayed(this, 1000);
//                    scrollview.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            scrollview.fullScroll(ScrollView.FOCUS_DOWN);
//                        }
//                    });
//                }
//            };
//            handler.post(r);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String random() {
        int leftLimit = 97;
        int rightLimit = 122;
        int targetStringLength = 25;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }
}
