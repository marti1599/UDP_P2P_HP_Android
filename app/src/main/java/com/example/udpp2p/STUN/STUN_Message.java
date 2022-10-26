package com.example.udpp2p.STUN;

import android.util.Pair;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implements STUN message. Defined in RFC 3489.
*/
public class STUN_Message
{

   /**
    * Specifies STUN attribute type.
   */
   private enum AttributeType
   {
      MappedAddress     (0x0001),
      ResponseAddress   (0x0002),
      ChangeRequest     (0x0003),
      SourceAddress     (0x0004),
      ChangedAddress    (0x0005),
      Username          (0x0006),
      Password          (0x0007),
      MessageIntegrity  (0x0008),
      ErrorCode         (0x0009),
      UnknownAttribute  (0x000A),
      ReflectedFrom     (0x000B),
      XorMappedAddress  (0x8020),
      XorOnly           (0x0021),
      ServerName        (0x8022),
      ;

      private static final Map<Integer, AttributeType> BY_INTEGER = new HashMap<>();

      static {
         for (AttributeType e: values()) {
            BY_INTEGER.put(e.getNumVal(), e);
         }
      }

      private Integer numVal;

      AttributeType(int i) {
         this.numVal = i;
      }

      public Integer getNumVal() {
         return numVal;
      }
   }

   /**
    * Specifies IP address family.
    */
   private enum IPFamily
   {
      IPv4  (0x01),
      IPv6  (0x02),
      ;

      private Integer numVal;

      IPFamily(Integer i) {
         this.numVal = i;
      }

      public Integer getNumVal() {
         return numVal;
      }
   }

   private STUN_MessageType            m_Type             = STUN_MessageType.BindingRequest;
   private UUID                        m_pTransactionID   = null;
   private InetSocketAddress           m_pMappedAddress   = null;
   private InetSocketAddress           m_pResponseAddress = null;
   private STUN_t_ChangeRequest        m_pChangeRequest   = null;
   private InetSocketAddress           m_pSourceAddress   = null;
   private InetSocketAddress           m_pChangedAddress  = null;
   private String                      m_UserName         = null;
   private String                      m_Password         = null;
   private STUN_t_ErrorCode            m_pErrorCode       = null;
   private InetSocketAddress           m_pReflectedFrom   = null;
   private String                      m_ServerName       = null;

   /**
    * Default constructor.
   */
   public STUN_Message()
   {
      m_pTransactionID = UUID.randomUUID();
   }

   /**
    * Parses STUN message from raw data packet.
    *
    * @param data Raw STUN message
    */
   public void Parse(byte[] data)
   {
            /** RFC 3489 11.1.
                All STUN messages consist of a 20 byte header:

                0                   1                   2                   3
                0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
               |      STUN Message Type        |         Message Length        |
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
               |
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                        Transaction ID
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                                                               |
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

               The message length is the count, in bytes, of the size of the
               message, not including the 20 byte header.
            */

      if(data.length < 20){
         throw new NullPointerException("Invalid STUN message value !");
      }

      int offset = 0;

      //--- message header --------------------------------------------------

      // STUN Message Type
      int messageType = (data[offset++] << 8 | data[offset++]);
      if(messageType == (int)STUN_MessageType.BindingErrorResponse.getNumVal()){
         m_Type = STUN_MessageType.BindingErrorResponse;
      }
      else if(messageType == (int)STUN_MessageType.BindingRequest.getNumVal()){
         m_Type = STUN_MessageType.BindingRequest;
      }
      else if(messageType == (int)STUN_MessageType.BindingResponse.getNumVal()){
         m_Type = STUN_MessageType.BindingResponse;
      }
      else if(messageType == (int)STUN_MessageType.SharedSecretErrorResponse.getNumVal()){
         m_Type = STUN_MessageType.SharedSecretErrorResponse;
      }
      else if(messageType == (int)STUN_MessageType.SharedSecretRequest.getNumVal()){
         m_Type = STUN_MessageType.SharedSecretRequest;
      }
      else if(messageType == (int)STUN_MessageType.SharedSecretResponse.getNumVal()){
         m_Type = STUN_MessageType.SharedSecretResponse;
      }
      else{
         throw new NullPointerException("Invalid STUN message type value !");
      }

      // Message Length
      int messageLength = (data[offset++] << 8 | data[offset++]);

      // Transaction ID
      byte[] guid = new byte[16];
      System.arraycopy(data,offset,guid,0,16);
      m_pTransactionID = getGuidFromByteArray(guid);
      offset += 16;

      //--- Message attributes ---------------------------------------------
      while((offset - 20) < messageLength){
         offset = ParseAttribute(data, offset);
      }
   }

