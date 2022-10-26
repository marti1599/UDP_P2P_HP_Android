package com.example.udpp2p;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

class Utility {
   /**
    * Get the Local IPs of the machine
    * @return a list of String that represent the Local IPs
    */
   public static ArrayList<String> GetLocalIPAddress()
   {
      ArrayList<String> IPs = new ArrayList<String>();
      try {
         Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
         while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            // filters out 127.0.0.1 and inactive interfaces
            if (iface.isLoopback() || !iface.isUp())
               continue;

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while(addresses.hasMoreElements()) {
               InetAddress addr = addresses.nextElement();
               if (!(addr instanceof Inet4Address)) {
                  // It's not ipv4
                  continue;
               }
               IPs.add(addr.toString().substring(1));
            }
         }

      } catch (SocketException e) {
         throw new RuntimeException(e);
      }

      return IPs;
   }
}
