package com.diakogiannis.metropolitan.paper.alexiusruntracker.helpers;

import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpUtils extends AsyncTask<String, Void, String> {

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

