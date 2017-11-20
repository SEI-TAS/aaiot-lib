package edu.cmu.sei.ttg.aaiot.tokens;

import COSE.KeyKeys;
import com.upokecenter.cbor.CBORObject;
import org.json.JSONObject;
import se.sics.ace.Constants;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;

/**
 * Created by sebastianecheverria on 8/30/17.
 */
public class TokenInfo
{
    public String rsId;
    public boolean isTokenSent = false;
    public CBORObject token = null;
    public byte[] popKeyId = null;
    public CBORObject popKey = null;
    private String popKeyIdAsString = "";

    public TokenInfo(String rsId, Map<String, CBORObject> data)
    {
        this.rsId = rsId;
        isTokenSent = false;

        token = data.get("access_token");
        System.out.println("Token :" + token);

        CBORObject popKeyCbor = data.get("cnf");
        System.out.println("Cnf (pop) key: " + popKeyCbor);

        popKey = popKeyCbor.get(Constants.COSE_KEY_CBOR);
        System.out.println("Cnf (pop) key data: " + popKey);

        CBORObject kidCbor = popKey.get(KeyKeys.KeyId.AsCBOR());
        popKeyId = kidCbor.GetByteString();
        System.out.println("Cnf (pop) key id: " + popKeyId);

        popKeyIdAsString = Base64.getEncoder().encodeToString(kidCbor.GetByteString());
        System.out.println("Token id in Base64 (cti): " + popKeyIdAsString);
    }

    public TokenInfo(JSONObject data)
    {
        rsId = data.getString("rsId");
        token = CBORObject.DecodeFromBytes(Base64.getDecoder().decode(data.getString("token")));
        popKey = CBORObject.DecodeFromBytes(Base64.getDecoder().decode(data.getString("key")));
        popKeyId = Base64.getDecoder().decode(data.getString("popKeyId"));
        isTokenSent = data.getBoolean("isTokenSent");
        popKeyIdAsString = data.getString("popKeyIdAsString");
    }

    public JSONObject toJSON()
    {
        JSONObject jsonData = new JSONObject();
        jsonData.put("rsId", rsId);
        jsonData.put("token", Base64.getEncoder().encodeToString(token.EncodeToBytes()));
        jsonData.put("key", Base64.getEncoder().encodeToString(popKey.EncodeToBytes()));
        jsonData.put("popKeyId", Base64.getEncoder().encodeToString(popKeyId));
        jsonData.put("isTokenSent", isTokenSent);
        jsonData.put("popKeyIdAsString", popKeyIdAsString);
        return jsonData;
    }

    public long bytesToLong(byte[] bytes)
    {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip(); //need flip
        return buffer.getLong();
    }

    public String getRsId()
    {
        return rsId;
    }

    public String getTokenAsString()
    {
        return token.toString();
    }

    /**
     * Returns the pop key ID as a token id. This matches the CTI id used in the AS, just because in the AS implementation
     * the CTI is also used as the pop key ID.
     * @return
     */
    public String getTokenId()
    {
        return popKeyIdAsString;
    }
}
