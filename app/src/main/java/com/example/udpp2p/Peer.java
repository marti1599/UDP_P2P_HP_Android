package com.example.udpp2p;

import android.util.Log;

import com.example.udpp2p.STUN.STUN_NetType;
import com.example.udpp2p.message.KEEPMessage;
import com.example.udpp2p.message.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;

class Peer {

    /**
     * Peer information
     *
     * @param LocalEndPoints is a list of IPs that contains all the Local IPs of the Peer
     * @param PublicEndPoint is the Public IP of the Peer
     * @param LastConnectionDateTime is the DateTime of the last connection, i use this because if is too old (more than 30 days) i delete the row on the superPeers.json
     * @param NetType is the NetType of the Peer, i use this for understand which type of Peer is (MasterPeer, SuperPeer, Peer, ecc..)
     * @param connected is a boolean that is used for say if the Peer is reachable so if is connected and can receive messages, this will become false and stop the Thread that send KEEP Messages if the Peer doesn't for an amount of time, it's also used for remove Peers form the connected_peers list
     * @param threadKeepConnectionAlive is the Thread that keep the connection alive sending KEEP Messages
     * @param lastKeepMessageReceived is the DateTime of the last message received from the Peer, i need this for check if the user is online
     */
    InetSocketAddress EndPoint;
    LocalDateTime LastConnectionDateTime;
    STUN_NetType NetType;
    boolean connected = false;
    Thread threadKeepConnectionAlive;
    LocalDateTime lastKeepMessageReceived = LocalDateTime.MIN;

    /**
     * This are constants for timers/sleeps
     *
     * @param msTimeoutSendNewKEEPMessage is the ms to wait before send a new KEEP Message
     * @param msForClosedConnection is the ms after a connection will be declared as closed
     */
    int msTimeoutSendNewKEEPMessage = 5000;
    int msForClosedConnection = 30000;

    public Peer(InetSocketAddress EndPoint, LocalDateTime LastConnectionDateTime, STUN_NetType NetType, DatagramSocket socket) {
        this.EndPoint = EndPoint;
        this.LastConnectionDateTime = LastConnectionDateTime;
        this.NetType = NetType;
        this.connected = true;
        //Keep the connection open sending KEEP Message
        threadKeepConnectionAlive = new Thread(new Runnable() {
            @Override
            public void run() {
                while (connected) {
                    try {
                        //Keep the connection Open sending KEEP messages
                        KEEPMessage messageKEEP = new KEEPMessage(MessageType.KEEP);
                        byte[] msgSendKEEP = messageKEEP.toJSONString().getBytes();

                        try {
                            DatagramPacket packetSendPublic = new DatagramPacket(msgSendKEEP, msgSendKEEP.length, EndPoint);
                            socket.send(packetSendPublic);

                            Log.d("CONNECTION", "Sent: " + messageKEEP.toJSONString() + " To: " + EndPoint.toString());
                        } catch (IllegalArgumentException e) {
                            //Invalid EndPoint
                            e.printStackTrace();
                        }

                        if (!lastKeepMessageReceived.isEqual(LocalDateTime.MIN)) {
                            if (lastKeepMessageReceived.plusSeconds(msForClosedConnection / 1000).isBefore(LocalDateTime.now())) {
                                connected = false;
                            }
                        }

                        Thread.sleep(msTimeoutSendNewKEEPMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        threadKeepConnectionAlive.start();
    }
}
