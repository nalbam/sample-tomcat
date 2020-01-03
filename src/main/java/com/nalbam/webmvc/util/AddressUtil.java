package com.nalbam.webmvc.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class AddressUtil {

    public static String getAddress() {
        try {
            InetAddress ip;
            for (final Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e
                    .hasMoreElements();) {
                for (final Enumeration<InetAddress> a = e.nextElement().getInetAddresses(); a.hasMoreElements();) {
                    ip = a.nextElement();
                    if (!ip.isLoopbackAddress() && !ip.isLinkLocalAddress() && ip.isSiteLocalAddress()) {
                        return ip.getHostAddress();
                    }
                }
            }
            ip = InetAddress.getLocalHost();
            return ip.getHostAddress();
        } catch (final Exception e) {
            // log.error(e.getMessage());
            // e.printStackTrace();
        }
        return "";
    }

}