   public static UUID getGuidFromByteArray(byte[] bytes) {
      ByteBuffer bb = ByteBuffer.wrap(bytes);
      long high = bb.getLong();
      long low = bb.getLong();
      UUID uuid = new UUID(high, low);
      return uuid;
   }

   /**
    * Converts this to raw STUN packet
    *
    * @return Returns raw STUN packet
    */
   public byte[] ToByteData()
   {
            /** RFC 3489 11.1.
                All STUN messages consist of a 20 byte header:

                0                   1                   2                   3
                0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
               |      STUN Message Type        |         Message Length        |
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
               |
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                        Transaction ID
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                                                               |
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

               The message length is the count, in bytes, of the size of the
               message, not including the 20 byte header.

            */

      // We allocate 512 for header, that should be more than enough.
      byte[] msg = new byte[512];

      int offset = 0;

      //--- message header -------------------------------------

      // STUN Message Type (2 bytes)
      msg[offset++] = (byte)((int)this.getType().getNumVal() >> 8);
      msg[offset++] = (byte)((int)this.getType().getNumVal() & 0xFF);

      // Message Length (2 bytes) will be assigned at last.
      msg[offset++] = 0;
      msg[offset++] = 0;

      // Transaction ID (16 bytes)

      System.arraycopy(getTransactionIDBytes(),0,msg,offset,16);
      offset += 16;

      //--- Message attributes ------------------------------------

            /** RFC 3489 11.2.
                After the header are 0 or more attributes.  Each attribute is TLV
                encoded, with a 16 bit type, 16 bit length, and variable value:

                0                   1                   2                   3
                0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
               |         Type                  |            Length             |
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
               |                             Value                             ....
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
            */

      if(this.getMappedAddress() != null){
         offset = StoreEndPoint(AttributeType.MappedAddress,this.getMappedAddress(),msg, offset);
      }
      else if(this.getResponseAddress() != null){
         offset = StoreEndPoint(AttributeType.ResponseAddress,this.getResponseAddress(),msg, offset);
      }
      else if(this.getChangeRequest() != null){
                /**
                    The CHANGE-REQUEST attribute is used by the client to request that
                    the server use a different address and/or port when sending the
                    response.  The attribute is 32 bits long, although only two bits (A
                    and B) are used:

                     0                   1                   2                   3
                     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                    |0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 A B 0|
                    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                    The meaning of the flags is:

                    A: This is the "change IP" flag.  If true, it requests the server
                       to send the Binding Response with a different IP address than the
                       one the Binding Request was received on.

                    B: This is the "change port" flag.  If true, it requests the
                       server to send the Binding Response with a different port than the
                       one the Binding Request was received on.
                */

         // Attribute header
         msg[offset++] = (byte)(AttributeType.ChangeRequest.getNumVal() >> 8);
         msg[offset++] = (byte)(AttributeType.ChangeRequest.getNumVal() & 0xFF);
         msg[offset++] = 0;
         msg[offset++] = 4;

         msg[offset++] = 0;
         msg[offset++] = 0;
         msg[offset++] = 0;
         msg[offset++] = (byte)(((this.getChangeRequest().getChangeIP() ? 1 : 0) << 2) | ((this.getChangeRequest().getChangePort() ? 1 : 0) << 1));
      }
      else if(this.getSourceAddress() != null){
         offset = StoreEndPoint(AttributeType.SourceAddress,this.getSourceAddress(),msg, offset);
      }
      else if(this.getChangedAddress() != null){
         offset = StoreEndPoint(AttributeType.ChangedAddress,this.getChangedAddress(),msg, offset);
      }
      else if(this.getUserName() != null){
         byte[] userBytes = this.getUserName().getBytes(StandardCharsets.US_ASCII);

         // Attribute header
         msg[offset++] = (byte)(AttributeType.Username.getNumVal() >> 8);
         msg[offset++] = (byte)(AttributeType.Username.getNumVal() & 0xFF);
         msg[offset++] = (byte)(userBytes.length >> 8);
         msg[offset++] = (byte)(userBytes.length & 0xFF);

         System.arraycopy(userBytes,0,msg,offset,userBytes.length);
         offset += userBytes.length;
      }
      else if(this.getPassword() != null){
         byte[] userBytes = this.getUserName().getBytes(StandardCharsets.US_ASCII);

         // Attribute header
         msg[offset++] = (byte)(AttributeType.Password.getNumVal() >> 8);
         msg[offset++] = (byte)(AttributeType.Password.getNumVal() & 0xFF);
         msg[offset++] = (byte)(userBytes.length >> 8);
         msg[offset++] = (byte)(userBytes.length & 0xFF);

         System.arraycopy(userBytes,0,msg,offset,userBytes.length);
         offset += userBytes.length;
      }
      else if(this.getErrorCode() != null){
                /** 3489 11.2.9.
                    0                   1                   2                   3
                    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                    |                   0                     |Class|     Number    |
                    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                    |      Reason Phrase (variable)                                ..
                    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                */

         byte[] reasonBytes = this.getErrorCode().getReasonText().getBytes(StandardCharsets.US_ASCII);

         // Header
         msg[offset++] = 0;
         msg[offset++] = AttributeType.ErrorCode.getNumVal().byteValue();
         msg[offset++] = 0;
         msg[offset++] = (byte)(4 + reasonBytes.length);

         // Empty
         msg[offset++] = 0;
         msg[offset++] = 0;
         // Class
         msg[offset++] = (byte)Math.floor((double)(this.getErrorCode().getCode() / 100));
         // Number
         msg[offset++] = (byte)(this.getErrorCode().getCode() & 0xFF);
         // ReasonPhrase
         System.arraycopy(reasonBytes,0, msg,0,reasonBytes.length);
         offset += reasonBytes.length;
      }
      else if(this.getReflectedFrom() != null){
         offset = StoreEndPoint(AttributeType.ReflectedFrom,this.getReflectedFrom(),msg, offset);
      }

      // Update Message Length. NOTE: 20 bytes header not included.
      msg[2] = (byte)((offset - 20) >> 8);
      msg[3] = (byte)((offset - 20) & 0xFF);

      // Make reatval with actual size.
      byte[] retVal = new byte[offset];
      System.arraycopy(msg,0, retVal, 0, retVal.length);

      return retVal;
   }

