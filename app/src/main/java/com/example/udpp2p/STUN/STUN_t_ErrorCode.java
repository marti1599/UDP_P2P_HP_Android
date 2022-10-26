package com.example.udpp2p.STUN;

/**
 * This class implements STUN ERROR-CODE. Defined in RFC 3489 11.2.9.
*/
public class STUN_t_ErrorCode {
    private int m_Code = 0;
    private String m_ReasonText = "";

    /**
     * Default constructor
     *
     * @param code Error code
     * @param reasonText Reason text
     */
    public STUN_t_ErrorCode(int code, String reasonText) {
        m_Code = code;
        m_ReasonText = reasonText;
    }

    /**
     * @return Gets or sets error code
     */
    public int getCode()
    {
        return m_Code;
    }

    public void setCode(int value)
    {
        m_Code = value;
    }

    /**
     * @return Gets reason text
     */
    public String getReasonText() {
        return m_ReasonText;
    }

    public void gsetReasonText(String value) {
        m_ReasonText = value;
    }
}
