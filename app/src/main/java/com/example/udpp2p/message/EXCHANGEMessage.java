package com.example.udpp2p.message;

import com.example.udpp2p.MessageType;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class EXCHANGEMessage extends  BaseMessage{

   /**
    * Message for Exchange the EndPoint for start the connection through the firewall.
    * The user send the message to all the peers connected, the message will be spread to all the network until the destination Peer receive it and send back the response (EXCHANGE_ACK)
    *
    * Example:
    * {
    * "MessageType": "MessageType.EXCHANGE",
    * "ID" : "**Random ID**",
    * "DateTime" : "2010-10-20 10:10:12",
    * "SourcePeerLocalEP" : "192.168.3.11:12356",
    * "SourcePeerPublicEP" : "11.10.1.11:12356",
    * "DestinationPeerLocalEP" : "192.168.3.11:12356"
    * "DestinationPeerPublicEP" : "12.10.1.11:12356"
    * }
    *
    * @param sourcePeerLocalEP is the sender Local EndPoint (IP:Port) taken from the STUN
    * @param sourcePeerPublicEP is the sender Public EndPoint (IP:Port) taken from the STUN
    * @param destinationPeerLocalEP is the destination Local EndPoint (IP:Port) taken from the superPeers.json file
    * @param destinationPeerPublicEP is the destination Public EndPoint (IP:Port) taken from the superPeers.json file
    */

   /// TODO I can remove the sourcePeerLocalEP because if i'm in the same network there is no firewall so i can communicate directly without hole punching,
   public InetSocketAddress sourcePeerLocalEP;
   public InetSocketAddress sourcePeerPublicEP;
   public InetSocketAddress destinationPeerLocalEP;
   public InetSocketAddress destinationPeerPublicEP;

   public EXCHANGEMessage(MessageType type, InetSocketAddress sourcePeerLocalEP, InetSocketAddress sourcePeerPublicEP, InetSocketAddress destinationPeerLocalEP, InetSocketAddress destinationPeerPublicEP) {
      super(type);

      this.sourcePeerLocalEP = sourcePeerLocalEP;
      this.sourcePeerPublicEP = sourcePeerPublicEP;

      this.destinationPeerLocalEP = destinationPeerLocalEP;
      this.destinationPeerPublicEP = destinationPeerPublicEP;
   }

   private EXCHANGEMessage(MessageType type, UUID ID, LocalDateTime dateTime, InetSocketAddress sourcePeerLocalEP, InetSocketAddress sourcePeerPublicEP, InetSocketAddress destinationPeerLocalEP, InetSocketAddress destinationPeerPublicEP)
   {
      super(type, ID, dateTime);

      this.sourcePeerLocalEP = sourcePeerLocalEP;
      this.sourcePeerPublicEP = sourcePeerPublicEP;

      this.destinationPeerLocalEP = destinationPeerLocalEP;
      this.destinationPeerPublicEP = destinationPeerPublicEP;
   }

   @Override
   public String toJSONString() {

      /// Message Scheme
      /// ******************************************************************************************************************************************************************************************************************************************************************************
      /// ||          MessageType         ||          ID          ||          dateTime            ||        NetType        ||       SourceLocalEndPoint       ||        SourcePublicEndPoint       ||       DestinationLocalEndPoint        ||       DestinationPublicEndPoint        ||
      /// ******************************************************************************************************************************************************************************************************************************************************************************

      try {
         JSONObject obj = new JSONObject();
         obj.put("MessageType", type.toString());
         obj.put("ID", ID.toString());
         obj.put("DateTime", dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
         obj.put("SourcePeerLocalEP", sourcePeerLocalEP.toString());
         obj.put("SourcePeerPublicEP", sourcePeerPublicEP.toString());
         obj.put("DestinationPeerLocalEP", destinationPeerLocalEP.toString());
         obj.put("DestinationPeerPublicEP", destinationPeerPublicEP.toString());

         return obj.toString();
      } catch (JSONException e) {
         e.printStackTrace();
      }
      return null;
   }

   public static EXCHANGEMessage Parse(String message) {
      try {
         JSONObject obj = new JSONObject(message);

         MessageType type = MessageType.valueOf(obj.getString("MessageType"));

         UUID ID = UUID.fromString(obj.getString("ID"));

         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
         LocalDateTime dateTime = LocalDateTime.parse(obj.getString("DateTime"), formatter);

         String[] splitSourcePeerLocalEP = obj.getString("SourcePeerLocalEP").split(":");
         InetSocketAddress sourcePeerLocalEP = new InetSocketAddress(splitSourcePeerLocalEP[0], Integer.parseInt(splitSourcePeerLocalEP[1]));

         String[] splitSourcePeerPublicEP = obj.getString("SourcePeerPublicEP").split(":");
         InetSocketAddress sourcePeerPublicEP = new InetSocketAddress(splitSourcePeerPublicEP[0], Integer.parseInt(splitSourcePeerPublicEP[1]));

         String[] splitDestinationPeerLocalEP = obj.getString("DestinationPeerLocalEP").split(":");
         InetSocketAddress destinationPeerLocalEP = new InetSocketAddress(splitDestinationPeerLocalEP[0], Integer.parseInt(splitDestinationPeerLocalEP[1]));

         String[] splitDestinationPeerPublicEP = obj.getString("DestinationPeerPublicEP").split(":");
         InetSocketAddress destinationPeerPublicEP = new InetSocketAddress(splitDestinationPeerPublicEP[0], Integer.parseInt(splitDestinationPeerPublicEP[1]));

         return new EXCHANGEMessage(type, ID, dateTime, sourcePeerLocalEP, sourcePeerPublicEP, destinationPeerLocalEP, destinationPeerPublicEP);
      } catch (JSONException e) {
         e.printStackTrace();
      }
      return null;
   }
}
