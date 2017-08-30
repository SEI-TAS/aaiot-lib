package edu.cmu.sei.ttg.aaiot.tokens;

import COSE.KeyKeys;
import com.upokecenter.cbor.CBORObject;
import org.json.JSONObject;
import se.sics.ace.Constants;

import java.util.Base64;
import java.util.Map;

/**
 * Created by sebastianecheverria on 8/30/17.
 */
public class ResourceServer
{
    public String rsId;
    public boolean isTokenSent = false;
    public CBORObject token = null;
    public String popKeyId = null;
    public CBORObject popKey = null;

    public ResourceServer(String rsId, Map<String, CBORObject> data)
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
        popKeyId = new String(kidCbor.GetByteString(), Constants.charset);
        System.out.println("Cnf (pop) key id: " + popKeyId);
    }

    public ResourceServer(JSONObject data)
    {
        rsId = data.getString("rsId");
        token = CBORObject.DecodeFromBytes(Base64.getDecoder().decode(data.getString("token")));
        popKey = CBORObject.DecodeFromBytes(Base64.getDecoder().decode(data.getString("key")));
        popKeyId = data.getString("popKeyId");
        isTokenSent = data.getBoolean("isTokenSent");
    }

    public JSONObject toJSON()
    {
        JSONObject jsonData = new JSONObject();
        jsonData.put("rsId", rsId);
        jsonData.put("token", Base64.getEncoder().encodeToString(token.EncodeToBytes()));
        jsonData.put("key", Base64.getEncoder().encodeToString(popKey.EncodeToBytes()));
        jsonData.put("popKeyId", popKeyId);
        jsonData.put("isTokenSent", isTokenSent);
        return jsonData;
    }
}