   /**
    * Parses attribute from data
    * @param data SIP message data
    * @param offset Offset in data
    * @return
    */
   private int ParseAttribute(byte[] data, int offset) {
            /** RFC 3489 11.2.
                Each attribute is TLV encoded, with a 16 bit type, 16 bit length, and variable value:

                0                   1                   2                   3
                0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
               |         Type                  |            Length             |
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
               |                             Value                             ....
               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
            */

      // Type
      AttributeType type = AttributeType.BY_INTEGER.get((data[offset++] << 8 | data[offset++]));

      // Length
      int length = (data[offset++] << 8 | data[offset++]);

      // MAPPED-ADDRESS
      if (type == AttributeType.MappedAddress) {
         Pair<Integer, InetSocketAddress> pair = ParseEndPoint(data, offset);
         offset = pair.first;
         m_pMappedAddress = pair.second;
      }
      // RESPONSE-ADDRESS
      else if (type == AttributeType.ResponseAddress) {
         Pair<Integer, InetSocketAddress> pair = ParseEndPoint(data, offset);
         offset = pair.first;
         m_pResponseAddress = pair.second;
      }
      // CHANGE-REQUEST
      else if (type == AttributeType.ChangeRequest) {
                /**
                    The CHANGE-REQUEST attribute is used by the client to request that
                    the server use a different address and/or port when sending the
                    response.  The attribute is 32 bits long, although only two bits (A
                    and B) are used:

                     0                   1                   2                   3
                     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                    |0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 A B 0|
                    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                    The meaning of the flags is:

                    A: This is the "change IP" flag.  If true, it requests the server
                       to send the Binding Response with a different IP address than the
                       one the Binding Request was received on.

                    B: This is the "change port" flag.  If true, it requests the
                       server to send the Binding Response with a different port than the
                       one the Binding Request was received on.
                */

         // Skip 3 bytes
         offset += 3;

         m_pChangeRequest = new STUN_t_ChangeRequest((data[offset] & 4) != 0, (data[offset] & 2) != 0);
         offset++;
      }
      // SOURCE-ADDRESS
      else if (type == AttributeType.SourceAddress) {
         Pair<Integer, InetSocketAddress> pair = ParseEndPoint(data, offset);
         offset = pair.first;
         m_pSourceAddress = pair.second;
      }
      // CHANGED-ADDRESS
      else if (type == AttributeType.ChangedAddress) {
         Pair<Integer, InetSocketAddress> pair = ParseEndPoint(data, offset);
         offset = pair.first;
         m_pChangedAddress = pair.second;
      }
      // USERNAME
      else if (type == AttributeType.Username) {
         m_UserName = new String(data, offset, length);
         offset += length;
      }
      // PASSWORD
      else if (type == AttributeType.Password) {
         m_Password = new String(data, offset, length);
         offset += length;
      }
      // MESSAGE-INTEGRITY
      else if (type == AttributeType.MessageIntegrity) {
         offset += length;
      }
      // ERROR-CODE
      else if (type == AttributeType.ErrorCode) {
                /** 3489 11.2.9.
                    0                   1                   2                   3
                    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                    |                   0                     |Class|     Number    |
                    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                    |      Reason Phrase (variable)                                ..
                    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                */

         int errorCode = (data[offset + 2] & 0x7) * 100 + (data[offset + 3] & 0xFF);

         m_pErrorCode = new STUN_t_ErrorCode(errorCode, new String(data, offset + 4, length - 4));
         offset += length;
      }
      // UNKNOWN-ATTRIBUTES
      else if (type == AttributeType.UnknownAttribute) {
         offset += length;
      }
      // REFLECTED-FROM
      else if (type == AttributeType.ReflectedFrom) {
         Pair<Integer, InetSocketAddress> pair = ParseEndPoint(data, offset);
         offset = pair.first;
         m_pReflectedFrom = pair.second;
      }
      // XorMappedAddress
      // XorOnly
      // ServerName
      else if (type == AttributeType.ServerName) {
         m_ServerName = new String(data, offset, length);
         offset += length;
      }
      // Unknown
      else {
         offset += length;
      }

      return offset;
   }

