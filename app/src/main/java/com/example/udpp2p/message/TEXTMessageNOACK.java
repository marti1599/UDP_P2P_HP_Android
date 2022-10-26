package com.example.udpp2p.message;

import com.example.udpp2p.MessageType;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TEXTMessageNOACK extends BaseMessage {

    /**
     * Send a Text Message to a connected Peer
     *
     * @param message the text of the message
     */
    public String message;

    public TEXTMessageNOACK(MessageType type, String message) {
        super(type);
        this.message = message;
    }

    private TEXTMessageNOACK(MessageType type, UUID ID, LocalDateTime dateTime, String message) {
        super(type, ID, dateTime);
        this.message = message;
    }

    @Override
    public String toJSONString() {
        /// Message Scheme
        /// *******************************************************************************************************************
        /// ||          MessageType         ||          ID          ||          dateTime            ||        message        ||
        /// *******************************************************************************************************************

        try {
            JSONObject obj = new JSONObject();
            obj.put("MessageType", type.toString());
            obj.put("ID", ID.toString());
            obj.put("DateTime", dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            obj.put("TextMessage", message);

            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static TEXTMessageNOACK Parse(String message) {
        try {
            JSONObject obj = new JSONObject(message);

            MessageType type = MessageType.valueOf(obj.getString("MessageType"));
            UUID ID = UUID.fromString(obj.getString("ID"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(obj.getString("DateTime"), formatter);

            String msg = obj.getString("TextMessage");

            return new TEXTMessageNOACK(type, ID, dateTime, msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
