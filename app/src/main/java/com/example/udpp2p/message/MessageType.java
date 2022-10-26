package com.example.udpp2p.message;

public enum MessageType {
   /**
    * @param SYN Message for start the connection
    */
   SYN,

   /**
    * @param SYN_ACK Response to a SYN Message, confirm connection
    */
   SYN_ACK,

   /**
    * @param ACK Send back the confirm of the connection
    */
   ACK,

   /**
    * @param EXCHANGE Exchange IP and Port to the destination endpoint
    */
   EXCHANGE,

   /**
    * @param KEEP Send a message for keep the connection open
    */
   KEEP,

   /**
    * @param SUPERPEERJSON_REQUEST Send a request to the Peer for get his superPeers.json file
    */
   SUPERPEERJSON_REQUEST,

   /**
    * @param SUPERPEERJSON_RESPONSE Send a response to the Peer for receive the json file
    */
   SUPERPEERJSON_RESPONSE,

   /**
    * @param TEXTMessageNOACK Send a text message to the destination user without waiting for a ACK
    */
   TEXTMessageNOACK,

   /**
    * @param TEXTMessageNOACKSPREAD Send a text message to all the connected Peers until it reaches the destination user without waiting for a ACK
    */
   TEXTMessageNOACKSPREAD,

   /**
    * @param TEXTMessageACK Send a text message to the destination user and if all goes well i need to receive a ACK
    */
   TEXTMessageACK,

   /**
    * @param TEXTMessageACK_ACK Send the ACK for the TEXTMessageACK Message
    */
   TEXTMessageACK_ACK,
}