   /**
    * Parses IP endpoint attribute.
    *
    * @param data STUN message data
    * @param offset Offset in data
    * @return Returns parsed IP end point
    */
   private Pair<Integer, InetSocketAddress> ParseEndPoint(byte[] data, int offset)
   {
            /**
                It consists of an eight bit address family, and a sixteen bit
                port, followed by a fixed length value representing the IP address.

                0                   1                   2                   3
                0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                |x x x x x x x x|    Family     |           Port                |
                +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                |                             Address                           |
                +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                24-25 = Family
                26-27 = Port
                28-29-30-31 = IP
            */

      // Skip family
      offset++;
      offset++;

      // Port
      int port = ((Byte.toUnsignedInt(data[offset++]) << 8) | Byte.toUnsignedInt(data[offset++]));

      // Address
      int[] ip = new int[4];
      ip[0] = Byte.toUnsignedInt(data[offset++]);
      ip[1] = Byte.toUnsignedInt(data[offset++]);
      ip[2] = Byte.toUnsignedInt(data[offset++]);
      ip[3] = Byte.toUnsignedInt(data[offset++]);

      String ipString = ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];

      return new Pair<>(offset, new InetSocketAddress(ipString,port));
   }

   /**
    * Stores ip end point attribute to buffer.
    *
    * @param type Attribute type
    * @param endPoint IP end point
    * @param message Buffer where to store
    * @param offset Offset in buffer
    * @return
    */
   private int StoreEndPoint(AttributeType type,InetSocketAddress endPoint,byte[] message, int offset)
   {
            /**
                It consists of an eight bit address family, and a sixteen bit
                port, followed by a fixed length value representing the IP address.

                0                   1                   2                   3
                0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                |x x x x x x x x|    Family     |           Port                |
                +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                |                             Address                           |
                +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
            */

      // Header
      message[offset++] = (byte)((int)type.getNumVal() >> 8);
      message[offset++] = (byte)((int)type.getNumVal() & 0xFF);
      message[offset++] = 0;
      message[offset++] = 8;

      // Unused
      message[offset++] = 0;
      // Family
      message[offset++] = IPFamily.IPv4.getNumVal().byteValue();
      // Port
      message[offset++] = (byte)(endPoint.getPort() >> 8);
      message[offset++] = (byte)(endPoint.getPort() & 0xFF);
      // Address
      byte[] ipBytes = endPoint.getAddress().getAddress();
      message[offset++] = ipBytes[0];
      message[offset++] = ipBytes[0];
      message[offset++] = ipBytes[0];
      message[offset++] = ipBytes[0];

      return offset;
   }

   /**
    * @return Gets STUN message type.
    */
   public STUN_MessageType getType()
   {
      return m_Type;
   }

   /**
    * @param value Set STUN MessageType
    */
   public void setType(STUN_MessageType value)
   {
      m_Type = value;
   }

   /**
    * @return Gets transaction ID
    */
   public UUID getTransactionID()
   {
      return m_pTransactionID;
   }

   /**
    * @return Get byte[] that contains the transaction ID
    */
   public byte[] getTransactionIDBytes()
   {
      ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
      bb.putLong(m_pTransactionID.getMostSignificantBits());
      bb.putLong(m_pTransactionID.getLeastSignificantBits());
      return bb.array();
   }

   /**
    * @return Gets or sets IP end point what was actually connected to STUN server. Returns null if not specified
    */
   public InetSocketAddress getMappedAddress()
   {
      return m_pMappedAddress;
   }

   public void setMappedAddress(InetSocketAddress value){
      m_pMappedAddress = value;
   }

   /**
    * @return Gets or sets IP end point where to STUN client likes to receive response. Value null means not specified
    */
   public InetSocketAddress getResponseAddress()
   {
      return m_pResponseAddress;
   }

   public void setResponseAddress(InetSocketAddress value)
   {
      m_pResponseAddress = value;
   }

   /**
    * @return Gets or sets how and where STUN server must send response back to STUN client. Value null means not specified
    */
   public STUN_t_ChangeRequest getChangeRequest()
   {
      return m_pChangeRequest;
   }

   public void setChangeRequest(STUN_t_ChangeRequest value)
   {
      m_pChangeRequest = value;
   }

   /**
    * @return Gets or sets STUN server IP end point what sent response to STUN client. Value null means not specified
    */
   public InetSocketAddress getSourceAddress()
   {
      return m_pSourceAddress;
   }

   public void setSourceAddress(InetSocketAddress value)
   {
      m_pSourceAddress = value;
   }

   /**
    * @return Gets or sets IP end point where STUN server will send response back to STUN client if the "change IP" and "change port" flags had been set in the ChangeRequest
    */
   public InetSocketAddress getChangedAddress()
   {
      return m_pChangedAddress;
   }

   public void getChangedAddress(InetSocketAddress value)
   {
      m_pChangedAddress = value;
   }

   /**
    * @return Gets or sets user name. Value null means not specified
    */
   public String getUserName()
   {
      return m_UserName;
   }

   public void setUserName(String value)
   {
      m_UserName = value;
   }

   /**
    * @return Gets or sets password. Value null means not specified
    */
   public String getPassword()
   {
      return m_Password;
   }

   public void setPassword(String value)
   {
      m_Password = value;
   }

   /**
    * @return Gets or sets error info. Returns null if not specified
    */
   public STUN_t_ErrorCode getErrorCode()
   {
      return m_pErrorCode;
   }

   public void setErrorCode(STUN_t_ErrorCode value)
   {
      m_pErrorCode = value;
   }

   /**
    * @return Gets or sets IP endpoint from which IP end point STUN server got STUN client request. Value null means not specified
    */
   public InetSocketAddress getReflectedFrom()
   {
      return m_pReflectedFrom;
   }

   public void setReflectedFrom(InetSocketAddress value)
   {
      m_pReflectedFrom = value;
   }

   /**
    * @return Gets or sets server name
    */
   public String getServerName()
   {
      return m_ServerName;
   }

   public void setServerName(String value)
   {
      m_ServerName = value;
   }
}
