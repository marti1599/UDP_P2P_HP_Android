package com.example.udpp2p;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.example.udpp2p.message.MessageType;
import com.example.udpp2p.message.TEXTMessageNOACKSPREAD;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class TEXTMessageNOACKSPREADActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textmessage_noackspreadactivity);
    }

    public void BtnTextMessageNOACKSPREADTEST(View view) {
        EditText EditText_TextMessage = findViewById(R.id.EditText_TextMessage);

        //Send a EXCHANGE Message to a connected Peer
        EditText localDestinationIPPort = findViewById(R.id.ExitTextLocalIPPortDestination);
        String[] splitLocalDestination = localDestinationIPPort.getText().toString().split(":");
        InetSocketAddress localDestinationEP = new InetSocketAddress(splitLocalDestination[0], Integer.parseInt(splitLocalDestination[1]));

        EditText publicDestinationIPPort = findViewById(R.id.ExitTextPublicIPPortDestination);
        String[] splitPublicDestination = publicDestinationIPPort.getText().toString().split(":");
        InetSocketAddress publicDestinationEP = new InetSocketAddress(splitPublicDestination[0], Integer.parseInt(splitPublicDestination[1]));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    // Send a TEXTMessageNOACK Message
                    TEXTMessageNOACKSPREAD textMessageNOACKSPREAD = new TEXTMessageNOACKSPREAD(MessageType.TEXTMessageNOACKSPREAD, EditText_TextMessage.getText().toString(), BackgroundService.stunResult.localEP, BackgroundService.stunResult.publicEP, localDestinationEP, publicDestinationEP);
                    byte[] msgSendTEXTMessageNOACKSPREAD = textMessageNOACKSPREAD.toJSONString().getBytes();

                    for (Peer peer: BackgroundService.connected_Peers.values()) {
                        DatagramPacket packetSend = new DatagramPacket(msgSendTEXTMessageNOACKSPREAD, msgSendTEXTMessageNOACKSPREAD.length, peer.EndPoint);
                        BackgroundService.socket.send(packetSend);

                        Log.d("CONNECTION", "Sent: " + textMessageNOACKSPREAD.toJSONString() + " To: " + peer.EndPoint.toString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}