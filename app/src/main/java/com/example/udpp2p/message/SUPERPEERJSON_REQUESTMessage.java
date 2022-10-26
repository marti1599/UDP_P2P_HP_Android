package com.example.udpp2p.message;

import com.example.udpp2p.MessageType;
import com.example.udpp2p.STUN.STUN_NetType;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SUPERPEERJSON_REQUESTMessage extends BaseMessage{

    /**
     * Message for send a request to a Peer for receive back his superPeer.json file
     *
     * @param limit is the max amount of SuperPeerRow that i can have in the response
     *
     * //TODO check if i really need to send this information
     * @param LocalSourceEP is the Local EndPoint of the requester, i use this for add the requester Peer to the superPeers.json file on the remote user
     * @param PublicSourceEP is the Public EndPoint of the requester, i use this for add the requester Peer to the superPeers.json file on the remote user
     * @param netType is the NetType of the requester, i use this for add the requester Peer to the superPeers.json file on the remote user
     */
    public InetSocketAddress LocalSourceEP;
    public InetSocketAddress PublicSourceEP;
    public STUN_NetType netType;
    public int limit;

    public SUPERPEERJSON_REQUESTMessage(MessageType type, InetSocketAddress LocalSourceEP, InetSocketAddress PublicSourceEP, STUN_NetType netType, int limit) {
        super(type);
        this.LocalSourceEP = LocalSourceEP;
        this.PublicSourceEP = PublicSourceEP;
        this.netType = netType;
        this.limit = limit;
    }

    private SUPERPEERJSON_REQUESTMessage(MessageType type, UUID ID, LocalDateTime dateTime, InetSocketAddress LocalSourceEP, InetSocketAddress PublicSourceEP, STUN_NetType netType, int limit)
    {
        super(type, ID, dateTime);
        this.LocalSourceEP = LocalSourceEP;
        this.PublicSourceEP = PublicSourceEP;
        this.netType = netType;
        this.limit = limit;
    }

    @Override
    public String toJSONString() {

        /// Message Scheme
        /// **********************************************************************************************************************************************************************************************************************
        /// ||          MessageType         ||          ID          ||          dateTime            ||        NetType        ||         SourceLocalEP           ||          SourcePublicEP          ||          limit           ||
        /// **********************************************************************************************************************************************************************************************************************

        try {
            JSONObject obj = new JSONObject();
            obj.put("MessageType", type.toString());
            obj.put("ID", ID.toString());
            obj.put("DateTime", dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            obj.put("LocalSourceEP", LocalSourceEP.toString());
            obj.put("PublicSourceEP", PublicSourceEP.toString());
            obj.put("NetType", netType.toString());
            obj.put("Limit", limit);

            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SUPERPEERJSON_REQUESTMessage Parse(String message) {
        try {
            JSONObject obj = new JSONObject(message);

            MessageType type = MessageType.valueOf(obj.getString("MessageType"));

            UUID ID = UUID.fromString(obj.getString("ID"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(obj.getString("DateTime"), formatter);

            String[] localSourceEPStr = obj.getString("LocalSourceEP").split(":");
            InetSocketAddress localSourceEP = new InetSocketAddress(localSourceEPStr[0], Integer.parseInt(localSourceEPStr[1]));

            String[] publicSourceEPStr = obj.getString("SourcePublicEP").split(":");
            InetSocketAddress publicSourceEP = new InetSocketAddress(publicSourceEPStr[0], Integer.parseInt(publicSourceEPStr[1]));

            STUN_NetType netType = STUN_NetType.valueOf(obj.getString("NetType"));

            int limit = obj.getInt("Limit");

            return new SUPERPEERJSON_REQUESTMessage(type, ID, dateTime, localSourceEP, publicSourceEP, netType, limit);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
