package com.homersp.nsd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

/**
 * Created by homer on 01/10/14.
 */
public class JmDNSHelper {
    private static final String TAG = "JmDNSHelper";

    private static JmDNS mDNS = null;
    private static WifiManager.MulticastLock mMultiCastLock;

    private static List<JmDNSItem> mItems = new ArrayList<JmDNSItem>();

    private static final Object mRegisterLock = new Object();

    public static void create(Context context) {
        context.registerReceiver(mWifiStateChangedReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
    }
    public static void destroy(Context context) {
        try {
            context.unregisterReceiver(mWifiStateChangedReceiver);
        } catch(IllegalArgumentException e) {

        }
    }

    public static void addService(final Context context, final JmDNSItem item) {
        if(Build.VERSION.SDK_INT < 19) {
            if (mMultiCastLock == null) {
                Log.d(TAG, "Attempting to lock multicast");

                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                mMultiCastLock = wifiManager.createMulticastLock("CastReceiver");
                mMultiCastLock.setReferenceCounted(true);
                mMultiCastLock.acquire();
            }
        }
        if(mDNS == null) {
            InetAddress addr = null;
            try {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

                // Convert little-endian to big-endian if needed
                if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                    ipAddress = Integer.reverseBytes(ipAddress);
                }

                byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

                addr = InetAddress.getByAddress(ipByteArray);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }

            if(addr == null) {
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                addr = inetAddress;
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }

            try {
                if(addr == null) {
                    Log.d(TAG, "addr == null!");
                } else {
                    Log.d(TAG, "Creating JmDNS using address: " + addr.toString());
                    mDNS = JmDNS.create(addr, addr.getHostName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /*} else {
            if(mDNS == null) {
                try {
                    mDNS = JmDNS.create();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }*/

        new Thread(new JmDNSRunnable(item)).start();
    }
    public static void addService(final Context context, final CountDownLatch latch, final String serviceType, final String serviceName, int port, final String[] strValues) {
        HashMap<String, byte[]> values = new HashMap<String, byte[]>();
        for(int i = 0; i < strValues.length; i+=2) {
            try {
                values.put(strValues[i], strValues[i+1].getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        JmDNSItem item = new JmDNSItem();
        item.Latch = latch;
        item.Type = serviceType;
        item.Name = serviceName;
        item.Port = port;
        item.Values = values;

        addService(context, item);
    }

    public static void destroyAllServices() {
        Log.d(TAG, "destroy");
        synchronized (mRegisterLock) {
            if (mDNS != null) {
                mDNS.unregisterAllServices();
                try {
                    mDNS.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mDNS = null;
            }
            if (mMultiCastLock != null) {
                mMultiCastLock.release();
                mMultiCastLock = null;
            }
        }
    }


    private static BroadcastReceiver mWifiStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int size;
            synchronized (mItems) {
                size = mItems.size();
            }

            if(size > 0) {
                Log.d(TAG, "WIFI_STATE_CHANGED: " + intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0));
                destroyAllServices();

                if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0) == WifiManager.WIFI_STATE_ENABLED) {
                    Log.d(TAG, "Re-enabling services due to wifi state changed " + size);

                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            List<JmDNSItem> items = new ArrayList<JmDNSItem>();
                            synchronized (mItems) {
                                for (JmDNSItem item : mItems)
                                    items.add(item);

                                mItems.clear();
                            }
                            for (JmDNSItem item : items) {
                                Log.d(TAG, "Re-enabling " + item.Name);
                                addService(context, item);
                            }
                        }
                    }).start();
                }
            }
        }
    };

    private static class JmDNSItem {
        private CountDownLatch Latch;
        private String Type;
        private String Name;
        private int Port;
        private HashMap<String, byte[]> Values;
    }

    private static class JmDNSRunnable implements Runnable {
        private JmDNSItem mItem = new JmDNSItem();

        public JmDNSRunnable(JmDNSItem item) {
            mItem = item;
        }
        public JmDNSRunnable(CountDownLatch latch, String type, String name, int port, HashMap<String, byte[]> values) {
            mItem.Latch = latch;
            mItem.Type = type;
            mItem.Name = name;
            mItem.Port = port;
            mItem.Values = values;
        }

        @Override
        public void run() {
            synchronized (mRegisterLock) {
                if(mDNS == null)
                    return;

                if (mItem.Type.endsWith("."))
                    mItem.Type += "local";

                Log.d(TAG, "Type: " + mItem.Type);
                Log.d(TAG, "Name: " + mItem.Name);
                Log.d(TAG, "Port: " + mItem.Port);

                //mItem.Values.put("md", Build.MODEL.getBytes());

                for (Map.Entry<String, byte[]> value : mItem.Values.entrySet()) {
                    Log.d(TAG, "Key: " + value.getKey() + ", value: " + new String(value.getValue()));
                }

                try {
                    ServiceInfo deviceService = ServiceInfo.create(mItem.Type, mItem.Name, mItem.Port, 0, 0, mItem.Values);
                    mDNS.registerService(deviceService);
                    mItem.Latch.countDown();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                synchronized (mItems) {
                    mItems.add(mItem);
                }
            }
        }
    }
}
