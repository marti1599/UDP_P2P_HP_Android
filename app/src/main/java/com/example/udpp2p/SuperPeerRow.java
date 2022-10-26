package com.example.udpp2p;

import com.example.udpp2p.STUN.STUN_NetType;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;

public class SuperPeerRow {
   /**
    * SuperPeerRow information
    *
    * @param LocalEndPoint is the Local EndPoint of the Peer that he get when connected to a STUN server
    * @param PublicEndPoint is the Public EndPoint of the Peer that he get when connected to a STUN server
    * @param LastConnectionDateTime is the DateTime in the format yyyy-MM-dd HH:mm:ss of the last connection with this Peer
    * @param NetType is the NetType of the Peer that he get when connected to a STUN server
    */
   public InetSocketAddress LocalEndPoint;
   public InetSocketAddress PublicEndPoint;
   public LocalDateTime LastConnectionDateTime;
   public STUN_NetType NetType;

   public SuperPeerRow(InetSocketAddress LocalEndPoint, InetSocketAddress PublicEndPoint, LocalDateTime LastConnectionDateTime, STUN_NetType NetType)
   {
      this.LocalEndPoint = LocalEndPoint;
      this.PublicEndPoint = PublicEndPoint;
      this.LastConnectionDateTime = LastConnectionDateTime;
      this.NetType = NetType;
   }
}
