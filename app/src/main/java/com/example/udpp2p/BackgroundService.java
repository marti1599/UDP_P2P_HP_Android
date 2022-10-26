package com.example.udpp2p;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;

import com.example.udpp2p.STUN.STUN_Client;
import com.example.udpp2p.STUN.STUN_NetType;
import com.example.udpp2p.STUN.STUN_Result;
import com.example.udpp2p.message.ACKMessage;
import com.example.udpp2p.message.BaseMessage;
import com.example.udpp2p.message.EXCHANGEMessage;
import com.example.udpp2p.message.SUPERPEERJSON_REQUESTMessage;
import com.example.udpp2p.message.SUPERPEERJSON_RESPONSEMessage;
import com.example.udpp2p.message.TEXTMessageNOACK;
import com.example.udpp2p.message.TEXTMessageNOACKSPREAD;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundService extends Service {

    //adb shell am clear-debug-app

    public static final String BROADCAST_ACTION = "com.example.udpp2p";

    public static String TAG = "BackgroundService";

    /**
     * Handler var
     */
    public Handler handler = null;
    public static Runnable runnable = null;

    /**
     * ms Values for Handler
     * <p>
     * Method: OnCreate
     */
    //The ms for restart the handler when i get the error No Internet connection
    int msForRestartNoInternetConnection = 20000;
    //The ms for restart the handler and check if the connection to a Peer is terminated
    int msForRestartRefillList = 300000;
    //The ms for restart the handler and check if the connection to a Peer is terminated when the connection changed
    int msForRestartRefillListNewConnection = 10000;

    /**
     * superPeer.json Values
     */
    //Int that indicate how many rows read from the file in one time
    int superPeersJSONRowInOneTime_MasterPeers = 1;

    /**
     * ms Values
     * <p>
     * Method: StartThreadConnect
     */
    //ms for timeout of the send operation
    static int msForTimeoutConnection_MasterPeers = 5000;
    //ms for how much time try to connect to the Peer
    static int msForStopTryConnection_MasterPeers = 120000;
    //minimum count of MasterPeers connected
    static int minConnectedMasterPeers = 3;

    /**
     * ms Values CheckThreadsSYN
     */
    //ms for check when the sending SYN Threads are ended
    int msForRecheckIfThreadsSendingSYNisEmpty = 10000;

    //Port to use for default
    int listeningPort = 12356;
    //Local IPs of this machine
    ArrayList<InetSocketAddress> localEPs = new ArrayList<InetSocketAddress>();

    //Result of the STUN
    public static STUNResult stunResult;

    //Threads that are sending SYN for init the connection
    static HashMap<Pair<InetSocketAddress, InetSocketAddress>, Thread> threadsSendingSYN = new HashMap<>();

    //Max of connected peers
    int maxConnectedPeers = 20;
    //Connected peers
    public static HashMap<InetSocketAddress, Peer> connected_Peers = new HashMap<>();

    int defaultByteSize = 4096;

    //Socket for send/receive
    public static DatagramSocket socket;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        // Check if the Android Version is > 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            /// Show Notification

            // I need to show the notification for let the user know about the background worker
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("").setContentText("").build();
            startForeground(1, notification);
        }

        handler = new Handler();
        runnable = new Runnable() {
            public void run() {

                /**
                 * STEP 1
                 *
                 * Check Internet connection
                 */
                // Check if the Internet connection is ON
                if (!CheckInternetConnection()) {
                    /// Machine not connected to Internet

                    // Reset all and restart the handler
                    ResetConnectedPeers();
                    socket = null;

                    Log.d(TAG, "No Internet Connection");
                    handler.postDelayed(runnable, msForRestartNoInternetConnection);
                }

                /// Connection is ok

                /**
                 * STEP 2
                 *
                 * Check Network type using STUNs
                 */

                /// Save the lastConnectedEP for check if the IP:Port changed from before
                InetSocketAddress lastLocalEP = null;
                InetSocketAddress lastPublicEP = null;
                if (stunResult != null) {
                    lastLocalEP = stunResult.localEP;
                    lastPublicEP = stunResult.publicEP;
                }

                /// Check the connection Type using STUNs

                if(socket == null) {
                    // Clear the list of Local IPs
                    localEPs.clear();

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // Try to connect to STUN with all the local IPs
                            for (String ip : Utility.GetLocalIPAddress()) {
                                // TODO Check multiple times (it could be 10 times) if i can connect to the STUN until i get as valid response
                                // TODO Use multiple STUNs, not only the 2 that i use now, i want a list/file/http request os STUNs
                                STUNResult result = ConnectToSTUN(ip, listeningPort);

                                // Check if is a valid response
                                if (result != null) {
                                    if (result.netType != STUN_NetType.UdpBlocked) {
                                        socket = result.socket;
                                        stunResult = result;
                                    }
                                }

                                /// Add Local IP to the list of localEPs
                                localEPs.add(new InetSocketAddress(ip, listeningPort));
                            }
                        }
                    });

                    t.start();
                }

                if (stunResult != null) {
                    /// Check if network changed
                    if (lastLocalEP != null && lastPublicEP != null) {
                        if (!lastPublicEP.equals(stunResult.publicEP) || !lastLocalEP.equals(stunResult.localEP)) {
                            // Public or Local EP Changed, i'm connected to a new Network so reset all
                            ResetConnectedPeers();
                            socket = null;
                            handler.postDelayed(runnable, msForRestartRefillListNewConnection);
                        }
                    }

                    /**
                     * STEP 3
                     *
                     * Start the connection to Peers
                     */

                    /// Execute code based on the NetType
                    // TODO save the NetType, so the next time i will execute this i can check if the netType changed

                    Intent intent = new Intent(BROADCAST_ACTION);
                    intent.putExtra("NetType", stunResult.netType.toString());
                    intent.putExtra("LocalEP", stunResult.localEP.toString());
                    intent.putExtra("PublicEP", stunResult.publicEP.toString());
                    sendBroadcast(intent);
                    // TODO Check which threads are running, if this thread is already running i don't need to restart it or start another

                    /// Get PeerType
                    PeerType peerType = ConvertNetTypeToPeerType(stunResult.netType);

                    /// Start thread that listen to incoming message
                    StartThreadListen(listeningPort, peerType);

                    /// Start thread that try to connect to Peers
                    ConnectToPeers(peerType);
                }else{
                    handler.postDelayed(runnable, 500);
                }

                /// Restart the handler after X time
                handler.postDelayed(runnable, msForRestartRefillList);
            }
        };

        /// Start the handler
        handler.postDelayed(runnable, 100);
    }

    /**
     * Reset connected peer stopping all the KEEP thread
     */
    private void ResetConnectedPeers() {
        /// Stop all the threads that send KEEP
        for (Peer peer : connected_Peers.values()) {
            peer.threadKeepConnectionAlive.interrupt();
        }

        //TODO i think i need to stop the ConnectToPeers_MasterPeer thread or i can get new connections

        /// Clear the connected peers list
        connected_Peers.clear();
    }

    /**
     * Method for connect to Peers from the superPeers.json file
     */
    private void ConnectToPeers(PeerType peerType) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                if(peerType.equals(PeerType.Peer))
                {
                    /// If i'm a Peer i can only connect to MasterPeers, so i put the minimum MasterPeers count to the maxConnectedPeers value

                    minConnectedMasterPeers = maxConnectedPeers;
                }

                /// Count MasterPeers connected
                int countMasterPeers = 0;
                /// Index that indicate the current row that i need to start from for read
                int currentSuperPeerJSONFileRow_MasterPeers = 0;
                /// Count of superPeers.json rows
                int superPeersRows_MasterPeers_Count = ReadSuperPeersJsonRows(0, 0).size();

                /// Loop for try to connect to Peers until i reach the desired number
                while (connected_Peers.keySet().size() < maxConnectedPeers) {

                    /// Check if the file is ended
                    if (currentSuperPeerJSONFileRow_MasterPeers + superPeersJSONRowInOneTime_MasterPeers > superPeersRows_MasterPeers_Count) {
                        break;
                    }

                    /// Read lines from the superPeers.json file from index to index
                    ArrayList<SuperPeerRow> superPeersRows = ReadSuperPeersJsonRows(currentSuperPeerJSONFileRow_MasterPeers, currentSuperPeerJSONFileRow_MasterPeers + superPeersJSONRowInOneTime_MasterPeers);

                    /// Start loop for start threads that send SYN
                    for (int i = 0; i < superPeersRows.size(); i++) {

                        /// With this i need to connected first to MasterPeers and after to other Peers, so i have at least minConnectedMasterPeers MasterPeers connected
                        if(minConnectedMasterPeers > countMasterPeers)
                        {
                            /// Connect only to MasterPeers

                            /// Skip this row if is not a MasterPeer
                            if(!ConvertNetTypeToPeerType(superPeersRows.get(i).NetType).equals(PeerType.MasterPeer)) {
                                continue;
                            }
                        }

                        /// Skip the row if i'm already connected to this Peer
                        if (!connected_Peers.keySet().contains(superPeersRows.get(i).LocalEndPoint) || !connected_Peers.keySet().contains(superPeersRows.get(i).LocalEndPoint)) {
                            /// Start thread that send SYN
                            StartThreadConnect(superPeersRows.get(i).LocalEndPoint, superPeersRows.get(i).PublicEndPoint);

                            /// Count the MasterPeers connected
                            if(ConvertNetTypeToPeerType(superPeersRows.get(i).NetType).equals(PeerType.MasterPeer)) {
                                countMasterPeers++;
                            }
                        }
                    }

                    /// Update the currentRow index
                    currentSuperPeerJSONFileRow_MasterPeers = currentSuperPeerJSONFileRow_MasterPeers + superPeersJSONRowInOneTime_MasterPeers;

                    /// Wait the Thread send SYN end (check if threadsSendingSYN is empty, if no wait)
                    while (!threadsSendingSYN.isEmpty()) {
                        // Wait
                        try {
                            Thread.sleep(msForRecheckIfThreadsSendingSYNisEmpty);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private PeerType ConvertNetTypeToPeerType(STUN_NetType netType)
    {
        switch (netType) {
            case OpenInternet: {
                /// This will be a MasterPeer
                return PeerType.MasterPeer;
            }
            case FullCone_TRANSLATEIP:
            case FullCone_TRANSLATEIP_TRANSLATEPORT: {
                /// This will be a SuperPeer
                return PeerType.SuperPeer;
            }
            case FullCone_TRANSLATEIP_TRANSLATEPORTDESTINATION:
            case FullCone_TRANSLATEIP_TRANSLATEPORTFREE: {
                /// This will be a Peer
                return PeerType.Peer;
            }
            default:
                /// Connection type not implemented
                throw new NullPointerException();
        }
    }

    /**
     * Start thread that listen for incoming messages
     *
     * @param port port for start the listener
     */
    private void StartThreadListen(int port, PeerType peerType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    /// Init the byte for read incoming messages
                    byte[] buf = new byte[defaultByteSize];

                    /// Loop
                    while (true) {
                        try {

                            /// Check if all the Peers in the connection_Peers are connected
                            CheckIfPeersConnected();

                            /// Receive for incoming messages
                            DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress("0.0.0.0", port));
                            socket.receive(packet);

                            /// String of message received
                            String received = new String(packet.getData(), 0, packet.getLength());

                            // TODO add try catch to parse
                            /// Parse the message and take the basic message info
                            BaseMessage baseMessage = BaseMessage.Parse(received);

                            /// Check if is a valid message
                            if (baseMessage != null) {

                                /// Message received, check the type

                                /// Every time i receive a message i put it in a file of received messages for check if i've already received it
                                if (!MessagesReceivedUtility.CheckIfAlreadyReceivedMessage(baseMessage)) {

                                    /// Check the MessageType
                                    switch (baseMessage.type) {
                                        case SYN: {

                                            /// Check if this type of Peer can receive this type of message
                                            if(peerType.equals(PeerType.MasterPeer) || peerType.equals(PeerType.SuperPeer) || peerType.equals(PeerType.Peer)) {
                                                /// SYN MESSAGE, SOMEONE WANT TO START THE CONNECTION WITH ME

                                                Log.d("CONNECTION", "Received: " + baseMessage.toJSONString() + " From: " + packet.getSocketAddress().toString());

                                                // Request for connection, send response (SYN_ACK)
                                                ACKMessage messageSYNACK = new ACKMessage(MessageType.SYN_ACK, stunResult.netType, localEPs, stunResult.publicEP);
                                                byte[] msgSendSYNACK = messageSYNACK.toJSONString().getBytes();

                                                DatagramPacket packetSend = new DatagramPacket(msgSendSYNACK, msgSendSYNACK.length, packet.getSocketAddress());
                                                socket.send(packetSend);

                                                Log.d("CONNECTION", "Sent: " + messageSYNACK.toJSONString() + " To: " + packet.getSocketAddress().toString());
                                            }else{
                                                Log.d("CONNECTION", "Error i can't execute SYN");
                                            }
                                        }
                                        break;
                                        case SYN_ACK: {

                                            /// Check if this type of Peer can receive this type of message
                                            if (peerType.equals(PeerType.MasterPeer) || peerType.equals(PeerType.SuperPeer) || peerType.equals(PeerType.Peer)) {

                                                /// SYN_ACK MESSAGE, I GET A RESPONSE FOR THE SENT SYN MESSAGE

                                                ACKMessage ackMessage = ACKMessage.Parse(received);

                                                Log.d("CONNECTION", "Received: " + ackMessage.toJSONString() + " From: " + packet.getSocketAddress().toString());

                                                // Connection established, send ACK for confirm
                                                ACKMessage messageACK = new ACKMessage(MessageType.ACK, stunResult.netType, localEPs, stunResult.publicEP);
                                                byte[] msgSendACK = messageACK.toJSONString().getBytes();

                                                DatagramPacket packetSend = new DatagramPacket(msgSendACK, msgSendACK.length, packet.getSocketAddress());
                                                socket.send(packetSend);

                                                Log.d("CONNECTION", "Sent: " + messageACK.toJSONString() + " To: " + packet.getSocketAddress().toString());

                                                // Add the InetSocketAddress to the list of connected users
                                                LocalDateTime dateTime = LocalDateTime.now();
                                                connected_Peers.put((InetSocketAddress) packet.getSocketAddress(), new Peer((InetSocketAddress) packet.getSocketAddress(), dateTime, ackMessage.netType, socket));

                                                // TODO CHECK THIS WELL
                                                // I was the starter of the connection so i need to stop the Thread that send SYNs
                                                for (Pair<InetSocketAddress, InetSocketAddress> index : threadsSendingSYN.keySet()) {
                                                    //TODO I need to edit this after the TEST mode end, i need to check if the index is the one i received the Message, if yes stop the send SYN Thread
                                                    if (index.first.equals(packet.getSocketAddress()) || index.second.equals(packet.getSocketAddress())) {
                                                        threadsSendingSYN.get(index).interrupt();
                                                        threadsSendingSYN.remove(index);
                                                    }
                                                }
                                            }else{
                                                Log.d("CONNECTION", "Error i can't execute SYN_ACK");
                                            }

                                        }
                                        break;
                                        case ACK: {

                                            if (peerType.equals(PeerType.MasterPeer) || peerType.equals(PeerType.SuperPeer) || peerType.equals(PeerType.Peer)) {

                                                /// ACK MESSAGE, I GET A RESPONSE FOR THE SENT SYN_ACK MESSAGE

                                                ACKMessage ackMessage = ACKMessage.Parse(received);

                                                Log.d("CONNECTION", "Received: " + ackMessage.toJSONString() + " From: " + packet.getSocketAddress().toString());

                                                // Add the InetSocketAddress to the list of connected users
                                                if (!connected_Peers.containsKey(packet.getSocketAddress())) {
                                                    connected_Peers.put((InetSocketAddress) packet.getSocketAddress(), new Peer((InetSocketAddress) packet.getSocketAddress(), LocalDateTime.now(), ackMessage.netType, socket));
                                                }

                                                /************************/
                                                // Connection confirmed
                                                /************************/
                                            }else{
                                                Log.d("CONNECTION", "Error i can't execute ACK");
                                            }
                                        }
                                        break;
                                        case KEEP: {

                                            if (peerType.equals(PeerType.MasterPeer) || peerType.equals(PeerType.SuperPeer) || peerType.equals(PeerType.Peer)) {
                                                /// KEEP MESSAGE, THIS IS FOR KEEP THE FIREWALL PORT OPEN

                                                // Update the dateTime of the last KEEP Message Received
                                                if (connected_Peers.keySet().contains(packet.getSocketAddress())) {
                                                    //Write Message only if it's a connected Peer
                                                    Log.d("CONNECTION", "Received: " + baseMessage.toJSONString() + " From: " + packet.getSocketAddress().toString());

                                                    connected_Peers.get(packet.getSocketAddress()).lastKeepMessageReceived = LocalDateTime.now();
                                                }
                                            }else{
                                                Log.d("CONNECTION", "Error i can't execute KEEP");
                                            }
                                        }
                                        break;
                                        case EXCHANGE: {

                                            if (peerType.equals(PeerType.MasterPeer) || peerType.equals(PeerType.SuperPeer)) {

                                            /// EXCHANGE MESSAGE, THIS IS FOR SPREAD THE EXCHANGE MESSAGE UNTIL IT REACHES THE DESTINATION PEER

                                            EXCHANGEMessage exchangeMessage = EXCHANGEMessage.Parse(received);

                                            Log.d("CONNECTION", "Received: " + exchangeMessage.toJSONString() + " From: " + packet.getSocketAddress().toString());

                                            if (stunResult.localEP.equals(exchangeMessage.destinationPeerLocalEP) && stunResult.publicEP.equals(exchangeMessage.destinationPeerPublicEP)) {

                                                /// I'm the destination

                                                // Check if i'm already connected to the Peer
                                                boolean alreadyConnected = false;
                                                for (Peer peer : connected_Peers.values()) {
                                                    if (peer.EndPoint.equals(exchangeMessage.sourcePeerPublicEP)) {
                                                        alreadyConnected = true;
                                                    }
                                                }
                                                if (!alreadyConnected) {
                                                    // Start a Thread that send SYN Messages to the received EndPoint for create a hole on the firewall
                                                    StartThreadConnect(exchangeMessage.sourcePeerLocalEP, exchangeMessage.sourcePeerPublicEP);
                                                }
                                            } else {

                                                /// I'm not the destination, send the message to all my connected Peers

                                                byte[] msgSendEXCHANGE = exchangeMessage.toJSONString().getBytes();

                                                /// Send the message to other Peers
                                                for (InetSocketAddress peerEndPoint : connected_Peers.keySet()) {

                                                    DatagramPacket packetSendPublic = new DatagramPacket(msgSendEXCHANGE, msgSendEXCHANGE.length, connected_Peers.get(peerEndPoint).EndPoint);
                                                    socket.send(packetSendPublic);

                                                    Log.d("CONNECTION", "Sent: " + exchangeMessage.toJSONString() + " To: " + packet.getSocketAddress().toString());
                                                }
                                            }
                                            }else{
                                                Log.d("CONNECTION", "Error i can't execute EXCHANGE");
                                            }
                                        }
                                        break;
                                        case SUPERPEERJSON_REQUEST: {

                                            if (peerType.equals(PeerType.MasterPeer) || peerType.equals(PeerType.SuperPeer) || peerType.equals(PeerType.Peer)) {


                                                /// SUPERPEERJSON_REQUEST MESSAGE, THIS IS FOR READ THE REQUEST FROM THE PEER

                                                // Received a request for get my superPeers.json file
                                                SUPERPEERJSON_REQUESTMessage superpeerjson_requestMessage = SUPERPEERJSON_REQUESTMessage.Parse(received);

                                                //Save the Requester information
                                                ArrayList<SuperPeerRow> arraySuperPeerRows = new ArrayList<>();
                                                arraySuperPeerRows.add(new SuperPeerRow(superpeerjson_requestMessage.LocalSourceEP, superpeerjson_requestMessage.PublicSourceEP, LocalDateTime.now(), superpeerjson_requestMessage.netType));
                                                AddSuperPeersJsonRows(arraySuperPeerRows);

                                                //Read Peers from file
                                                ArrayList<SuperPeerRow> superPeersRows_MasterPeers = ReadSuperPeersJsonRows(0, superpeerjson_requestMessage.limit);

                                                //Send response
                                                SUPERPEERJSON_RESPONSEMessage superpeerjson_responseMessage = new SUPERPEERJSON_RESPONSEMessage(MessageType.SUPERPEERJSON_RESPONSE, superPeersRows_MasterPeers);
                                                byte[] msgSendSUPERPEERJSON_RESPONSE = superpeerjson_responseMessage.toJSONString().getBytes();

                                                DatagramPacket packetSend = new DatagramPacket(msgSendSUPERPEERJSON_RESPONSE, msgSendSUPERPEERJSON_RESPONSE.length, packet.getSocketAddress());
                                                socket.send(packetSend);

                                                Log.d("CONNECTION", "Sent: " + superpeerjson_requestMessage.toJSONString() + " To: " + packet.getSocketAddress().toString());
                                            }else{
                                                Log.d("CONNECTION", "Error i can't execute SUPERPEERJSON_REQUEST");
                                            }
                                        }
                                        break;
                                        case SUPERPEERJSON_RESPONSE: {

                                            if (peerType.equals(PeerType.MasterPeer) || peerType.equals(PeerType.SuperPeer) || peerType.equals(PeerType.Peer)) {


                                                /// SUPERPEERJSON_RESPONSE MESSAGE, RECEIVED A RESPONSE FOR THE SUPERPEERJSON_REQUEST

                                                // Received a response for the SUPERPEERJSON_REQUEST, parse it
                                                SUPERPEERJSON_RESPONSEMessage superpeerjson_responseMessage = SUPERPEERJSON_RESPONSEMessage.Parse(received);

                                                AddSuperPeersJsonRows(superpeerjson_responseMessage.superPeerRows);

                                                Log.d("CONNECTION", "Received: " + superpeerjson_responseMessage.toJSONString() + " From: " + packet.getSocketAddress().toString());
                                            }else{
                                                Log.d("CONNECTION", "Error i can't execute SUPERPEERJSON_RESPONSE");
                                            }
                                        }
                                        break;
                                        case TEXTMessageNOACK: {

                                            if (peerType.equals(PeerType.MasterPeer) || peerType.equals(PeerType.SuperPeer)) {
                                                // Received a Text Message
                                                TEXTMessageNOACK textMessageNOACK = TEXTMessageNOACK.Parse(received);

                                                Log.d("CONNECTION", "Received: " + textMessageNOACK.toJSONString() + " From: " + packet.getSocketAddress().toString());
                                                // TODO Display received message
                                            }else{
                                                Log.d("CONNECTION", "Error i can't execute SUPERPEERJSON_RESPONSE");
                                            }
                                        }
                                        break;
                                        case TEXTMessageNOACKSPREAD: {

                                            TEXTMessageNOACKSPREAD textMessageNOACKSPREAD = TEXTMessageNOACKSPREAD.Parse(received);

                                            Log.d("CONNECTION", "Received: " + textMessageNOACKSPREAD.toJSONString() + " From: " + packet.getSocketAddress().toString());

                                            if (peerType.equals(PeerType.MasterPeer) || peerType.equals(PeerType.SuperPeer) || peerType.equals(PeerType.Peer)) {
                                                if (stunResult.localEP.equals(textMessageNOACKSPREAD.destinationPeerLocalEP) && stunResult.publicEP.equals(textMessageNOACKSPREAD.destinationPeerPublicEP)) {

                                                    /// I'm the destination

                                                    // TODO i'm the destination, display the message
                                                } else {

                                                    /// I'm not the destination, send the message to all my connected Peers

                                                    byte[] msgSendTEXTMessageNOACKSPREAD = textMessageNOACKSPREAD.toJSONString().getBytes();

                                                    /// Send the message to other Peers
                                                    for (InetSocketAddress peerEndPoint : connected_Peers.keySet()) {

                                                        DatagramPacket packetSendPublic = new DatagramPacket(msgSendTEXTMessageNOACKSPREAD, msgSendTEXTMessageNOACKSPREAD.length, connected_Peers.get(peerEndPoint).EndPoint);
                                                        socket.send(packetSendPublic);

                                                        Log.d("CONNECTION", "Sent: " + textMessageNOACKSPREAD.toJSONString() + " To: " + packet.getSocketAddress().toString());
                                                    }
                                                }
                                            }else{
                                                Log.d("CONNECTION", "Error i can't execute SUPERPEERJSON_RESPONSE");
                                            }
                                        }
                                        break;
                                        default:
                                            // MessageType not Found
                                            Log.d("CONNECTION", "Received: " + baseMessage.toJSONString() + " From: " + packet.getSocketAddress().toString());
                                            break;
                                    }
                                }
                            }
                        } catch (SocketTimeoutException e) {
                            //Socket Timeout, restart listen
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Check if the Peer is connected, free the list
     */
    private void CheckIfPeersConnected() {
        //Check if the Peers are connected
        for (int i = 0; i < connected_Peers.keySet().toArray().length; i++) {
            if (!connected_Peers.get(connected_Peers.keySet().toArray()[i]).connected) {
                //Peer not connected, remove it from the list
                connected_Peers.remove(connected_Peers.get(connected_Peers.keySet().toArray()[i]));
            }
        }
    }

    /**
     * Start thread that send SYN message for connect to Peers
     *
     * @param LocalEndPoint  LocalEP of the destination Peer
     * @param PublicEndPoint PublicEP of the destination Peer
     */
    public static void StartThreadConnect(InetSocketAddress LocalEndPoint, InetSocketAddress PublicEndPoint) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    /// Loop for send messages for X time
                    for (int i = 0; i < msForStopTryConnection_MasterPeers / msForTimeoutConnection_MasterPeers; i++) {
                        try {
                            BaseMessage message = new BaseMessage(MessageType.SYN);
                            byte[] buf = message.toJSONString().getBytes();

                            /// Send SYN Message to the MasterPeer, he need to receive the response and send back a SYN Message
                            DatagramPacket packetSendLocal = new DatagramPacket(buf, buf.length, LocalEndPoint);
                            socket.send(packetSendLocal);

                            Log.d("CONNECTION", "Sent: " + message.toJSONString() + " To: " + packetSendLocal.getSocketAddress().toString());

                            /// Send SYN Message to the MasterPeer, he need to receive the response and send back a SYN Message
                            DatagramPacket packetSendPublic = new DatagramPacket(buf, buf.length, PublicEndPoint);
                            socket.send(packetSendPublic);

                            Log.d("CONNECTION", "Sent: " + message.toJSONString() + " To: " + packetSendPublic.getSocketAddress().toString());

                            Thread.sleep(msForTimeoutConnection_MasterPeers);
                        } catch (SocketTimeoutException ex) {
                            /// Receive Timeout
                        } catch (InterruptedException e) {
                            break;
                        }
                    }

                    // TODO TRY WITH EXCHANGE

                    /// Thread ended, remove it from the sendingSYN list
                    threadsSendingSYN.remove(new Pair<>(LocalEndPoint, PublicEndPoint));
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();

        threadsSendingSYN.put(new Pair<>(LocalEndPoint, PublicEndPoint), t);
    }

    /**
     * Add Peers to the superPeers.json file
     *
     * @param superPeerRows Peers list to add
     */
    private void AddSuperPeersJsonRows(ArrayList<SuperPeerRow> superPeerRows) {
        try {

            /// Read the json file that contains Peers
            String json = null;
            try {
                InputStream is = new FileInputStream(this.getFilesDir() + "/" + "superPeers.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                json = new String(buffer, "UTF-8");
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }

            JSONObject objRoot = new JSONObject(json);
            JSONArray m_jArry = objRoot.getJSONArray("root");

            /// Check if Peers is already in the file, if no add it
            for (SuperPeerRow superPeerRow : superPeerRows) {
                boolean superPeerAlreadyAdded = false;
                /// Loop for check if the Peer is already in the file
                for (int i = 0; i < m_jArry.length(); i++) {
                    JSONObject obj = m_jArry.getJSONObject(i);

                    String LocalIP = obj.getString("LocalIP");
                    String LocalPort = obj.getString("LocalPort");
                    InetSocketAddress LocalEP = new InetSocketAddress(LocalIP, Integer.parseInt(LocalPort));

                    String PublicIP = obj.getString("PublicIP");
                    String PublicPort = obj.getString("PublicPort");
                    InetSocketAddress PublicEP = new InetSocketAddress(PublicIP, Integer.parseInt(PublicPort));

                    if ((LocalEP.equals(superPeerRow.LocalEndPoint) && PublicEP.equals(superPeerRow.PublicEndPoint)) || (stunResult.localEP.equals(superPeerRow.LocalEndPoint) && stunResult.publicEP.equals(superPeerRow.PublicEndPoint))) {
                        //Peer already added
                        superPeerAlreadyAdded = true;

                        //TODO here i can update the LastConnectionDateTime
                    }
                }

                /// Add Peer to the file
                if (!superPeerAlreadyAdded) {
                    JSONObject obj_row = new JSONObject();

                    obj_row.put("LocalIP", superPeerRow.LocalEndPoint.getAddress().toString().substring(1));
                    obj_row.put("LocalPort", String.valueOf(superPeerRow.LocalEndPoint.getPort()));
                    obj_row.put("PublicIP", superPeerRow.PublicEndPoint.getAddress().toString().substring(1));
                    obj_row.put("PublicPort", String.valueOf(superPeerRow.PublicEndPoint.getPort()));
                    obj_row.put("LastConnectionDateTime", superPeerRow.LastConnectionDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    obj_row.put("NetType", superPeerRow.NetType.toString());

                    m_jArry.put(obj_row);
                }
            }

            objRoot = new JSONObject();
            objRoot.put("root", m_jArry);

            // Constructs a FileWriter given a file name, using the platform's default charset
            FileWriter file = null;
            file = new FileWriter(this.getFilesDir() + "/" + "superPeers.json");
            file.write(objRoot.toString());

            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read SuperPeerRows from file
     *
     * @param indexStartRow Start index of the list
     * @param indexEndRow   End index of the list (If is 0 it means to the end of the file)
     * @return Return list of SuperPeerRow of the file
     */
    private ArrayList<SuperPeerRow> ReadSuperPeersJsonRows(int indexStartRow, int indexEndRow) {
        ArrayList<SuperPeerRow> rows = new ArrayList<>();

        String json = null;
        try {
            InputStream is = new FileInputStream(this.getFilesDir() + "/" + "superPeers.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        try {
            JSONObject objRoot = new JSONObject(json);
            JSONArray m_jArry = objRoot.getJSONArray("root");

            if (indexEndRow == 0) {
                indexEndRow = m_jArry.length();
            }

            for (int i = indexStartRow; i < indexEndRow; i++) {
                JSONObject jo_inside = m_jArry.getJSONObject(i);

                String LocalIP_value = jo_inside.getString("LocalIP");
                int LocalPort_value = jo_inside.getInt("LocalPort");
                String PublicIP_value = jo_inside.getString("PublicIP");
                int PublicPort_value = jo_inside.getInt("PublicPort");
                String LastConnectionDateTime_value = jo_inside.getString("LastConnectionDateTime");
                String NetType = jo_inside.getString("NetType");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime dateTime = LocalDateTime.parse(LastConnectionDateTime_value, formatter);

                //TODO WHY????????
                //TODO Return only if NetType is OpenInternet
                SuperPeerRow row = new SuperPeerRow(new InetSocketAddress(LocalIP_value, LocalPort_value), new InetSocketAddress(PublicIP_value, PublicPort_value), dateTime, STUN_NetType.valueOf(NetType));
                rows.add(row);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return rows;
    }

    /**
     * Check the Internet connection
     *
     * @return true if is connected, false if not
     */
    public boolean CheckInternetConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            return true;
        } else {
            return false;
        }
    }

    /**
     * Try to connect to the STUN servers
     *
     * @param IP   The IP for try to connect to the STUNs
     * @param port The Port for try to connect to the STUNs
     * @return STUNResult that contains all the info of the machine
     */
    public STUNResult ConnectToSTUN(String IP, int port) {

        ExecutorService executor = Executors.newFixedThreadPool(5);

        Callable<STUNResult> callable = new Callable<STUNResult>() {
            @Override
            public STUNResult call() {
                try {
                    DatagramSocket socket = new DatagramSocket(null);
                    socket.bind(new InetSocketAddress(IP, port));

                    //TEST 1
                    //Base Test for get the Public IP and Port
                    STUN_Result result_1 = STUN_Client.Query("stun.l.google.com", 19302, socket);
                    STUN_NetType netType_1 = result_1.getNetType();
                    InetSocketAddress epLocal_1 = (InetSocketAddress) socket.getLocalSocketAddress();
                    InetSocketAddress epPublic_1 = null;
                    if (result_1.getNetType() != STUN_NetType.UdpBlocked) {
                        epPublic_1 = result_1.getPublicEndPoint();
                    }

                    //TEST 2
                    //Test for check if sending a request to the same STUN the result change
                    //(Port could be random on every request)
                    STUN_Result result_2 = STUN_Client.Query("stun.l.google.com", 19302, socket);
                    STUN_NetType netType_2 = result_2.getNetType();
                    InetSocketAddress epLocal_2 = (InetSocketAddress) socket.getLocalSocketAddress();
                    InetSocketAddress epPublic_2 = null;
                    if (result_2.getNetType() != STUN_NetType.UdpBlocked) {
                        epPublic_2 = result_2.getPublicEndPoint();
                    }

                    //TEST 3
                    //Test for check if sending a request to a new STUN the result is different
                    //(Port could change according to the destination)
                    STUN_Result result_3 = STUN_Client.Query("stun1.l.google.com", 19302, socket);
                    STUN_NetType netType_3 = result_3.getNetType();
                    InetSocketAddress epLocal_3 = (InetSocketAddress) socket.getLocalSocketAddress();
                    InetSocketAddress epPublic_3 = null;
                    if (result_3.getNetType() != STUN_NetType.UdpBlocked) {
                        epPublic_3 = result_3.getPublicEndPoint();
                    }

                    STUN_NetType netTypeResult = netType_1;

                    if (netType_1.equals(STUN_NetType.FullCone) || netType_2.equals(STUN_NetType.FullCone) || netType_3.equals(STUN_NetType.FullCone)) {
                        if (epPublic_1.getPort() == port && epPublic_2.getPort() == port && epPublic_3.getPort() == port) {
                            //All the port results are the same of the passed one, so the port is not translated and fixed
                            netTypeResult = STUN_NetType.FullCone_TRANSLATEIP;
                        } else if ((epPublic_1.getPort() != port || epPublic_2.getPort() != port || epPublic_3.getPort() != port)
                                && epPublic_1.getPort() == epPublic_2.getPort()
                                && epPublic_1.getPort() == epPublic_3.getPort()) {
                            //The port is different from the passed one, but is always the same (Fixed)
                            netTypeResult = STUN_NetType.FullCone_TRANSLATEIP_TRANSLATEPORT;
                        } else if ((epPublic_1.getPort() != port || epPublic_2.getPort() != port || epPublic_3.getPort() != port)
                                && epPublic_1.getPort() == epPublic_2.getPort()
                                && epPublic_1.getPort() != epPublic_3.getPort()) {
                            //The port change according to the destination
                            netTypeResult = STUN_NetType.FullCone_TRANSLATEIP_TRANSLATEPORTDESTINATION;
                        } else if ((epPublic_1.getPort() != port || epPublic_2.getPort() != port || epPublic_3.getPort() != port)
                                && epPublic_1.getPort() != epPublic_2.getPort()
                                && epPublic_1.getPort() != epPublic_3.getPort()) {
                            //The port change according to the destination
                            netTypeResult = STUN_NetType.FullCone_TRANSLATEIP_TRANSLATEPORTFREE;
                        }
                    }

                    return new STUNResult(netTypeResult, epLocal_1, epPublic_1, socket);
                } catch (SocketException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        try {
            STUNResult value = executor.submit(callable).get();
            executor.shutdown();

            if (value != null) {
                return value;
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onDestroy() {
        /* IF YOU WANT THIS SERVICE KILLED WITH THE APP THEN UNCOMMENT THE FOLLOWING LINE */
        //handler.removeCallbacks(runnable);
        //Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStart(Intent intent, int startid) {
        //Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();
    }
}