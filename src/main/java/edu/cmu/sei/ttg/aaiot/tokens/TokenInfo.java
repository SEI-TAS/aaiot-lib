/*
AAIoT Source Code

Copyright 2018 Carnegie Mellon University. All Rights Reserved.

NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS"
BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER
INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM
USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM
PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.

Released under a MIT (SEI)-style license, please see license.txt or contact permission@sei.cmu.edu for full terms.

[DISTRIBUTION STATEMENT A] This material has been approved for public release and unlimited distribution.  Please see
Copyright notice for non-US Government use and distribution.

This Software includes and/or makes use of the following Third-Party Software subject to its own license:

1. ace-java (https://bitbucket.org/lseitz/ace-java/src/9b4c5c6dfa5ed8a3456b32a65a3affe08de9286b/LICENSE.md?at=master&fileviewer=file-view-default)
Copyright 2016-2018 RISE SICS AB.
2. zxing (https://github.com/zxing/zxing/blob/master/LICENSE) Copyright 2018 zxing.
3. sarxos webcam-capture (https://github.com/sarxos/webcam-capture/blob/master/LICENSE.txt) Copyright 2017 Bartosz Firyn.
4. 6lbr (https://github.com/cetic/6lbr/blob/develop/LICENSE) Copyright 2017 CETIC.

DM18-0702
*/

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
        System.out.println("Token : " + token);

        CBORObject tokenCbor = CBORObject.DecodeFromBytes(token.GetByteString());
        System.out.println("Token COSE: " + tokenCbor + ", type: " + tokenCbor.getType());

        CBORObject protectedHeaders = CBORObject.DecodeFromBytes(tokenCbor.get(0).GetByteString());
        System.out.println("Token Prot Headers: " + protectedHeaders);

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
