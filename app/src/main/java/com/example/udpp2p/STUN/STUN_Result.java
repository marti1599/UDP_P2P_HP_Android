package com.example.udpp2p.STUN;

import java.net.InetSocketAddress;

/**
 * This class holds STUN_Client.Query method return data.
 */
public class STUN_Result {
    private STUN_NetType m_NetType = STUN_NetType.OpenInternet;
    private InetSocketAddress m_pPublicEndPoint = null;

    /**
     * Default constructor
     *
     * @param netType Specifies UDP network type
     * @param publicEndPoint Public IP end point
     */
    public STUN_Result(STUN_NetType netType, InetSocketAddress publicEndPoint) {
        m_NetType = netType;
        m_pPublicEndPoint = publicEndPoint;
    }

    /**
     * @return Gets UDP network type
     */
    public STUN_NetType getNetType()
    {
        return m_NetType;
    }

    /**
     * @return Gets public IP end point. This value is null if failed to get network type
     */
    public InetSocketAddress getPublicEndPoint()
    {
        return m_pPublicEndPoint;
    }
}
