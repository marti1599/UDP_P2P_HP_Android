package com.example.udpp2p;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.udpp2p.message.MessageType;
import com.example.udpp2p.message.SUPERPEERJSON_REQUESTMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class SuperPeerJsonActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_peer_json);

        ArrayList<String> arraySpinner = new ArrayList<>();
        for (InetSocketAddress address : BackgroundService.connected_Peers.keySet()) {
            arraySpinner.add(address.toString());
        }
        Spinner s = (Spinner) findViewById(R.id.Spinner_ConnectedPeersList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
    }

    public void BtnSuperJsonRequestTEST(View view) {
        Spinner s = (Spinner) findViewById(R.id.Spinner_ConnectedPeersList);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    //Destination Peer
                    String[] str = s.getSelectedItem().toString().split(":");
                    Peer peer = BackgroundService.connected_Peers.get(new InetSocketAddress(str[0].substring(1), Integer.parseInt(str[1])));

                    //Send a SUPERJSONREQUEST Message
                    SUPERPEERJSON_REQUESTMessage superpeerjson_requestMessage = new SUPERPEERJSON_REQUESTMessage(MessageType.SUPERPEERJSON_REQUEST, BackgroundService.stunResult.localEP, BackgroundService.stunResult.publicEP, peer.NetType, 20);
                    byte[] msgSendSUPERPEERJSON_RESPONSE = superpeerjson_requestMessage.toJSONString().getBytes();

                    DatagramPacket packetSend = new DatagramPacket(msgSendSUPERPEERJSON_RESPONSE, msgSendSUPERPEERJSON_RESPONSE.length, peer.EndPoint);
                    BackgroundService.socket.send(packetSend);

                    Log.d("CONNECTION", "Sent: " + superpeerjson_requestMessage.toJSONString() + " To: " + peer.EndPoint.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}