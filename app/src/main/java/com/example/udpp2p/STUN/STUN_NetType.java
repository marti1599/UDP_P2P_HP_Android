package com.example.udpp2p.STUN;

/**
 * Specifies UDP network type.
 */
public enum STUN_NetType {
    /**
     * UDP is always blocked.
     */
    UdpBlocked,

    /**
     * No NAT, public IP, no firewall.
     */
    OpenInternet,

    /**
     * No NAT, public IP, but symmetric UDP firewall.
     */
    SymmetricUdpFirewall,

    /**
     * A restricted cone NAT is one where all requests from the same internal IP address and
     * port are mapped to the same external IP address and port. Unlike a full cone NAT, an external
     * host (with IP address X) can send a packet to the internal host only if the internal host
     * had previously sent a packet to IP address X.
     */
    RestrictedCone,

    /**
     * A port restricted cone NAT is like a restricted cone NAT, but the restriction
     * includes port numbers. Specifically, an external host can send a packet, with source IP
     * address X and source port P, to the internal host only if the internal host had previously
     * sent a packet to IP address X and port P.
     */
    PortRestrictedCone,

    /**
     * A symmetric NAT is one where all requests from the same internal IP address and port,
     * to a specific destination IP address and port, are mapped to the same external IP address and
     * port.  If the same host sends a packet with the same source address and port, but to
     * a different destination, a different mapping is used. Furthermore, only the external host that
     * receives a packet can send a UDP packet back to the internal host.
     */
    Symmetric,

    /**
     * A full cone NAT is one where all requests from the same internal IP address and port are
     * mapped to the same external IP address and port. Furthermore, any external host can send
     * a packet to the internal host, by sending a packet to the mapped external address.
     */

    //Generic
    FullCone,

    /**
     * Translate only from Local IP to Public IP
     * Communicate: For Communicate the remote Client need the Public IP
     * Example:
     * LocalEP = 192.168.0.20:12356
     * PublicEP = 1.1.1.1:12356
     */
    FullCone_TRANSLATEIP,

    /**
     * Translate IP and PORT to a Public IP and a free Port but the port will always be the same even if i change the destination
     * This situation could happen too if the port is already used
     * Communicate: For Communicate the remote Client need the Public IP and the Public PORT
     * Example:
     * STUN1
     * LocalEP = 192.168.0.20:12356
     * PublicEP = 1.1.1.1:22222
     * <p>
     * STUN1_2 (STUN 1, 2 request)
     * LocalEP = 192.168.0.20:12356
     * PublicEP = 1.1.1.1:22222
     * <p>
     * STUN2
     * LocalEP = 192.168.0.20:12356
     * PublicEP = 1.1.1.1:22222
     */
    FullCone_TRANSLATEIP_TRANSLATEPORT,

    /**
     * Translate IP and PORT to a Public IP and a free Port but the port will depend according to the destination
     * Communicate: For Communicate with this type of NAT i need a Third Client that is fully Open (OPEN INTERNET) for get the Port for communicate, sometimes this don't work so it is a Client
     * Example:
     * STUN1
     * LocalEP = 192.168.0.20:12356
     * PublicEP = 1.1.1.1:22222
     * <p>
     * STUN1_2 (STUN 1, 2 request)
     * LocalEP = 192.168.0.20:12356
     * PublicEP = 1.1.1.1:22222
     * <p>
     * STUN2
     * LocalEP = 192.168.0.20:12356
     * PublicEP = 1.1.1.1:22223
     */
    FullCone_TRANSLATEIP_TRANSLATEPORTDESTINATION,

    /**
     * Translate IP and PORT to Public IP and a random free Port
     * Communicate: For Communicate with this type of NAT i need a Third Client that is fully Open (OPEN INTERNET) for get the Port for communicate, sometimes this don't work so it is a Client
     * Example:
     * STUN1
     * LocalEP = 192.168.0.20:12356
     * PublicEP = 1.1.1.1:22222
     * <p>
     * STUN1_2 (STUN 1, 2 request)
     * LocalEP = 192.168.0.20:12356
     * PublicEP = 1.1.1.1:22223
     * <p>
     * STUN2
     * LocalEP = 192.168.0.20:12356
     * PublicEP = 1.1.1.1:22224
     */
    FullCone_TRANSLATEIP_TRANSLATEPORTFREE,
}
