package com.example.udpp2p.STUN;

/**
 * This class implements STUN CHANGE-REQUEST attribute. Defined in RFC 3489 11.2.4.
 */
public class STUN_t_ChangeRequest {
    private boolean m_ChangeIP = true;
    private boolean m_ChangePort = true;

    /**
     * Default constructor
     */
    public STUN_t_ChangeRequest() {
    }

    /**
     * Default constructor
     *
     * @param changeIP   Specifies if STUN server must send response to different IP than request was received
     * @param changePort Specifies if STUN server must send response to different port than request was received
     */
    public STUN_t_ChangeRequest(boolean changeIP, boolean changePort) {
        m_ChangeIP = changeIP;
        m_ChangePort = changePort;
    }

    /**
     * @return Gets or sets if STUN server must send response to different IP than request was received
     */
    public boolean getChangeIP() {
        return m_ChangeIP;
    }

    public void setChangeIP(boolean value) {
        m_ChangeIP = value;
    }

    /**
     * @return Gets or sets if STUN server must send response to different port than request was received
     */
    public boolean getChangePort() {
        return m_ChangePort;
    }

    public void setChangePort(boolean value) {
        m_ChangePort = value;
    }
}
