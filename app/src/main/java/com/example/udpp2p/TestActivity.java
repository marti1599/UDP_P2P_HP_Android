package com.example.udpp2p;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.udpp2p.STUN.STUN_Client;
import com.example.udpp2p.STUN.STUN_NetType;
import com.example.udpp2p.STUN.STUN_Result;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestActivity extends AppCompatActivity {
    static String NameSuperPeersJson = "SuperPeers.json";
    String PathSuperPeersJson;

    boolean stopReceiving = true;
    boolean stopSend = true;

    DatagramSocket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        PathSuperPeersJson = getFilesDir().getAbsolutePath() + "/" + NameSuperPeersJson;

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET}, 1);

        ArrayList<String> localIP = Utility.GetLocalIPAddress();

        TextView TextView_STUNNetType_Value = findViewById(R.id.TextView_STUNNetType_Value);
        TextView TextView_STUNLocalIP_Value = findViewById(R.id.TextView_STUNLocalIP_Value);
        TextView TextView_STUNPublicIP_Value = findViewById(R.id.TextView_STUNPublicIP_Value);

        for (String ip: localIP) {
            STUNResult result = ConnectToSTUN(ip, 12356);

            if(result.netType != STUN_NetType.UdpBlocked)
            {
                socket = result.socket;
                TextView_STUNNetType_Value.setText(TextView_STUNNetType_Value.getText() + String.valueOf(result.netType) + "\n");
                TextView_STUNLocalIP_Value.setText(TextView_STUNLocalIP_Value.getText() + String.valueOf(result.localEP) + "\n");
                TextView_STUNPublicIP_Value.setText(TextView_STUNPublicIP_Value.getText() + String.valueOf(result.publicEP) + "\n");
            }
        }

        //PathSuperPeersJson = this.getFilesDir().getAbsolutePath() + NameSuperPeersJson;
        /*if (CheckIfSuperPeersJsonExist()) {
            //SuperPeers.json exist, start to connect
            ArrayList<SuperPeerInfoJSON> superPeersListInfo = ParseSuperPeersJson();


        } else {
            //SuperPeers.json doesn't exist, i need a friend for get into the network
        }*/
    }


    private boolean CheckIfSuperPeersJsonExist() {
        return new File(PathSuperPeersJson).exists();
    }

    /*private ArrayList<SuperPeerInfoJSON> ParseSuperPeersJson() {
        ArrayList<SuperPeerInfoJSON> resultSuperPeersListInfo = new ArrayList<SuperPeerInfoJSON>();

        try {
            File superPeersJSON = new File(PathSuperPeersJson);
            BufferedReader br = new BufferedReader(new FileReader(superPeersJSON));

            String json = "";
            String line;
            while ((line = br.readLine()) != null) {
                json += line;
            }

            JSONArray jsonArraySuperPeers = new JSONObject(json).getJSONArray("superPeers");
            //JSONArray jsonArrayPeers = new JSONObject(json).getJSONArray("peers");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (int i = 0; i < jsonArraySuperPeers.length(); i++) {
                JSONObject jsonSuperPeer = jsonArraySuperPeers.getJSONObject(i);
                resultSuperPeersListInfo.add(new SuperPeerInfoJSON(jsonSuperPeer.getString("IP"), jsonSuperPeer.getInt("Port"), sdf.parse(jsonSuperPeer.getString("LastConnectionDateTime"))));
            }

            /*for (int i = 0; i < jsonArrayPeers.length(); i++) {
                JSONObject jsonPeer = jsonArrayPeers.getJSONObject(i);
                resultSuperPeersListInfo.add(new SuperPeerInfoJSON(jsonPeer.getString("IP"), jsonPeer.getInt("Port"), jsonPeer.getString("LastConnectionDateTime")));
            }*/
        /*} catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return resultSuperPeersListInfo;
    }*/

    //**************************************************************
    //Test Functions
    //**************************************************************

    private void StartThreadSendLoop(String IP, int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String msg = "SYN";
                    byte[] buf = msg.getBytes();

                    InetAddress address = InetAddress.getByName(IP);

                    while(!stopSend) {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                        socket.send(packet);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView TextViewLog = findViewById(R.id.TextView_LogReceivedMessages);
                                TextViewLog.append("Sent: " + msg + "\n");
                                TextViewLog.append("To: " + address.toString() + ":" + port + "\n");
                            }
                        });

                        Thread.sleep(3000);
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void StartThreadScanPort(String IP, int StartPort, int EndPort) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String msg = "SYN";
                    byte[] buf = msg.getBytes();

                    InetAddress address = InetAddress.getByName(IP);

                    for(int i = StartPort; i < EndPort; i++)
                    {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, i);
                        socket.send(packet);

                        int finalI = i;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView TextView_LogReceivedMessages = findViewById(R.id.TextView_LogReceivedMessages);
                                TextView_LogReceivedMessages.append("Sent SYN to " + address.toString() + ":" + finalI + "\n");
                            }
                        });
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView TextView_LogReceivedMessages = findViewById(R.id.TextView_LogReceivedMessages);
                            TextView_LogReceivedMessages.append("Done\n");
                        }
                    });
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void StartThreadReceive(int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    //Work on local network
                    InetSocketAddress addressBind = new InetSocketAddress("0.0.0.0", port);

                    //InetSocketAddress addressBind = new InetSocketAddress(InetAddress.getLocalHost(), port);

                    socket.setSoTimeout(3000);

                    byte[] buf = new byte[4096];

                    while (!stopReceiving) {
                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView TextView_LogReceivedMessages = findViewById(R.id.TextView_LogReceivedMessages);
                                    TextView_LogReceivedMessages.append("Listening on " + socket.getLocalSocketAddress().toString() + "...\n");
                                }
                            });

                            DatagramPacket packet = new DatagramPacket(buf, buf.length, addressBind.getAddress(), addressBind.getPort());
                            socket.receive(packet);

                            InetAddress address = packet.getAddress();
                            int port = packet.getPort();
                            String received = new String(packet.getData(), 0, packet.getLength());

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView TextView_LogReceivedMessages = findViewById(R.id.TextView_LogReceivedMessages);
                                    TextView_LogReceivedMessages.append("Received: " + received + "\n");
                                    TextView_LogReceivedMessages.append("From: " + address.toString() + ":" + port + "\n");
                                }
                            });
                        }catch (SocketTimeoutException e){
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

    public void BtnStartReceive(View view) {
        stopReceiving = false;
        EditText editTextPort = findViewById(R.id.EditText_PortValue);
        int port = Integer.parseInt(editTextPort.getText().toString());
        StartThreadReceive(port);
    }

    public void BtnStartSend(View view) {
        stopSend = false;

        EditText editTextLocalIP = findViewById(R.id.EditText_LocalIPValue);
        EditText editTextLocalPort = findViewById(R.id.EditText_LocalPortValue);

        String LocalIP = editTextLocalIP.getText().toString();
        int LocalPort = Integer.parseInt(editTextLocalPort.getText().toString());
        StartThreadSendLoop(LocalIP, LocalPort);

        EditText editTextRemoteIP = findViewById(R.id.EditText_RemoteIPValue);
        EditText editTextRemotePort = findViewById(R.id.EditText_RemotePortValue);

        String RemoteIP = editTextRemoteIP.getText().toString();
        int RemotePort = Integer.parseInt(editTextRemotePort.getText().toString());
        StartThreadSendLoop(RemoteIP, RemotePort);
    }

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

                    /*boolean netTypeEquals = false;
                    boolean epLocalEquals = false;
                    boolean epPublicEquals = false;


                    if(netType_1.equals(netType_2))
                    {
                        netTypeEquals = true;
                    }
                    if(epLocal_1.equals(epLocal_2))
                    {
                        epLocalEquals = true;
                    }
                    if(epPublic_1.equals(epPublic_2))
                    {
                        epPublicEquals = true;
                    }*/

                    /*STUN_NetType netTypeResult = netType_1;

                    if(netType_1.equals(STUN_NetType.FullCone) || netType_2.equals(STUN_NetType.FullCone)) {
                        if (epPublic_1.getPort() == port && epPublic_2.getPort() == port) {
                            netTypeResult = STUN_NetType.FullCone_CHANGEIP;
                        } else if ((epPublic_1.getPort() != port || epPublic_2.getPort() != port) && epPublic_1.getPort() == epPublic_2.getPort()) {
                            netTypeResult = STUN_NetType.FullCone_CHANGEIP_CHANGEFIXEDPORT;
                        } else if ((epPublic_1.getPort() != port || epPublic_2.getPort() != port) && epPublic_1.getPort() != epPublic_2.getPort()) {
                            netTypeResult = STUN_NetType.FullCone_CHANGEIP_CHANGERANDOMPORT;
                        }
                    }*/

                    STUN_NetType netTypeResult = netType_1;

                    if(netType_1.equals(STUN_NetType.FullCone) || netType_2.equals(STUN_NetType.FullCone) || netType_3.equals(STUN_NetType.FullCone))
                    {
                        if (epPublic_1.getPort() == port && epPublic_2.getPort() == port && epPublic_3.getPort() == port)
                        {
                            //All the port results are the same of the passed one, so the port is not translated and fixed
                            netTypeResult = STUN_NetType.FullCone_TRANSLATEIP;
                        }
                        else if ((epPublic_1.getPort() != port || epPublic_2.getPort() != port || epPublic_3.getPort() != port)
                                && epPublic_1.getPort() == epPublic_2.getPort()
                                && epPublic_1.getPort() == epPublic_3.getPort())
                        {
                            //The port is different from the passed one, but is always the same (Fixed)
                            netTypeResult = STUN_NetType.FullCone_TRANSLATEIP_TRANSLATEPORT;
                        }
                        else if ((epPublic_1.getPort() != port || epPublic_2.getPort() != port || epPublic_3.getPort() != port)
                                && epPublic_1.getPort() == epPublic_2.getPort()
                                && epPublic_1.getPort() != epPublic_3.getPort())
                        {
                            //The port change according to the destination
                            netTypeResult = STUN_NetType.FullCone_TRANSLATEIP_TRANSLATEPORTDESTINATION;
                        }
                        else if ((epPublic_1.getPort() != port || epPublic_2.getPort() != port || epPublic_3.getPort() != port)
                                && epPublic_1.getPort() != epPublic_2.getPort()
                                && epPublic_1.getPort() != epPublic_3.getPort())
                        {
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

            if(value != null) {
                return value;
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void BtnStopReceive(View view) {
        stopReceiving = true;
    }

    public void BtnStopSend(View view) {
        stopSend = true;
    }

    public void BtnStartScanPort(View view) {
        EditText editTextScanIP = findViewById(R.id.EditText_ScanIPValue);
        StartThreadScanPort(editTextScanIP.getText().toString(), 1, 1000);
    }
}
class STUNResult
{
    public STUN_NetType netType;
    public InetSocketAddress localEP;
    public InetSocketAddress publicEP;
    public DatagramSocket socket;

    public STUNResult(STUN_NetType netType, InetSocketAddress localEP, InetSocketAddress publicEP, DatagramSocket socket)
    {
        this.netType = netType;
        this.localEP = localEP;
        this.publicEP = publicEP;
        this.socket = socket;
    }
}