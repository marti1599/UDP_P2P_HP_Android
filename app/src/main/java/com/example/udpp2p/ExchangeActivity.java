package com.example.udpp2p;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.udpp2p.STUN.STUN_NetType;
import com.example.udpp2p.message.EXCHANGEMessage;
import com.example.udpp2p.message.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class ExchangeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange);
    }

    // TODO if i'm under the same router (working with local IP this will never be used)
    public void BtnExchangeIPTEST(View view) {
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
                    if(!BackgroundService.connected_Peers.keySet().contains(localDestinationEP) && !BackgroundService.connected_Peers.keySet().contains(publicDestinationEP)) {
                        BackgroundService.StartThreadConnect(localDestinationEP, publicDestinationEP);
                        /// Send Exchange Message to all the OpenInternet Peers
                        for (Peer peer : BackgroundService.connected_Peers.values()) {
                            if (peer.NetType == STUN_NetType.OpenInternet) {

                                /// Send a EXCHANGE Message
                                EXCHANGEMessage messageEXCHANGE = new EXCHANGEMessage(MessageType.EXCHANGE, BackgroundService.stunResult.localEP, BackgroundService.stunResult.publicEP, localDestinationEP, publicDestinationEP);
                                byte[] msgSendEXCHANGE = messageEXCHANGE.toJSONString().getBytes();

                                DatagramPacket packetSendPublic = new DatagramPacket(msgSendEXCHANGE, msgSendEXCHANGE.length, peer.EndPoint);
                                BackgroundService.socket.send(packetSendPublic);

                                /// Add message to the message sent list
                                MessagesReceivedUtility.AddMessageToFile(MessagesReceivedUtility.ReadFile(), messageEXCHANGE);

                                Log.d("CONNECTION", "Sent: " + messageEXCHANGE.toJSONString() + " To: " + peer.EndPoint.toString());
                            }
                        }
                    }else  {
                        Log.d("CONNECTION", "Already connected to this Peer");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}