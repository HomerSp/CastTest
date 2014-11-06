package com.homersp.casttest;

import android.app.Activity;
import android.media.DeniedByServerException;
import android.media.NotProvisionedException;
import android.media.ResourceBusyException;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.homersp.media.MediaDrm;
import com.homersp.media.MediaLogger;
import com.homersp.nsd.JmDNSHelper;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;


public class MainActivity extends Activity {
    private static final String TAG = "HomerSp." + MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        JmDNSHelper.create(this);

        File externalDir = this.getExternalFilesDir(null);
        if(externalDir != null)
            externalDir.mkdirs();

        new Thread(new Runnable() {

            @Override
            public void run() {
                String values[] = new String[] {
                        "id", "2bbc498d59dd4dc543ebeaca422ef231",
                        "ve", "02",
                        "md", Build.MODEL,
                        "ic", "http://foo.com/wrong.png",
                        "fn", Build.MODEL,
                        "ca", "5",
                        "st", "0",
                };

                CountDownLatch latch = new CountDownLatch(0);
                JmDNSHelper.addService(MainActivity.this, latch, "_googlecast._tcp.local", Build.MODEL, 8009, values);
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                MediaLogger.instance().log("MainActivity", "Creating MediaDrm");
                android.media.MediaDrm drm = MediaDrm.createMediaDrm();

                {
                    android.media.MediaDrm.ProvisionRequest request = drm.getProvisionRequest();

                    AndroidHttpClient client = AndroidHttpClient.newInstance("");

                    String url = request.getDefaultUrl();
                    if (!url.contains("?"))
                        url += "?";
                    else
                        url += "&";

                    url += "signedRequest=" + new String(request.getData());

                    HttpPost post = new HttpPost(url);
                    post.setHeader("Accept", "");
                    post.setHeader("Content-Type", "application/json");

                    MediaLogger.instance().log("Request", post.getRequestLine().toString());
                    MediaLogger.instance().log("Headers", Arrays.toString(post.getAllHeaders()));

                    try {
                        HttpResponse response = client.execute(post);
                        MediaLogger.instance().log("Response is", response.getStatusLine().toString());

                        if (response.getStatusLine().getStatusCode() == 200) {
                            byte[] dataResponse = EntityUtils.toByteArray(response.getEntity());

                            drm.provideProvisionResponse(dataResponse);

                            try {
                                drm.setPropertyString("securityLevel", "L1");

                                Log.d("HOMERSP", "openSession");
                                byte[] session = drm.openSession();
                                drm.closeSession(session);
                            } catch (ResourceBusyException e) {
                                e.printStackTrace();
                            } catch (NotProvisionedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (DeniedByServerException e) {
                        e.printStackTrace();
                    }
                }

                {
                    MediaLogger.instance().log("MainActivity", "Getting certificate request for cast.google.com");
                    MediaDrm.CertificateRequest req = MediaDrm.getCertificateRequest(drm, 1, "cast.google.com");

                    MediaLogger.instance().log("Sending data", req.getDefaultUrl());
                    AndroidHttpClient client = AndroidHttpClient.newInstance("ADT-1/MediaShell");

                    String url = req.getDefaultUrl();
                    if (!url.contains("?"))
                        url += "?";
                    else
                        url += "&";

                    url += "signedRequest=" + new String(req.getData());

                    HttpPost post = new HttpPost(url);
                    post.setHeader("Accept", "");
                    post.setHeader("Content-Type", "application/json");

                    MediaLogger.instance().log("Request", post.getRequestLine().toString());
                    MediaLogger.instance().log("Headers", Arrays.toString(post.getAllHeaders()));

                    try {
                        HttpResponse response = client.execute(post);
                        MediaLogger.instance().log("Response is", response.getStatusLine().toString());

                        if (response.getStatusLine().getStatusCode() == 200) {
                            //byte[] dataResponse = EntityUtils.toByteArray(response.getEntity());

                            byte[] dataResponse = Base64.decode("ew0KICJraW5kIjogImNlcnRpZmljYXRlcHJvdmlzaW9uaW5nI2NlcnRpZmljYXRlUHJvdmlzaW9uaW5nUmVzcG9uc2UiLA0KICJzaWduZWRSZXNwb25zZSI6ICJDcFkxQ3RBSlNBT3l3NUNtN1c4VkJJd3NSQWxKQjZ6Mjl1Yk5lT2JPQkI4dzJ0S05GaHlKNmQ1Z2hYVUtpeWt5eEFKSHVPTUpnSklXM1BXZEpab0VlRFFUX1hvSWpYOFZUZ1pMeW56RXZ4RkVVcHV3SWRFdVp5cHBsZzNuVFNZWGN2VkdUNW5IOWhIUXBJdmJQWElJUDcwQlFGZXNfTEhrX2pqZUV4MFJHSWxfbzVNM1dIdGNOdHJoZWdIRElsQzFRS0x2eEdlQWFiR2VLTXg4eDE1NG1WVU51aDJDR2pkYjAzZE9UWGdhZjh3UlZQbGdPTTFGdEQyRHJCUkx4Y1NGbHlnR3FTdmtEN0ctcklLNVBXamZEOV8yeFpvRTJvSmFoRHVKQzdTTndLT3ZUaXI0S3dEa3lOaEtQTV90cnEyNXo5QTNETURsYzdaRzJUQ3VVZ3F2aFNxMU1ZZmlMM2dnQnBaU09BTC1TSjJXMzhWQTNNa0FMRFdNSld4MHFRR3RSaERyX3YtSnZmTl8wazQ2QXAzMXQwTDFiOFYzdmlxNUlULUozYy00SlNTcEJOLXdiUzkyclF1a3ZIb1ZSTGpCejVBbFRpMVNmZFdhTW80M2UwY29lVzRHZXhKVW1iZ1h2MnRtUWtyR1F5ZHFWaVpMM2JWMTFqdEEwRDZtN0puT3gwUVI1SU1KMnVNZjcxUlFKZldPb2VCcnZqeUtIWFdKSTlXa011NGFiTmNNSi1kUzBzQzBpUnp2aTdZdVJqNkVyY0I4RVFSQ3NVMU95WXZKMVVtVUxYd1dicUtUTThWem9rMS1mMVNBVkpHRWFCbjNlUGo5eW9ZNTVLaDdfOFlfT2dLajd4UUtjVF9wbGZ0WjdkdDVybUZrVTgxUTRBN0ZIbUplUFZ3RjhaNk5kTG4yb0hMN0JTTnZoWjhpR3JFWFc1QkJOUFdkX2xaOHIySGgzRkdnVWVNaXVOdlNTWkRQLXg5bXFLM1lBa3pNdGZWeEJqZDdURm15X2I3dm1SNEFsT0l1VThOSTRobTYwVkkzci00cU9Pd2hoWHhVUk1ZYXZGbkpicFlROTg2TS1JSjdZT3VHNmFhd2pCSkVzcHZzTTJLN0JBVmNpbjV6dmhHdDE5b1QwRVVtcVVtVjFuUVpfMGI2TXotcGdiZEg5bGdad09KcGR4OV96THFGeDkyNjU0TFZ0NjBpeVNPcThxYjZBakRTa1NkVlRGcjFQeEpIaktVdUd1czZaMnQ3MTUzdEFUeUh5MU5oNERfRzN2ZFQ5UHFKNWRjOVJUTTZTYWs0ck1KNFlSeThkUDcySjhWUVVoUEpIZHZDZTdDZHdsbzZ5UEhTdFNoRHRPcDh0T0JqRURoTFZMdVVGSDl5a192OU84WkRRVHhKVTE2U05rT0tTRksyV2RUeV9RU2tObUJldERLb1h0VmloUnZKZ1A4VDNlSnh2bXJNbWh5VWN4TkFTakxEVnhFV2U3RVVrUDRROVFwNTNxMmh5cG9ENmY2VHlSS19OOUp3WkdyNk1QbW85SDMzRUl0Z1hTUlh2Q1Q2UWtHTUlyTnNhY2pYdHd6UFc3aHU2SHBHQW9jYjd2cEFpUjhRcHdDeS1vYlRNX0VlZ0t6b2ItUktXX3BVMWFlNS1ZWTZxc2N6NVdmd3pnVVVuaXNBV2Q0RGZib2t6UXJuZGxDZDZsd0hCU2NJeUE4bUQwODdlUV84UGlkbGNBRU9HYkRrTUJ5cXFFejIzVU9rbC1UOVEtQXZwS2xpX01IYnFzRHV1UEstRmUya3gtMlJ4WXkxbnp2cVctZF85VFk3R2lpN2VJQXJSMGFodzdCZVJEWWhBSWVoRXYtbDgxRkt2UUhzT3dsaWM4Y0Q1cDdHeFdEMWo1a19rVFNDM3NaZ1BtcW1WcU8zR2VFN1h1T3B4R0lrQVVaS1Q5akZNdUUwcXYyaENQSUdiQXhlQVZMOGtnUHprRThSM3MwWXFTNUJYTU5yaXFuaFVRSEFGeVAzcDl3dnduNW9taURQaXo2clNYemlSSnRQQkhJWnBmZ3BGa3JtaXhxLWIwbVRQT1huVm80UzJHc2JoNW12eW5jbGg4TExtQmtSenhjZnRkV2lOS2tzZWg1Z21QUzE0ODR4alNSbFZmckppcFZORkRUZlhpOVpqRTFCUGRBd0pHRFhDY0dINElsbXRnWFNuWS12aXVVU0VOSlRsZmFsMmZvRG1jWGYwS1FsMTFVYXFDc3RMUzB0TFVKRlIwbE9JRU5GVWxSSlJrbERRVlJGTFMwdExTMEtUVWxKUkRacVEwTkJkRXRuUVhkSlFrRm5TVVVyVHpKcVZrUkJUa0puYTNGb2EybEhPWGN3UWtGUlZVWkJSRUl2VFZGemQwTlJXVVJXVVZGSFJYZEtWZ3BWZWtWVVRVSkZSMEV4VlVWRFFYZExWakpHZW1GSGJIVmFNMUoyWW1wRlVrMUJPRWRCTVZWRlFuZDNTVk15YkhsaE1uaG9ZbTFSZUVWNlFWSkNaMDVXQ2tKQmIwMURhMlIyWWpKa2MxcFRRa3BpYlUxNFJWUkJVRUpuVGxaQ1FYTk5RMFprY0ZwSFZqSmhWelZzVFZOQmQwaG5XVVJXVVZGRVJFSmtXR0ZYVW13S1pHMXNkVnBUUWs1aU1uaHpaVk5DUkZsWVRqQkpSV3hFVVZSQlpVWjNNSGhPUkVFMVRXcFpkMDFVVFRKTmVrcGhSbmN3ZUU1VVFUVk5hbGwzVFZSTk1ncE5la3BoVFVoemVFTjZRVXBDWjA1V1FrRlpWRUZzVmxSTlVrMTNSVkZaUkZaUlVVbEVRWEJZV1ZoT2IyRlhOVzVrUnpsMVRWSkZkMFIzV1VSV1VWRklDa1JCYUV4aFdFcHlZa2RHZFZwRVJWUk5Ra1ZIUVRGVlJVTm5kMHRTTWpsMldqSjRiRWxGYkhWWmVrVlNUVUU0UjBFeFZVVkRkM2RKVmpKc2ExcFlXbkFLWW0xVmVFaEVRV0ZDWjA1V1FrRk5UVVY2U1RCTmFtY3dUbFJWTVUxcWEzcE5SRWswVFdwak5FNXFRWGRuWjBWcFRVRXdSME5UY1VkVFNXSXpSRkZGUWdwQlVWVkJRVFJKUWtSM1FYZG5aMFZMUVc5SlFrRlJRMkpsTVdKeFMyNXpjWG9yUVhkTlZXMDRhVEptUzI5S1N6Y3JUa05XUjJkYVowTjFSa3A1TmxGdENqVTFOV3hrV0ZBeVFXSnBaekl6WW5oYVprOXNkM0JsY0ZGSE4yNTZRVVJxZGpKQ1lXMVdkMDFXU2xkNmR5dG1PV2hJT0hKUlZUVndZbTQxT0dOMVZGRUtUM1U1YWtVNVdFcGhhSEpxVEU1TmVsUlhjVTFXTUVwSmNtZENlRzEzUTJaMVVWTk9XQ3RLVVZsSmFGZHVRMnhrVjJoSVZFVldSVzR4YkVsR2MyUkRaZ3AyZG5CaWJqSkxXVWMzU0RJNVNHVTBNVWxSZDBJdmRIZzRlbTlsYzNKUWJ6VXdkMXBwZUVsNFZEQnBlSE5XVGxSc1NrTXlWazFaZEdaT1RXUnRTR0oyQ2k5TFYyeDZia2xSVVdkcFRucDVRa2N2ZURCRlRXTXZjbmRSVVdwRVMxcDViMVZhYjJ3M1pXVTVTSGd2UlVKcmFtMXdVM2t2Umk5R1JGWjJWM1p6V2pjS1YxZGlNRlExZDFwMGJGaGxPVGd4Tmk5aUwwbFpVbkJUUkVaQ2FrSkViM0Y1V0dJd1NWWXZkMHRGVW5wQlowMUNRVUZIYW1OcVFuZE5RWGRIUVRGVlpBcEZkMFZDTDNkUlEwMUJRWGREZDFsRVZsSXdVRUpCVVVSQloyVkJUVUpOUjBFeFZXUktVVkZOVFVGdlIwTkRjMGRCVVZWR1FuZE5RMDFDTUVkQk1WVmtDa1JuVVZkQ1FsSk1iMjV3ZVVkdFdDc3ZURnBYZDAwNVZrMUtjMEYzZUZaR1ZsUkJaa0puVGxaSVUwMUZSMFJCVjJkQ1UwUXliME5aUTFGU2QyaE1WRGtLV0Zka2JGRk1OVkZZU1dSM1JucEJUa0puYTNGb2EybEhPWGN3UWtGUlZVWkJRVTlEUVZGRlFXZEJiM05HZGxaMVVUbGhSMDVUYm0xRFQweHJlV1J0TVFwdU1HaGpiVXB5UlZweWExRk9WbmhVY0d4b1UxTXlibmRPVEhCM01XTTJjbWhWVGtJMVVqaGhSa1p1V0hCb2JFcENabUkwZFUxSmIweDFTbEpaWlcxVUNrbHNjbFZQYWxkVFdubzNSR1pGUzI5alVXTmthMGRFVDNGS2JGWnJjMWtyTUU0eEx6QXplVFo0TmlzNFJFeG5SMk5PZFRaV2VsWnBSbEJZV1ZGcU9HSUtSelF4UVM4d1ZqQTVNWEpMVVdkSWQzWjVPRFF6VlhGd01UWkpLMlZuVUVwSlJGZEtUa0UzV2sxYU56RXdWakp1YjNRNFoyVnRiVlVyT0V4WldHOWphQXBWTUZab1ltczNlVlZ6ZG5od1UxUlpTMUJYVWk5eU1FOXJiVlZQWWtwVE5HVkZPVE5qZVhaS2FHRXZNUzlRYzJ4amRWRk9kVzVvWVU5bU5sUTJhMHBNQ2xOek9GVTJXalUzU21KR1JHbFpaVGwxVm1wSmFYUTFRMVZvYTFKdlZ6TTNSVkJsYzBoeGRWRmpWRkY0TkVzNFZTODNkVzkwZGpaNGNFVnRhMmxCUFQwS0xTMHRMUzFGVGtRZ1EwVlNWRWxHU1VOQlZFVXRMUzB0TFFvdExTMHRMVUpGUjBsT0lFTkZVbFJKUmtsRFFWUkZMUzB0TFMwS1RVbEpSSHBVUTBOQmNsZG5RWGRKUWtGblNVSkJWRUZPUW1kcmNXaHJhVWM1ZHpCQ1FWRlZSa0ZFUWpsTlVYTjNRMUZaUkZaUlVVZEZkMHBXVlhwRlZBcE5Ra1ZIUVRGVlJVTkJkMHRXTWtaNllVZHNkVm96VW5aaWFrVlNUVUU0UjBFeFZVVkNkM2RKVXpKc2VXRXllR2hpYlZGNFJYcEJVa0puVGxaQ1FXOU5Da05yWkhaaU1tUnpXbE5DU21KdFRYaEZWRUZRUW1kT1ZrSkJjMDFEUm1Sd1drZFdNbUZYTld4TlVqUjNTRUZaUkZaUlVVUkVRbFpZWVZkU2JHUnRiSFVLV2xOQ1JGbFlUakJKUms0eFdXNUtkbUl6VVhkSWFHTk9UVlJSZDA1RVFUVk5WR014VG5wRk1GZG9ZMDVOVkd0M1RrUkJORTFVWXpGT2VrVXdWMnBDTHdwTlVYTjNRMUZaUkZaUlVVZEZkMHBXVlhwRlZFMUNSVWRCTVZWRlEwRjNTMVl5Um5waFIyeDFXak5TZG1KcVJWSk5RVGhIUVRGVlJVSjNkMGxUTW14NUNtRXllR2hpYlZGNFJYcEJVa0puVGxaQ1FXOU5RMnRrZG1JeVpITmFVMEpLWW0xTmVFVlVRVkJDWjA1V1FrRnpUVU5HWkhCYVIxWXlZVmMxYkUxVFFYY0tTR2RaUkZaUlVVUkVRbVJZWVZkU2JHUnRiSFZhVTBKT1lqSjRjMlZUUWtSWldFNHdTVVZzUkZGVVEwTkJVMGwzUkZGWlNrdHZXa2xvZG1OT1FWRkZRZ3BDVVVGRVoyZEZVRUZFUTBOQlVXOURaMmRGUWtGTVptOTNLMUZ6TTI1U1ZEaHJiVlppWkVoaFlWWmpUbWgxV0hSMFRHNXRZelU1YzNKVWRHdG9VVTFPQ2tORlUzWkhSMjFEY21Gc01GcEVaRWcwWldOdFIxUk5PRFIwUXpGb1JIcFlja2RQZFhoRVNXbzVkSGRWUlVWMVZtWXJhVmt4TTNGbFVYb3hiMmw1Y0hjS09YZzFSR05NY1d4d1drOTBhWFJUWm5KSlRWYzRNR2htZUdWRGJGSk1hRkF5WkdneGEwTlhURFI0ZUhNeWIyb3ZRMU4yUzBocmFtUmtaemx2Vm01elZncHVZM0J5U0ZCa1NYZHZia2RyZDI5NE9HNW5ibEpVTTNoRVZuUjFWbFJNZGxOaFJGZHlObGwzYTJaSmFFdzVkV3RMWW0xaVNYSjZUa00yWVV4d2FVbzFDaTlqSzFack5XRjZTVGh1UjAxSk4wRTJVaTl6S3k5WFNUTmFaSGxHYVd0SksydE1ibFE0Y1hVeGR6aHFVMHAxUTNCNlpFc3pWMEZGWkdSNlpVTmFhbE1LUm1sTlJXTkZNbHB1TUhGRFMwOWhLMm8xTWk5dlZYVnBkWFpYZVZWU05VODFORU5sWldwcGFIaDNhME5CZDBWQlFXRk9WMDFHVVhkRloxbEVWbEl3VkFwQlVVZ3ZRa0ZuZDBKblJVSXZkMGxDUVVSQlpFSm5UbFpJVVRSRlJtZFJWV2M1Y1VGdFFXdEZZMGxUTUM5V01XNWFWVU1yVlVaNVNHTkNZM2RJZDFsRUNsWlNNR3BDUW1kM1JtOUJWVFZzWmxGWGMzcHNkRFF5UkVGaFJsa3lSVll5ZWxreldVNU5TWGRFVVZsS1MyOWFTV2gyWTA1QlVVVkdRbEZCUkdkblJVSUtRVU52Vkc1ek5XMURSMnhVWlZwMlRVbzJSMFkyZGxwdE5tYzNlak5XZWpJclNEZGhhMkZZVUdRNFJGazRXVGR1WjA5aFEyWmxVMWh0ZEhoYUwxTXZiUXBtYms4NVpqVlZhazl4YW0xdVNGSktVVzlwUTJaSVEwRTRPVVprVTAxUmMyWTFiekZwVTNKa1pXZHpUVEpvVFZBemVFNVpibUpsVkhsYVRsWnBabU5xQ2tGS2JtVTBUMWhUWWt4SVJYVTNXR1Z5TVdKUFMwRjRTMmN5VldscU0yaElVSEZSVWxCRk0wRlRXRUU0VWt0WWJsRnVjbEJ5V0RSYU9YcHBXVk16TjFjS2EwVkZVMDVzUjJJNWRXTXJka0V4ZUZZek1HOVlja0ZXY3pSU01qbEVjSEpOYVc5VWNpOVBla3BSWjBVeWExUlpOelEwY1hablVUTlllWEZHU2tZMWVncFJSakkwV1ZnNFpGQTRha3B4UVdNNVExQnVXRmRXYzBkdVR6SldiV1JuWTFWUmQxbENablZpYVRKM2MxQlZPRTlLWWs0eGRVOUNMemxLWlVab016WkhDakpaWW5KbE9GRjFlbE5YUzIxd2JGQllaMDVuVVd0VlBRb3RMUzB0TFVWT1JDQkRSVkpVU1VaSlEwRlVSUzB0TFMwdENpMHRMUzB0UWtWSFNVNGdRMFZTVkVsR1NVTkJWRVV0TFMwdExRcE5TVWxFZWxSRFEwRnlWMmRCZDBsQ1FXZEpRa0pVUVU1Q1oydHhhR3RwUnpsM01FSkJVVlZHUVVSQ01VMVJjM2REVVZsRVZsRlJSMFYzU2xaVmVrVlVDazFDUlVkQk1WVkZRMEYzUzFFeVJuTmhWMXAyWTIwMWNGbFVSVmROUWxGSFFURlZSVUozZDA1VVZ6a3hZbTVTYUdGWE5HZFdiV3hzWkhwRlZFMUNSVWNLUVRGVlJVTm5kMHRTTWpsMldqSjRiRWxGYkhWWmVrVk9UVUZ6UjBFeFZVVkRkM2RGVVRKR2VtUkVSVlpOUWsxSFFURlZSVUYzZDAxUk1rWjZaRU5DVXdwaU1qa3dTVVZPUWsxQ05GaEVWRVV3VFVSUmQwOVVRVEZOVkVsNVRWWnZXRVJVUlRWTlJGRjNUMVJCTVUxVVNYbE5WbTkzWmxSRlRFMUJhMGRCTVZWRkNrSm9UVU5XVmsxNFJYcEJVa0puVGxaQ1FXZE5RMnhrYUdNeWFIQmliV1F3WWpJMGVFVlVRVkJDWjA1V1FrRmpUVU5GZEhCamJYUnpXVmMxYTAxU1RYY0tSVkZaUkZaUlVVdEVRWEJJWWpJNWJtSkhWV2RUVnpWcVRWSkZkMFIzV1VSV1VWRk1SRUZvV0dGWFVteGtiV3gxV2xSRlpVMUNkMGRCTVZWRlFYZDNWZ3BXTW14cldsaGFjR0p0VldkUk1rWjZaRU5DVkdSWFNubGlNamt3VFVsSlFrbHFRVTVDWjJ0eGFHdHBSemwzTUVKQlVVVkdRVUZQUTBGUk9FRk5TVWxDQ2tOblMwTkJVVVZCY0RSMFNFUTNSMU55VmtST1ZHTlhkRVEwWVZCT00zTXlTMEpXWWtOUlQwSkNUamhWU1dzd1prVXhXa3hJUkU5RFRtaGxURlJyYTJRS0x5czRXVXhpWkZVMmVuSTNNMUphTmxSRWVtOUhOVkZKT1RoRVdDOVRNMk5QVm1rM1UyNXdhR05NTTNCaVduTklUMGs0VUdSRlZUSjBRbVZOUjFaRmRnb3ZlbTlLUW1KWFNVNW9TREo0WlhsUVJIZ3llbGRyUzNkcWVYbDViblV5Tm1OYWNVSmhUbXRWYW1OMmMwRTRhMU52WTBaTlFYaGthVmdyWkRkVVpqaDFDbEpUY1ZvemIyVlhNM0o0ZURKMVVHeFdiRGRXU2tadVpWVlZWa1ZSVkZSbVpUSTRORzFuVG5sNVNEZHViRUpMT1cxelFUTlBTVlUwYTNvNGFXcEdUa1FLYUVSM05EQTNWVXM1TUdORVZXUkxTbE15TVM5emEyTkpXRnBaTkRNNVEwZ3dlazF4VlRGTVNtMDFibEZuTTJZclJHSXlaVmx4VFdodVRYVTJkR1phWXdvdmRuaFRPR1l2TWsxU1dFbDVhelp6TTBseksyaEJTRTFJZG01cVZIZEpSRUZSUVVKdk1rRjNXR3BCVUVKblRsWklVazFGUTBSQlIwRlJTQzlCWjBWQ0NrMUNNRWRCTVZWa1JHZFJWMEpDVkcxV09VSmhlazlYTTJwWlRVSnZWbXBaVWxoaVRtcGtaekIzYWtGbVFtZE9Wa2hUVFVWSFJFRlhaMEpTT0cxb05Ua0tNek5zVlhaT1prMVljM0ZhYUd0V05WcFlVVzlIVkVGTVFtZE9Wa2hST0VWQ1FVMURRVkZaZDBSUldVcExiMXBKYUhaalRrRlJSVVpDVVVGRVoyZEZRZ3BCU25rNWRWZEtjMGxCVWtacGFVeGhjR0ZUVFdWRVpIZHJjQ3RVY1VSUlRHRnlSeTk2VlZsR05sWk1jRE42ZW5kMFdWTnhjVE5KT1dkRk0wUnBiVXAwQ2twc2RrZEhZVGhIWlZaMFluRk9iMGRFWVVkQmRsWm9OMmMwUTIxRGJUUnBhRmxWTDNsdGRsTTVVMkk0Vnl0T2JuSnNkMWREZVdFdmMzTmhSMkZSVDFNS1oxcGFkRWx5U1hsVGRWQXpMek0zVDNZeVdtOHhMelZTVlZKbVlTdFpLMjR6V1VwVE5TOHZPR3h1YkZsR1NGRTJjbmRvUVdNMlZIaHhZU3MwTlNzMVZncDRVM1o1UzNveVpEYzNTV2xhVkhrek0wbFNPR0o1ZFZkSlpHNUpWRkJTZVRGSVJubFphRGxOV2s1blNUVXdZbU5ITmtWaE5IQnVkMFYzVUhKUk5YVkJDbmd3YzFJeGMwbG9SMWd5TVdwT2VHWjJhMmxVTlRsQ2NVczVRMFZKWm5ORE5YZFBVMk5vTWpkeFozSlZTa3N5ZEVOUVFWVmhhRWw2ZVhVd2QyWTFNRklLVlV0eWRHTnFTVk5IYkhaTVp6UklaREJoZG5wc1IwVTlDaTB0TFMwdFJVNUVJRU5GVWxSSlJrbERRVlJGTFMwdExTMEtMUzB0TFMxQ1JVZEpUaUJEUlZKVVNVWkpRMEZVUlMwdExTMHRDazFKU1VSNFZFTkRRWEV5WjBGM1NVSkJaMGxDUVdwQlRrSm5hM0ZvYTJsSE9YY3dRa0ZSVlVaQlJFSXhUVkZ6ZDBOUldVUldVVkZIUlhkS1ZsVjZSVlFLVFVKRlIwRXhWVVZEUVhkTFVUSkdjMkZYV25aamJUVndXVlJGVjAxQ1VVZEJNVlZGUW5kM1RsUlhPVEZpYmxKb1lWYzBaMVp0Ykd4a2VrVlVUVUpGUndwQk1WVkZRMmQzUzFJeU9YWmFNbmhzU1VWc2RWbDZSVTVOUVhOSFFURlZSVU4zZDBWUk1rWjZaRVJGVmsxQ1RVZEJNVlZGUVhkM1RWRXlSbnBrUTBKVENtSXlPVEJKUlU1Q1RVSTBXRVJVUlRCTlJGRjNUV3BGTTAxNlVYbE9iRzlZUkZSTk1FMUVUWGxQUkVVelRYcFJlVTVzYjNka1ZFVk1UVUZyUjBFeFZVVUtRbWhOUTFaV1RYaEZla0ZTUW1kT1ZrSkJaMDFEYTA1b1lrZHNiV0l6U25WaFYwVjRSbXBCVlVKblRsWkNRV05OUkZVeGRtUlhOVEJaVjJ4MVNVWmFjQXBhV0dONFJYcEJVa0puVGxaQ1FXOU5RMnRrZG1JeVpITmFVMEpLWW0xTmVFUlVRVXhDWjA1V1FrRnpUVUpGVG1oak0xRjRSbFJCVkVKblRsWkNRVTFOQ2tSRlRtaGpNMUZuVlcwNWRtUkRRa1JSVkVORFFWTkpkMFJSV1VwTGIxcEphSFpqVGtGUlJVSkNVVUZFWjJkRlVFRkVRME5CVVc5RFoyZEZRa0ZNY2xvS1dsb3pZVTlrVUVKa0wySlZNRXMyVUZkQmFHOVBWWEZXTjFoRVVDOVlhMGx4WVhKc05tSnBia3hoUW01U05IRmxlV001ZDNOM1YwaGhVa2h6WTBwcFdBcDNLMkpFZHl0MU9YaHlRVGt2UlM5Q1dHcHBaakp6T1hwTlFWcGlaVlJtUWxodmVVaFNOVk5oVVZwSmNURndXRVZqVm5kdVdGRnBlR2ROWVZOMlVuWnFDbEZhWldnM1NGZG1WbG8wSzI0ME9HTjRNbFpyUWpsUGVteHhSVVZ1TlVoRk0yZHdOMkpPYmtsM1NHZDRiMEpzUTNGbGFVUTBPRGM0T0dNM1EweHBVa2NLYkZGcldubHpRa2R6ZFZWQ2RYUmtVRGczTHpKaFlUSmFRbEJ4WjBKNmEwODFkRGxTVW5kbVFUVkxiR05UTlZSR1REZFBaMDFJTDI1c1YzVjVjbnBKVGdvNFdYcFdZbU4wTjFJMlkwbHhPSE51YnpBelVGTnNjbmhDWkVnMFdYTlZVVXR1VW5CeGRWcE1iSFoxWWpKSFVHdFhSMkpVY2xsd2RTOHpkR1VyWVZaWENraHBNa05OVm5aM05HbFViVkZWYjJaeWFFMURRWGRGUVVGaFRtZE5SalIzUkhkWlJGWlNNRlJDUVdkM1FtZEZRaTkzU1VKQmFrRmtRbWRPVmtoUk5FVUtSbWRSVldaS2IyVm1aRGsxVmt4NldIcEdOMHR0V1ZwR1pWZFdNRXRDYTNkSWQxbEVWbEl3YWtKQ1ozZEdiMEZWWmtwdlpXWmtPVFZXVEhwWWVrWTNTd3B0V1ZwR1pWZFdNRXRDYTNkRGQxbEVWbEl3VUVKQlVVUkJaMFZIVFVFd1IwTlRjVWRUU1dJelJGRkZRa0pSVlVGQk5FbENRVkZEUVRsR2NqZFFVMmRhQ2xWVFJGZ3hVSE5UYkRCd2JEaHNaekZyYm1OM1lYWklXSFJzUldGbU5YSk9lRE56UkZGeE1WWmhaME4yT0U5RlIzZHlNWEpsU0ZoaUwydEZVbFV3YnpVS2RUVnZObmhzYXpCTWVYZDZORGRNVjFoSUwyUmxUM1I0VjNwdVlXYzFSRVpOWlVrdlNTc3ZZVFo1YzNSa01UZGxkekJRVTNsWGRGcG5jM0pXTjJaeGFBcGFSblpNT0ZFd1lWbDFSMk0yUzJOWlkxQkNaa1kxWWpRM1dXSmljbWd6WjNwNk5XUk1kVFJYWWxwVmNsQlFNbGc0ZDFaaFNrZG9UazlpWWpRMVJtazJDamxsUVcxbFJraEdWekV4VDBObFZuTlNOSFEyVjJrMlNsVXJZazFPYkhOdFVGQm9lVkYzUzBNd2FYWk9PRTVQYWpkQ1RTdFZkRmRFVUZGbVkwaFZUbXdLWldwTlEwRmhVRTkwT1ZwblZWUnpTbmRwVDB0TmRqWlpSMWRDYVdzMFdFNU9SV0ppTVZOTlVHVmtjRE5CUTI5RFlsbE9XWHBuVGpOT1pVZHFTVXBRUXdwVGNVdHJVbWg0TVV4Q09VNEtMUzB0TFMxRlRrUWdRMFZTVkVsR1NVTkJWRVV0TFMwdExRb2lCTFdDUW9NU0lId01Kd3dNb1ZTalJYTVdqWldmUE1xVE5WbkF4TnpIdW43djRWTjd0MGxwIg0KfQ", 0);

                            MediaDrm.Certificate cert = MediaDrm.provideCertificateResponse(drm, dataResponse);

                            byte[] sessionId = drm.openSession();

                            byte[] msg = Base64.decode("MCEwCQYFKw4DAhoFAAQUY4CXkGujT5Aed1/IIuq5O8c8xWE=", 0);
                            byte ret[] = MediaDrm.signRSA(drm, sessionId, "PKCS1-BlockType1", cert.getWrappedPrivateKey(), msg);

                            drm.closeSession(sessionId);

                            MediaLogger.instance().log("Signed RSA message", Base64.encodeToString(ret, 0));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    client.close();

                }
            }
        }).start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
