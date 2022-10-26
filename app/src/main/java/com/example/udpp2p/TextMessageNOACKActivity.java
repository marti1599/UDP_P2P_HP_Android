package com.example.udpp2p;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.udpp2p.message.MessageType;
import com.example.udpp2p.message.TEXTMessageNOACK;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class TextMessageNOACKActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_message_noackactivity);

        ArrayList<String> arraySpinner = new ArrayList<>();
        for (InetSocketAddress address : BackgroundService.connected_Peers.keySet()) {
            arraySpinner.add(address.toString());
        }
        Spinner s = (Spinner) findViewById(R.id.Spinner_ConnectedPeersList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
    }

    public void BtnTextMessageNOACKTEST(View view) {
        EditText EditText_TextMessage = findViewById(R.id.EditText_TextMessage);
        Spinner s = (Spinner) findViewById(R.id.Spinner_ConnectedPeersList);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    /// Destination Peer
                    String[] str = s.getSelectedItem().toString().split(":");
                    Peer peer = BackgroundService.connected_Peers.get(new InetSocketAddress(str[0].substring(1), Integer.parseInt(str[1])));

                    // Send a TEXTMessageNOACK Message
                    TEXTMessageNOACK textMessageNOACK = new TEXTMessageNOACK(MessageType.TEXTMessageNOACK, EditText_TextMessage.getText().toString());
                    byte[] msgSendTEXTMessageNOACK = textMessageNOACK.toJSONString().getBytes();

                    DatagramPacket packetSend = new DatagramPacket(msgSendTEXTMessageNOACK, msgSendTEXTMessageNOACK.length, peer.EndPoint);
                    BackgroundService.socket.send(packetSend);

                    Log.d("CONNECTION", "Sent: " + textMessageNOACK.toJSONString() + " To: " + peer.EndPoint.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}