package com.example.udpp2p.message;

import com.example.udpp2p.MessageType;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TEXTMessageNOACKSPREAD extends BaseMessage {

    /**
     * Send a Text Message to a connected Peer
     *
     * @param message the text of the message
     */
    public String message;

    public InetSocketAddress sourcePeerLocalEP;
    public InetSocketAddress sourcePeerPublicEP;
    public InetSocketAddress destinationPeerLocalEP;
    public InetSocketAddress destinationPeerPublicEP;

    public TEXTMessageNOACKSPREAD(MessageType type, String message, InetSocketAddress sourcePeerLocalEP, InetSocketAddress sourcePeerPublicEP, InetSocketAddress destinationPeerLocalEP, InetSocketAddress destinationPeerPublicEP) {
        super(type);
        this.message = message;

        this.sourcePeerLocalEP = sourcePeerLocalEP;
        this.sourcePeerPublicEP = sourcePeerPublicEP;

        this.destinationPeerLocalEP = destinationPeerLocalEP;
        this.destinationPeerPublicEP = destinationPeerPublicEP;
    }

    private TEXTMessageNOACKSPREAD(MessageType type, UUID ID, LocalDateTime dateTime, String message, InetSocketAddress sourcePeerLocalEP, InetSocketAddress sourcePeerPublicEP, InetSocketAddress destinationPeerLocalEP, InetSocketAddress destinationPeerPublicEP) {
        super(type, ID, dateTime);
        this.message = message;

        this.sourcePeerLocalEP = sourcePeerLocalEP;
        this.sourcePeerPublicEP = sourcePeerPublicEP;

        this.destinationPeerLocalEP = destinationPeerLocalEP;
        this.destinationPeerPublicEP = destinationPeerPublicEP;
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

    public static TEXTMessageNOACKSPREAD Parse(String message) {
        try {
            JSONObject obj = new JSONObject(message);

            MessageType type = MessageType.valueOf(obj.getString("MessageType"));
            UUID ID = UUID.fromString(obj.getString("ID"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(obj.getString("DateTime"), formatter);

            String msg = obj.getString("TextMessage");

            String[] splitSourcePeerLocalEP = obj.getString("SourcePeerLocalEP").split(":");
            InetSocketAddress sourcePeerLocalEP = new InetSocketAddress(splitSourcePeerLocalEP[0], Integer.parseInt(splitSourcePeerLocalEP[1]));

            String[] splitSourcePeerPublicEP = obj.getString("SourcePeerPublicEP").split(":");
            InetSocketAddress sourcePeerPublicEP = new InetSocketAddress(splitSourcePeerPublicEP[0], Integer.parseInt(splitSourcePeerPublicEP[1]));

            String[] splitDestinationPeerLocalEP = obj.getString("DestinationPeerLocalEP").split(":");
            InetSocketAddress destinationPeerLocalEP = new InetSocketAddress(splitDestinationPeerLocalEP[0], Integer.parseInt(splitDestinationPeerLocalEP[1]));

            String[] splitDestinationPeerPublicEP = obj.getString("DestinationPeerPublicEP").split(":");
            InetSocketAddress destinationPeerPublicEP = new InetSocketAddress(splitDestinationPeerPublicEP[0], Integer.parseInt(splitDestinationPeerPublicEP[1]));

            return new TEXTMessageNOACKSPREAD(type, ID, dateTime, msg, sourcePeerLocalEP, sourcePeerPublicEP, destinationPeerLocalEP, destinationPeerPublicEP);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
