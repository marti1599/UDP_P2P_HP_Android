package com.example.udpp2p.STUN;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.LocalDateTime;

/**
 * This class implements STUN client. Defined in RFC 3489.
 *
 * Example:
 *    Code:
 *----------------------------------------------------------------------
 * // Create new socket for STUN client.
 * Socket socket = new Socket(AddressFamily.InterNetwork,SocketType.Dgram,ProtocolType.Udp);
 * socket.Bind(new IPEndPoint(IPAddress.Any,0));
 *
 * // Query STUN server
 * STUN_Result result = STUN_Client.Query("stunserver.org",3478,socket);
 * if(result.NetType != STUN_NetType.UdpBlocked){
 * // UDP blocked or !!!! bad STUN server
 * }
 * else{
 * IPEndPoint publicEP = result.PublicEndPoint;
 * // Do your stuff
 * }
 */
public class STUN_Client {

   /**
    * Gets NAT info from STUN server.
    *
    * @param host STUN server name or IP
    * @param port STUN server port. Default port is 3478
    * @param socket UDP socket to use
    * @return Returns UDP network info
    * @exception Exception Throws exception if unexpected error happens
    */
   public static STUN_Result Query(String host, int port, DatagramSocket socket) throws SocketException {
      if (host == null) {
         throw new NullPointerException("host");
      }
      if (socket == null) {
         throw new NullPointerException("socket");
      }
      if (port < 1) {
         throw new NullPointerException("Port value must be >= 1 !");
      }

      //TODO Check Protocol
      /*if(socket.ProtocolType != ProtocolType.Udp){
         throw new NullPointerException("Socket must be UDP socket !");
      }*/

      InetSocketAddress remoteEndPoint = new InetSocketAddress(host, port);

      socket.setSoTimeout(3000);

            /**
                In test I, the client sends a STUN Binding Request to a server, without any flags set in the
                CHANGE-REQUEST attribute, and without the RESPONSE-ADDRESS attribute. This causes the server
                to send the response back to the address and port that the request came from.

                In test II, the client sends a Binding Request with both the "change IP" and "change port" flags
                from the CHANGE-REQUEST attribute set.

                In test III, the client sends a Binding Request with only the "change port" flag set.

                                    +--------+
                                    |  Test  |
                                    |   I    |
                                    +--------+
                                         |
                                         |
                                         V
                                        /\              /\
                                     N /  \ Y          /  \ Y             +--------+
                      UDP     <-------/Resp\--------->/ IP \------------->|  Test  |
                      Blocked         \ ?  /          \Same/              |   II   |
                                       \  /            \? /               +--------+
                                        \/              \/                    |
                                                         | N                  |
                                                         |                    V
                                                         V                    /\
                                                     +--------+  Sym.      N /  \
                                                     |  Test  |  UDP    <---/Resp\
                                                     |   II   |  Firewall   \ ?  /
                                                     +--------+              \  /
                                                         |                    \/
                                                         V                     |Y
                              /\                         /\                    |
               Symmetric  N  /  \       +--------+   N  /  \                   V
                  NAT  <--- / IP \<-----|  Test  |<--- /Resp\               Open
                            \Same/      |   I    |     \ ?  /               Internet
                             \? /       +--------+      \  /
                              \/                         \/
                              |                           |Y
                              |                           |
                              |                           V
                              |                           Full
                              |                           Cone
                              V              /\
                          +--------+        /  \ Y
                          |  Test  |------>/Resp\---->Restricted
                          |   III  |       \ ?  /
                          +--------+        \  /
                                             \/
                                              |N
                                              |       Port
                                              +------>Restricted

            */

      // Test I
      STUN_Message test1 = new STUN_Message();
      test1.setType(STUN_MessageType.BindingRequest);
      STUN_Message test1response = DoTransaction(test1, socket, remoteEndPoint);

      // UDP blocked.
      if (test1response == null) {
         return new STUN_Result(STUN_NetType.UdpBlocked, null);
      } else {
         // Test II
         STUN_Message test2 = new STUN_Message();
         test2.setType(STUN_MessageType.BindingRequest);
         test2.setChangeRequest(new STUN_t_ChangeRequest(true, true));

         // No NAT.
         if (socket.getLocalAddress().equals(test1response.getMappedAddress())) {
            STUN_Message test2Response = DoTransaction(test2, socket, remoteEndPoint);
            // Open Internet.
            if (test2Response != null) {
               return new STUN_Result(STUN_NetType.OpenInternet, test1response.getMappedAddress());
            }
            // Symmetric UDP firewall.
            else {
               return new STUN_Result(STUN_NetType.SymmetricUdpFirewall, test1response.getMappedAddress());
            }
         }
         // NAT
         else {
            STUN_Message test2Response = DoTransaction(test2, socket, remoteEndPoint);
            // Full cone NAT.
            if (test2Response != null) {
               return new STUN_Result(STUN_NetType.FullCone, test1response.getMappedAddress());
            } else {
                        /*
                            If no response is received, it performs test I again, but this time, does so to
                            the address and port from the CHANGED-ADDRESS attribute from the response to test I.
                        */

               // Test I(II)
               STUN_Message test12 = new STUN_Message();
               test12.setType(STUN_MessageType.BindingRequest);

               STUN_Message test12Response = DoTransaction(test12, socket, test1response.getChangedAddress());
               if (test12Response == null) {
                  throw new NullPointerException("STUN Test I(II) dind't get resonse !");
               } else {
                  // Symmetric NAT
                  if (!test12Response.getMappedAddress().equals(test1response.getMappedAddress())) {
                     return new STUN_Result(STUN_NetType.Symmetric, test1response.getMappedAddress());
                  } else {
                     // Test III
                     STUN_Message test3 = new STUN_Message();
                     test3.setType(STUN_MessageType.BindingRequest);
                     test3.setChangeRequest(new STUN_t_ChangeRequest(false, true));

                     STUN_Message test3Response = DoTransaction(test3, socket, test1response.getChangedAddress());
                     // Restricted
                     if (test3Response != null) {
                        return new STUN_Result(STUN_NetType.RestrictedCone, test1response.getMappedAddress());
                     }
                     // Port restricted
                     else {
                        return new STUN_Result(STUN_NetType.PortRestrictedCone, test1response.getMappedAddress());
                     }
                  }
               }
            }
         }
      }
   }

