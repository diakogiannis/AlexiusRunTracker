package com.diakogiannis.metropolitan.paper.alexiusruntracker.timer;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.diakogiannis.metropolitan.paper.alexiusruntracker.tracker.LocationTrack;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.lang.reflect.Field;
import java.security.cert.CertificateException;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class StartMeasuringPace {

    private Timer mTimer1;
    private TimerTask mTt1;
    private Handler mTimerHandler = new Handler();


    public void stopTimer() {
        if (mTimer1 != null) {
            mTimer1.cancel();
            mTimer1.purge();
        }
    }

    public void startTimer(final LocationTrack ltrack) {
        mTimer1 = new Timer();
        final HttpUtils httpUtils = new HttpUtils();


        mTt1 = new TimerTask() {
            String[] urlParams = new String[2];

            public void run() {


                mTimerHandler.post(new Runnable() {
                    public void run() {

                        LocationTrack refreshLoc = null;

                        try {
                            final Field fld = LocationTrack.class.getDeclaredField("loc");
                            refreshLoc = (LocationTrack) fld.get(this);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }

                        urlParams[0] = "5000";
                        urlParams[1] = "1800000";
                        String pace = httpUtils.doInBackground(urlParams);
                        Log.i("Info", "Pace " + pace);
                        Log.i("Info", "getLatitude:" + refreshLoc.getLatitude());
                    }
                });
            }
        };

        mTimer1.schedule(mTt1, 1, 5000);
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

}


class HttpUtils extends AsyncTask<String, Void, String> {

    private final String URL_STRING = "http://144.91.124.241/pacecalculatorapi/api";

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setSslSocketFactory(sslSocketFactory);
            okHttpClient.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String doInBackground(String... params) {

        Log.i("Info", "pinging " + URL_STRING);
        OkHttpClient okHttpClient = getUnsafeOkHttpClient();


        HttpUrl.Builder urlBuilder = HttpUrl.parse(URL_STRING).newBuilder();
        urlBuilder.addQueryParameter("distance", params[0]);
        urlBuilder.addQueryParameter("seconds", params[1]);
        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();
        try {

            Response response = okHttpClient.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}