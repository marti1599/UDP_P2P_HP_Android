package com.example.udpp2p.message;

import com.example.udpp2p.MessageType;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class KEEPMessage extends BaseMessage {

    /**
     * Message for Keep the connection Open through the firewall, otherwise after 30sec-1min the port will be closed so the connection will end
     *
     * NOW THIS CLASS IS EQUAL BaseMessage BUT I CREATED IT IF IN THE FUTURE I NEED TO ADD VALUES
     */

    public KEEPMessage(MessageType type) {
        super(type);
    }

    private KEEPMessage(MessageType type, UUID ID, LocalDateTime dateTime) {
        super(type, ID, dateTime);
    }

    @Override
    public String toJSONString() {
        /// Message Scheme
        /// ******************************************************************************************
        /// ||          MessageType         ||          ID          ||          dateTime            ||
        /// ******************************************************************************************

        try {
            JSONObject obj = new JSONObject();
            obj.put("MessageType", type.toString());
            obj.put("ID", ID.toString());
            obj.put("DateTime", dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static KEEPMessage Parse(String message) {
        try {
            JSONObject obj = new JSONObject(message);

            MessageType type = MessageType.valueOf(obj.getString("MessageType"));
            UUID ID = UUID.fromString(obj.getString("ID"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(obj.getString("DateTime"), formatter);

            return new KEEPMessage(type, ID, dateTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