   private void GetSharedSecret() {
      /*
       *) Open TLS connection to STUN server.
       *) Send Shared Secret request.
       */

            /*
            using(SocketEx socket = new SocketEx()){
                socket.RawSocket.ReceiveTimeout = 5000;
                socket.RawSocket.SendTimeout = 5000;

                socket.Connect(host,port);
                socket.SwitchToSSL_AsClient();

                // Send Shared Secret request.
                STUN_Message sharedSecretRequest = new STUN_Message();
                sharedSecretRequest.Type = STUN_MessageType.SharedSecretRequest;
                socket.Write(sharedSecretRequest.ToByteData());

                // TODO: Parse message

                // We must get  "Shared Secret" or "Shared Secret Error" response.

                byte[] receiveBuffer = new byte[256];
                socket.RawSocket.Receive(receiveBuffer);

                STUN_Message sharedSecretRequestResponse = new STUN_Message();
                if(sharedSecretRequestResponse.Type == STUN_MessageType.SharedSecretResponse){
                }
                // Shared Secret Error or Unknown response, just try again.
                else{
                    // TODO: Unknown response
                }
            }*/
   }

   /**
    * Does STUN transaction. Returns transaction response or null if transaction failed
    * @param request STUN message
    * @param socket Socket to use for send/receive
    * @param remoteEndPoint Remote end point
    * @return Returns transaction response or null if transaction failed
    */
   private static STUN_Message DoTransaction(STUN_Message request, DatagramSocket socket, InetSocketAddress remoteEndPoint) {
      byte[] requestBytes = request.ToByteData();
      LocalDateTime startTime = LocalDateTime.now();
      // We do it only 2 sec and retransmit with 100 ms.
      while (startTime.plusSeconds(2).isAfter(LocalDateTime.now())) {
         try {
            DatagramPacket packet = new DatagramPacket(requestBytes, requestBytes.length, remoteEndPoint);
            socket.send(packet);

            // We got response.
            //if(socket.Poll(100,SelectMode.SelectRead)){
            byte[] receiveBuffer = new byte[512];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);

            // Parse message
            STUN_Message response = new STUN_Message();
            response.Parse(receiveBuffer);

            // Check that transaction ID matches or not response what we want.
            if (request.getTransactionID().equals(response.getTransactionID())) {
               return response;
            }
            //}
         } catch (Exception e) {
         }
      }

      return null;
   }
}
