package com.example.udpp2p.message;

import com.example.udpp2p.MessageType;
import com.example.udpp2p.STUN.STUN_NetType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;

public class ACKMessage extends BaseMessage{

   /**
    * I use this values for start the connection
    * Example:
    *
    * // Start message for start the connection
    * User 1:
    *
    * Send SYN:
    * {
    * "MessageType": "MessageType.SYN",
    * "ID" : "**Random ID**",
    * "DateTime" : "2010-10-20 10:10:10"
    * }
    *
    * User 2:
    *
    * Receive the Message
    *
    * Send SYN_ACK:
    * {
    * "MessageType": "MessageType.SYN_ACK",
    * "ID" : "**Random ID**",
    * "DateTime" : "2010-10-20 10:10:12",
    * "SourceLocalEPs" :
    * [{
    *     "192.168.3.11:12356"
    * }];
    * "SourcePublicEP" : "11.10.1.11:12356"
    * }
    *
    * User 1:
    *
    * Receive the Message
    *
    * Send ACK:
    * {
    * "MessageType": "MessageType.ACK",
    * "ID" : "**Random ID**",
    * "DateTime" : "2010-10-20 10:10:14",
    * "SourceLocalEPs" :
    * [{
    *     "192.168.1.11:12356"
    * }];
    * "SourcePublicEP" : "10.10.1.11:12356"
    * }
    *
    * @param netType is the NetType of the source EndPoint
    * @param sourceLocalEPs is the list of source Local EndPoint (IP:Port) of the sender of the message. This an ArrayList because the device can have multiple local ip (for example i can have 192.168.1.11 on WIFI and 192.168.2.3 on ethernet)
    * @param sourcePublicEP is the source Public EndPoint (IP:Port) of the sender of the message
    */

   // TODO I NEED TO CHECK IF I REALLY NEED TO SEND THIS INFORMATION
   public STUN_NetType netType;
   public ArrayList<InetSocketAddress> sourceLocalEPs;
   public InetSocketAddress sourcePublicEP;

   public ACKMessage(MessageType type, STUN_NetType netType, ArrayList<InetSocketAddress> sourceLocalEPs, InetSocketAddress sourcePublicEP) {
      super(type);
      this.netType = netType;
      this.sourceLocalEPs = sourceLocalEPs;
      this.sourcePublicEP = sourcePublicEP;
   }

   private ACKMessage(MessageType type, UUID ID, LocalDateTime dateTime, STUN_NetType netType, ArrayList<InetSocketAddress> sourceLocalEPs, InetSocketAddress sourcePublicEP)
   {
      super(type, ID, dateTime);
      this.netType = netType;
      this.sourceLocalEPs = sourceLocalEPs;
      this.sourcePublicEP = sourcePublicEP;
   }

   @Override
   public String toJSONString() {

      /// Message Scheme
      /// *********************************************************************************************************************************************************************************
      /// ||          MessageType         ||          ID          ||          dateTime            ||        NetType        ||       [sourceLocalEP]        ||       sourcePublicEP       ||
      /// *********************************************************************************************************************************************************************************

      try {
         JSONObject obj = new JSONObject();
         obj.put("MessageType", type.toString());
         obj.put("ID", ID.toString());
         obj.put("DateTime", dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
         obj.put("NetType", netType.toString());

         JSONArray localEPs = new JSONArray();
         for(InetSocketAddress ep : sourceLocalEPs)
         {
            localEPs.put(ep);
         }
         obj.put("SourceLocalEPs", localEPs);

         obj.put("SourcePublicEP", sourcePublicEP.toString());

         return obj.toString();
      } catch (JSONException e) {
         e.printStackTrace();
      }
      return null;
   }

   public static ACKMessage Parse(String message) {
      try {
         JSONObject obj = new JSONObject(message);

         MessageType type = MessageType.valueOf(obj.getString("MessageType"));
         UUID ID = UUID.fromString(obj.getString("ID"));
         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
         LocalDateTime dateTime = LocalDateTime.parse(obj.getString("DateTime"), formatter);
         STUN_NetType netType = STUN_NetType.valueOf(obj.getString("NetType"));

         ArrayList<InetSocketAddress> sourceLocalEPs = new ArrayList<InetSocketAddress>();
         JSONArray localEPs = obj.getJSONArray("SourceLocalEPs");
         for(int i = 0; i < localEPs.length(); i++)
         {
            String[] splitSourceLocalEP = localEPs.getString(i).split(":");
            sourceLocalEPs.add(new InetSocketAddress(splitSourceLocalEP[0], Integer.parseInt(splitSourceLocalEP[1])));
         }

         String[] splitSourcePublicEP = obj.getString("SourcePublicEP").split(":");
         InetSocketAddress sourcePublicEP = new InetSocketAddress(splitSourcePublicEP[0], Integer.parseInt(splitSourcePublicEP[1]));


         return new ACKMessage(type, ID, dateTime, netType, sourceLocalEPs, sourcePublicEP);
      } catch (JSONException e) {
         e.printStackTrace();
      }
      return null;
   }
}
