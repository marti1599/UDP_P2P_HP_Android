package com.example.udpp2p.message;

import com.example.udpp2p.MessageType;
import com.example.udpp2p.STUN.STUN_NetType;
import com.example.udpp2p.SuperPeerRow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;

public class SUPERPEERJSON_RESPONSEMessage extends  BaseMessage{

   /**
    * Send a response to the Message SUPERPEERJSON_REQUESTMessage
    * @param superPeerRows is the list of SuperPeerRows that i will get as a response
    */
   public ArrayList<SuperPeerRow> superPeerRows;
   public SUPERPEERJSON_RESPONSEMessage(MessageType type, ArrayList<SuperPeerRow> superPeerRows) {
      super(type);
      this.superPeerRows = superPeerRows;
   }

   private SUPERPEERJSON_RESPONSEMessage(MessageType type, UUID ID, LocalDateTime dateTime, ArrayList<SuperPeerRow> superPeerRows)
   {
      super(type, ID, dateTime);
      this.superPeerRows = superPeerRows;
   }

   @Override
   public String toJSONString() {
      /// Message Scheme
      /// ****************************************************************************************************************************************************
      /// ||          MessageType         ||          ID          ||          dateTime            ||        NetType        ||       [superPeerJSON]         ||
      /// ****************************************************************************************************************************************************

      try {
         JSONObject obj = new JSONObject();
         obj.put("MessageType", type.toString());
         obj.put("ID", ID.toString());
         obj.put("DateTime", dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

         JSONArray rows = new JSONArray();
         for (SuperPeerRow row : superPeerRows) {
            JSONObject obj_row = new JSONObject();
            obj_row.put("LocalIP", row.LocalEndPoint.getAddress().toString());
            obj_row.put("LocalPort", row.LocalEndPoint.getPort());
            obj_row.put("PublicIP", row.PublicEndPoint.getAddress().toString());
            obj_row.put("PublicPort", row.PublicEndPoint.getPort());
            obj_row.put("LastConnectionDateTime", row.LastConnectionDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            obj_row.put("NetType", row.NetType.toString());
            rows.put(obj_row);
         }

         obj.put("superPeerJSON", rows);


         return obj.toString();
      } catch (JSONException e) {
         e.printStackTrace();
      }
      return null;
   }

   public static SUPERPEERJSON_RESPONSEMessage Parse(String message) {
      try {
         JSONObject obj = new JSONObject(message);

         MessageType type = MessageType.valueOf(obj.getString("MessageType"));

         UUID ID = UUID.fromString(obj.getString("ID"));

         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
         LocalDateTime dateTime = LocalDateTime.parse(obj.getString("DateTime"), formatter);

         JSONArray rows = obj.getJSONArray("superPeerJSON");
         ArrayList<SuperPeerRow> superPeerRows = new ArrayList<>();
         for(int n = 0; n < rows.length(); n++)
         {
            JSONObject obj_row = rows.getJSONObject(n);
            InetSocketAddress LocalEndPoint = new InetSocketAddress(obj_row.getString("LocalIP"), obj_row.getInt("LocalPort"));
            InetSocketAddress PublicEndPoint = new InetSocketAddress(obj_row.getString("PublicIP"), obj_row.getInt("PublicPort"));

            DateTimeFormatter formatterLastConnectionDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime LastConnectionDateTime = LocalDateTime.parse(obj_row.getString("LastConnectionDateTime"), formatterLastConnectionDateTime);

            STUN_NetType netType = STUN_NetType.valueOf(obj_row.getString("NetType"));

            superPeerRows.add(new SuperPeerRow(LocalEndPoint, PublicEndPoint, LastConnectionDateTime, netType));
         }

         return new SUPERPEERJSON_RESPONSEMessage(type, ID, dateTime, superPeerRows);
      } catch (JSONException e) {
         e.printStackTrace();
      }
      return null;
   }
}
