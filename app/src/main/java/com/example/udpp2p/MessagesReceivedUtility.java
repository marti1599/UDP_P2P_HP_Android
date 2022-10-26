package com.example.udpp2p;

import com.example.udpp2p.message.BaseMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class MessagesReceivedUtility {

   static String defaultFileName = "messagesReceived.json";

   /**
    * Add message to the list of received messages file (messagesReceived.json)
    *
    * @param message Message to check and add
    * @return return true if is already received, return false if is a new message
    */
   public static boolean CheckIfAlreadyReceivedMessage(BaseMessage message) {
      // TODO Start a new thread that wait until the file is accessible (for create something like a queue)
      // TODO When the file is accessible write the new message line
      // TODO Check the file size, if is more than N i need to parse it

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

      JSONArray m_jArry = new JSONArray();

      JSONArray jArrayRead = ReadFile();

      if (jArrayRead != null) {
         m_jArry = jArrayRead;
      }

      /// Scan the array for check if i already have this ID
      for (int i = 0; i < m_jArry.length(); i++) {
         /// Check if the file contains the received message ID and DateTime
         try {
            if (m_jArry.getJSONObject(i).get("ID").toString().equals(message.ID.toString()) && LocalDateTime.parse(m_jArry.getJSONObject(i).get("DateTime").toString(), formatter).equals(message.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))) {
               return true;
            }
         } catch (JSONException e) {
            e.printStackTrace();
         }
      }

      AddMessageToFile(m_jArry, message);

      return false;
   }

   public static JSONArray ReadFile() {
      /// Read the messages received on the file
      try {
         JSONArray m_jArry = new JSONArray();
         JSONObject objRoot = new JSONObject();
         if (new File(MainActivity.defaultFilesPath + "/" + defaultFileName).exists()) {
            InputStream is = new FileInputStream(MainActivity.defaultFilesPath + "/" + defaultFileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            objRoot = new JSONObject(json);
            m_jArry = objRoot.getJSONArray("root");

            return m_jArry;
         }
      } catch (JSONException | IOException e) {
         e.printStackTrace();
      }

      return null;
   }

   public static boolean AddMessageToFile(JSONArray m_jArry, BaseMessage message) {
      /// If is a new message

      try {
         /// Add the message to the file
         JSONObject obj_row = new JSONObject();
         obj_row.put("ID", message.ID.toString());
         obj_row.put("DateTime", message.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
         m_jArry.put(obj_row);

         JSONObject objRoot = new JSONObject();
         objRoot.put("root", m_jArry);

         // Constructs a FileWriter given a file name, using the platform's default charset
         FileWriter file = null;
         file = new FileWriter(MainActivity.defaultFilesPath + "/" + defaultFileName);
         file.write(objRoot.toString());

         file.flush();
         file.close();

         return true;
      } catch (JSONException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return false;
   }
}
