##### UDP_P2P_HP_Android
 Final project for test P2P Hole Punching
 
 This is the final project i have made for learn how P2P hole punching works.
 
 The App is made of different parts:
 - Background service
 - STUN
 - Messages
 - SuperPeer
 - Exchange Activity
 - Text Message NO ACK Activity
 - Text Message NO ACK Spread Activity
 - Peer
 
 ## Background Service
  The background service is a service that run in background (as the name says) and try to connect to peers inside the superPeers.json file.
  
  The steps the background service do are:
  - Check the internet connection (This is because for let all works i need the internet connection)
  - Check network type using STUN (This is for get the network type i am currently on)
  - Connect to Peers (Connect to peers inside the superPeers.json file)

  The role of this in the app is to connect to new peers for create new connections. I put some settigns like after how many seconds retry to connect, the max number of connected peers, the number of connection to try in parallel ecc...

 ## STUN
  The STUN is a server that when receive a request respond with the IP and Port (EndPoint) of the sender.
  I need this because when using connections like a home Wi-Fi there could be multiple routers so i need an external machine that when i send a request throught all those routers give me back the public EP for know which is the EP that the users outside the network sees.
  
  The STUN code i founded online was in C#. I tried it with my C# software but for test it i need 2 PCs. So i decided to translate all the scripts from C# to Java and i customized some of them because it doesn't handle all the cases.
  
  The network can be of different types:
  - UdpBlocked
	The UDP connection is always blocked
  - OpenInternet
	In this case the machine is totally free. I had a VPS and this machine had a OpenInternet connection. It means that there is no NAT, The IP is public (local and public IP are the same) and there is no firewall
  - SymmetricUdpFirewall
	In this case there is no NAT, the IP is public but symmetric UDP firewall. (I have not found a network like this so i have never tested it)
  - RestrictedCone
	A restricted cone NAT is one where all requests from the same internal IP address and port are mapped to the same external IP address and port. (I have not found a network like this so i have never tested it)
  - PortRestrictedCone
	 A port restricted cone NAT is like a restricted cone NAT, but the restriction includes port numbers. (I have not found a network like this so i have never tested it)
  - Symmetric
	A symmetric NAT is one where all requests from the same internal IP address and port, to a specific destination IP address and port, are mapped to the same external IP address and port. (I have not found a network like this so i have never tested it)
  - FullCone
	A full cone NAT is one where all requests from the same internal IP address and port are mapped to the same external IP address and port. (This is the most common network type)
	
  This was the base network type. I have found that the FullCone network type need to be subdivided in subcategories. For know this subcategories i added other STUN requests for check if something change.
  - FullCone_TRANSLATEIP
	In this network type when a connection go outside the LAN only the IP will be translated
  - FullCone_TRANSLATEIP_TRANSLATEPORT
	In this case when i connect to a STUN server the IP and Port change but is fixed (Even if i retry to connect or connect to another STUN server the result is the same)
  - FullCone_TRANSLATEIP_TRANSLATEPORTDESTINATION
	In this network type the connection change every time i connect to a different machine. (So i can't execute the UDP hole punching because the STUN server will have a port and the receiver machine will have a different port)
  - FullCone_TRANSLATEIP_TRANSLATEPORTFREE
	In this case the port change every connection so i can't use the UDP hole punching technique

 ## Messages
  The messages are just a list of messages type that can be used for communicate
  
  Messages types:
  - SYN
	Message for start the connection
  - SYN_ACK 
	Response to the SYN message that send the peer info
  - ACK
	Confirmation of the connection with the peer info
  - Exchange
	Exchnage EP with the destinatio EP
  - KEEP
	Keep the connection open
  - SUPERPEERJSON_REQUEST
	Request the receiver superPeers.json list
  - SUPERPEERJSON_RESPONSE
	Response with the list of peers in the superPeers.json file
  - TEXTMessageNOACK
	Send a message to the destination peer without waiting for and ACK
  - TEXTMessageNOACKSPREAD
	Spread a message to all the peers for reach the destination peer. This doesn't wait for an ACK
  - TEXTMessageACK (NOT IMPLEMENTED)
	Send a message to the destination peer and wait for and ACK  (confirm)
  - TEXTMessageACK_ACK (NOT IMPLEMENTED)
	Confirmation of the TEXTMessageACK message
	
	The base massage have:
	- UUID (Unique indentifier)
	- DateTime (When the message was sent)
	- Message type
	
	The UUID and DateTime are used for know if i have already received this message
	
 ## SuperPeer
   I decided to subdivide the peers based on their level of freedom.
   
   Peer types:
   - Master Peer
	 This peer type have full freedom so is a OpenInternet peer. He can communicate with everyone and can act like a server for let other peers communicate
   - Super Peer
	 This peer type is a little bit more limited but can still have some server functions
   - Peer
	 This peer type can't communicate without the help of other peers that redirect the messages
	 
   All the peers are written inside a json file called "superPeers.json". This is the file used for store the peers info and when the app is launched is used for connect to the network
   
 ## Exchange Activity
	This activity is used for exchange the IP and start the communication with a new machine.
 ## Text Message NO ACK Activity
	This activity is for send a message to a connected peer
 ## Text Message NO ACK Spread Activity
	This activity is for spread a message throught the network for reach the destination peer
 
 ## Peer
	A peer is a machine that want to connect to the P2P network. A peer when connected need to keep the connection alive so on init it starts a thread that send KEEP messages for let other messages always work.
	
	The peer always have the 2 files superPeers.json (that contains the list of peers) and messagesReceived.json (that contains the messages received for avoid duplicates)