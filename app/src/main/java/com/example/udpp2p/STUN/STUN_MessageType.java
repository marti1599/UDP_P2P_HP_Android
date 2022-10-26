package com.example.udpp2p.STUN;

/**
 * This enum specifies STUN message type.
 */
public enum STUN_MessageType {
    /**
     * STUN message is binding request.
     */
    BindingRequest(0x0001),

    /**
     * STUN message is binding request response.
     */
    BindingResponse(0x0101),

    /**
     * STUN message is binding requesr error response.
     */
    BindingErrorResponse(0x0111),

    /**
     * STUN message is "shared secret" request.
     */
    SharedSecretRequest(0x0002),

    /**
     * STUN message is "shared secret" request response.
     */
    SharedSecretResponse(0x0102),

    /**
     * STUN message is "shared secret" request error response.
     */
    SharedSecretErrorResponse(0x0112),
    ;

    private int numVal;

    STUN_MessageType(int i) {
        this.numVal = i;
    }

    public int getNumVal() {
        return numVal;
    }
}
