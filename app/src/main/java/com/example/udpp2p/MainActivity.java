package com.example.udpp2p;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static File defaultFilesPath;

    /*******************************************************************
    /// Scheme
    *******************************************************************/

    /******************************************************************
    /// Background Service
    /******************************************************************/

    /// The background service run in background for let the communication work even if the app is close. I need this because no one will keep the app open for a long time so with this i use the phone for keep the P2P network on
    /// Settings:
    //  TODO  Test the amount of connection that i can keep open that use low battery (and check which operation uses more energy)
    ///      - MAX CONNECTIONS: The background worker will handle a max of 20 connections (for use less energy and battery)

    /// What will the background worker do?
    /// Steps:
    /// 1 - Check if internet connection is available
    /// YES = Continue, NO = Wait for N min and recheck (wait until the connection will be available)
    ///     2 - Check the connection Type (/** Check NAT Type **/)
    ///     MasterPeer, SuperPeer, Peer
    ///     3.1 - If is a MasterPeer
    ///         1 - Start Thread that listen for incoming messages (StartThreadListen_MasterPeer)
    ///         2 - Read superPeers.json file and try to connect to every Peer
    ///             1 - Create a list of DatagramSocket with size of N (MAX CONNECTIONS)
    ///             2 - Start N Thread for the first N rows in the superPeers.json that try to send SYN using the row infos
    ///             3 - Check if i receive a SYN_ACK response in N sec
    ///             YES = Continue, NO = Retry N times, if after is always NO skip this row
	///					(1 - if the server get back a ACK Message i can send a SUPERPEERJSON_REQUEST for get his Json and update mine, in this way every time there is a connection the server will be update and when a client request the json he have a updated json)
    ///                 1 - If i get back a SYN_ACK i start to send KEEP packets to the received address (i need to do this because sometimes Port can change so only 1 will receive the packets, with this all work)
    ///                 2 - When i have all the N slots filled the communication is completed so i stop to read the rows and try to connect

    ///                 If one connection NOT work for more than 30 sec i let the slot free for a new connection

    ///     3.2 - If is a SuperPeer
    ///         1 - Start Thread that listen for incoming messages (StartThreadListen_SuperPeer)
    ///         2 - Read superPeers.json file and try to connect to every Peer
    ///             1 - Create a list of DatagramSocket with size of N (MAX CONNECTIONS) (In this list i need at least 1 OpenInternet)
    ///             2 - Start N Thread for the first N rows in the superPeers.json
    ///             3 - Check NetType of the row
    ///             MasterPeer NetTypes (OpenInternet), SuperPeer NetTypes (FullCone), Peer
    ///                 4.1 - If is MasterPeer
    ///                     1 - Start Thread to send SYN using the row infos
    ///                     2 - If i get back a SYN_ACK i start to send packets to the received address (i need to do this because sometimes Port can change so only 1 will receive the packets, with this all work)
    ///                 4.2 - If is SuperPeer
    ///                     1 - Send a SYN_EXCHANGE Message to a MasterPeer for Exchange IP and Port between the 2 users
    ///                     2 - Start Thread to send SYN using the row infos
    ///                     3 - If i get back a SYN_ACK i start to send packets to the received address (i need to do this because sometimes Port can change so only 1 will receive the packets, with this all work)
    ///                     (The MasterPeer will receive the SYN_EXCHANGE request and check if the IP and Port is in his list of connected users, if not he spread the SYN_EXCHANGE to the other MasterPeer until the final user will receive the message)
    ///                 4.3 - If is Peer
    ///                     //TODO
    ///                 If one connection NOT work for more than N min i restart to reads rows skipping the one that i'm already connected to until all the N slots will be filled again

    ///     3.3 - If is a Peer
    //TODO


    /******************************************************************
    /// Thread Listener
    ******************************************************************/

    /// Thread that listen and parse the incoming packets
    /// this will be a while(true) loop

    ///Steps:
    /// 1 - Start a localSocket that listen on Local IP (0.0.0.0) and chosen Port (12356)
    // TODO i need to check if is better to put the socket in receive for a long time or stop it every 3-5 sec
    /// 2 - Put the localSocket in a receive statement
    /// 3 - When the localSocket receive a message i parse it and read it
    ///     SYN = Message for start the connection
    ///         If i send a SYN message to 11.11.11.11:12356 and i receive back a message the connection is established
    ///     SYN_ACK = Message response for communicate that the connection is established
    ///     ACK = Message for confirm the connection

    ///     KEEP = Message for keep the connection open

    ///     EXCHANGE = Exchange LocalEP and PublicEP to the destination LocalEP and PublicEP
    ///         Note:
    ///             //TODO
    ///             For make it readable only from the destination i can Encrypt the message with the destination EndPoint, when a Peer receive the Message it use his IP for Decrypt it
    ///     EXCHANGE_ACK = Exchange LocalEP and PublicEP back to the source EndPoint (Local and Public)

    ///     REQUEST_SUPERPEERJSON = Send a request to the Peer for get his superPeers.json file
    ///     RESPONSE_SUPERPEERJSON = Send a response to the Peer for receive the json file


    /******************************************************************
    /// Check NAT Type
    ******************************************************************/

    /// I need to check the NAT Type for understand under which type of NAT i'm and for know which type of communication to use
    /// Try to connect to STUNS (3 Tests, 2 on the same STUN 1 on another STUN) for get the NetType

    /// Steps:
    /// 1 - Check if internet connection is available
    /// YES = Continue, NO = return a NOINTERNET Type
    ///     2 - Connect to a STUN from the STUN Server list
    ///     3 - Check the response
    ///     4 - Connect to the same STUN (i need this for check if every connection the port change)
    ///     5 - Check the response
    ///     6 - Connect to a new STUN Server
    ///     7 - Check the response
    ///     8 - Return the STUNResult with all the informations i get


    /******************************************************************
    /// superPeers.json
    ******************************************************************/

    /**
     * {
     *      "root":[
     *                  {"LocalIP": "192.168.1.1", "LocalPort": "500", "PublicIP": "109.10.10.10", "PublicPort": "500", "LastConnectionDateTime": "2021-11-11 10:10:10", "NetType": "FullCone_TRANSLATEIP"},
     *                  {"LocalIP": "192.168.1.1", "LocalPort": "500", "PublicIP": "109.10.10.10", "PublicPort": "500", "LastConnectionDateTime": "2021-11-11 10:10:10", "NetType": "OpenInternet"}
     *             ]
     * }
     */

    /// This file contains the informations of the possible servers to connect.
    /// LocalIP = The Local IP of the Peer
    /// LocalPort = The Local Port of the Peer where he is listening
    /// PublicIP = The Public IP of the Peer
    /// PublicPort = The Public Port of the Peer
    /// LastConnectionDateTime = The DateTime of the last connection, if is older than 30 days i try to connect, if fail i delete it
    /// NetType = The Type of the NAT:

    /************************************************************************************/
    ///     - UdpBlocked = Error with the connection, the machine could be not connected to the internet or there is someone that block the connection (firewall, ecc...)
    ///     Works with:
    ///
    ///     Note:
    ///         I can try to connect to a Proxy or a VPN for resolve this issue
    /************************************************************************************/

    /************************************************************************************/
    ///     - OpenInternet (1-2% of the network, not common) = Can connect to anyone because it is not behind a NAT and don't have a firewall so can receive packets from all.
    ///     Works with:
    ///         - OpenInternet
    ///         - FullCone_TRANSLATEIP
    ///         - FullCone_TRANSLATEIP_TRANSLATEPORT
    ///
    ///     Note:
    ///         This will act like a MasterPeer, a MasterPeer will exchange the IP and Port between the Clients
    /************************************************************************************/

    /************************************************************************************/
    ///     - SymmetricUdpFirewall = ???
    ///     Works with:
    ///
    ///     Note:
    ///
    /************************************************************************************/

    /************************************************************************************/
    ///     - RestrictedCone = ???
    ///     Works with:
    ///
    ///     Note:
    ///
    /************************************************************************************/

    /************************************************************************************/
    ///     - PortRestrictedCone = ???
    ///     Works with:
    ///
    ///     Note:
    ///
    /************************************************************************************/

    /************************************************************************************/
    ///     - Symmetric = ???
    ///     Works with:
    ///
    ///     Note:
    ///
    /************************************************************************************/

    /************************************************************************************/
    ///     - FullCone = Generic FullCone, need to test the FullConeType
    /************************************************************************************/

    /************************************************************************************/
    ///     - FullCone_TRANSLATEIP (40-50% of the network, very common) = This is behind a NAT but can communicate because the port doesn't change
    ///     Works with:
    ///         - OpenInternet
    ///         - FullCone_TRANSLATEIP
    ///         - FullCone_TRANSLATEIP_TRANSLATEPORT
    /************************************************************************************/

    /************************************************************************************/
    ///     - FullCone_TRANSLATEIP_TRANSLATEPORT (20% of the network, very common) = This is behind a NAT and can communicate even if the port changed (already taken or simply translated, but work because is fixed)
    ///     Works with:
    ///         - OpenInternet
    ///         - FullCone_TRANSLATEIP
    ///         - FullCone_TRANSLATEIP_TRANSLATEPORT
    /************************************************************************************/

    /// TODO I NEED TO TEST THIS. BECAUSE I CAN DO: CONNECT TO A OPEN INTERNET PEER, TRY TO EXCHANGE IP AND USE THE PORT USED WITH THE OPEN INTERNET
    /// TODO EXAMPLE:
    /// Normal situation:
    /// Local EP: 192.168.1.1:12356
    /// Public EP: 10.10.10.10:34532
    /// ---------------------------------------
    /// I send messages to 11.11.11.11:12356
    /// He will receive messages from 10.10.10.10:17426 (RANDOM PORT)
    /// If i exchange IPs the remote user will send packets to the OPEN INTERNET Port (17426) (I DON'T NEED TO USE STUN PORT AS USUAL, IT COULD BE CLOSED)
    /************************************************************************************/
    ///     - FullCone_TRANSLATEIP_TRANSLATEPORTDESTINATION (5% of the network, not very common) = This is behind a NAT and can't communicate because the remote address need to know the port associated with his IP
    ///     Works with:
    ///         - OpenInternet
    ///         - (All Peers without firewall)
    ///     Note:
    ///         With this type of NAT the only way to let it work is try to use a Proxy or a VPN, at least i can use a Brute Force on the Ports but is NOT RECOMMENDED
    /************************************************************************************/

    /************************************************************************************/
    ///     - FullCone_TRANSLATEIP_TRANSLATEPORTFREE (5% of the network, not very common) = This is behind a NAT and can communicate because the port is only changed (already taken or simply translated, but work because is fixed)
    ///     Works with:
    ///
    ///     Note:
    ///         With this type of NAT the only way to let it work is try to use a Proxy or a VPN, at least i can use a Brute Force on the Ports but is NOT RECOMMENDED
    /************************************************************************************/


    /******************************************************************
    /// Peers types
    ******************************************************************/

    /******************************************************************/
    /// MasterPeer:
    /// Level: 1
    ///     NetTypes:
    ///         - OpenInternet
    ///
    ///     Characteristics:
    ///         - Can do all the things of SuperPeer
    ///         - Can exchange IP and Port for let the communication start between Peers
    /******************************************************************/

    /******************************************************************/
    /// SuperPeer:
    /// Level: 2
    ///     NetTypes:
    ///         - FullCone_TRANSLATEIP
    ///         - FullCone_TRANSLATEIP_TRANSLATEPORT
    ///
    ///     Characteristics:
    ///         - Can do all the things of Peer
    ///         - Can act like a proxy for redirect messages and let Peers communicate
    /******************************************************************/

    /******************************************************************/
    /// Peer:
    /// Level: 3
    ///     NetTypes:
    ///         - FullCone_TRANSLATEIP_TRANSLATEPORTDESTINATION
    ///         - FullCone_TRANSLATEIP_TRANSLATEPORTFREE
    ///
    ///     Characteristics:
    ///         - Can't directly communicate with other Peers
    ///         - Act like a Client, this don't redirect messages, only send and receive messages for him self
    /******************************************************************/



    // TODO FOR ADD A NEW USER INTO THE NETWORK I CAN:
    //  THE USER ALREADY IN THE NETWORK CREATE A QR CODE WITH ALL HIS CONNECTED PEERS, ENCRYPT; CONVERT BASE64 ECC...
    //  THE NEW USER SCAN IT AND START TO CONNECT TO ALL THE PEERS AND REQUEST THEIR JSON

    // TODO I THINK THIS COULD BE A EASY AND STABLE METHOD

    // TODO FOR INSERT A PC I CAN USE THE QR CODE IMAGE OR THE QR CODE STRING WITH COPY PASTE


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        defaultFilesPath = this.getFilesDir();

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.FOREGROUND_SERVICE}, 1);

        WriteTestJSON();

        registerReceiver(broadcastReceiver, new IntentFilter(BackgroundService.BROADCAST_ACTION));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, BackgroundService.class));
        } else {
            startService(new Intent(this, BackgroundService.class));
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            if(extras != null) {
                TextView STUNInfo_NetType = findViewById(R.id.STUNInfo_NetType);
                STUNInfo_NetType.setText(extras.getString("NetType"));

                TextView STUNInfo_LocalEP = findViewById(R.id.STUNInfo_LocalEP);
                STUNInfo_LocalEP.setText(extras.getString("LocalEP"));

                TextView STUNInfo_PublicEP = findViewById(R.id.STUNInfo_PublicEP);
                STUNInfo_PublicEP.setText(extras.getString("PublicEP"));
            }
        }
    };

    private void WriteTestJSON() {
        try {
            // JSON object. Key value pairs are unordered. JSONObject supports java.util.Map interface.
            JSONObject objRoot = new JSONObject();
            JSONArray company = new JSONArray();

            // JSON object. Key value pairs are unordered. JSONObject supports java.util.Map interface.
            JSONObject obj = new JSONObject();
            obj.put("LocalIP", "167.86.122.45");
            obj.put("LocalPort", "12356");
            obj.put("PublicIP", "167.86.122.45");
            obj.put("PublicPort", "12356");
            obj.put("LastConnectionDateTime", "2022-02-16 11:54:30");
            obj.put("NetType", "OpenInternet");

            company.put(0, obj);

            objRoot.put("root", company);


            try {
                // Constructs a FileWriter given a file name, using the platform's default charset
                FileWriter file = new FileWriter(this.getFilesDir() + "/" + "superPeers.json");
                file.write(objRoot.toString());

                file.flush();
                file.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void BtnTextMessageNOACKSPREADTEST(View view) {
        Intent pageTextMessageNOACKSPREAD = new Intent(this, TEXTMessageNOACKSPREADActivity.class);
        startActivity(pageTextMessageNOACKSPREAD);
    }

    public void BtnTextMessageNOACKTEST(View view) {
        Intent pageTextMessageNOACK = new Intent(this, TextMessageNOACKActivity.class);
        startActivity(pageTextMessageNOACK);
    }

    public void BtnExchangeIPTEST(View view) {
        Intent pageExchangeIP = new Intent(this, ExchangeActivity.class);
        startActivity(pageExchangeIP);
    }

    public void BtnSuperJsonRequestTEST(View view) {
        Intent pageSuperJsonRequest = new Intent(this, SuperPeerJsonActivity.class);
        startActivity(pageSuperJsonRequest);
    }
}