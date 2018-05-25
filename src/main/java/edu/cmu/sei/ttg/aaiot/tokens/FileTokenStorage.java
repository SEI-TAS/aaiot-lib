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

import com.upokecenter.cbor.CBORObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sebastianecheverria on 8/30/17.
 */
public class FileTokenStorage
{
    private static final String DEFAULT_FILE_PATH = "tokens.json";

    private Map<String, TokenInfo> tokens = new HashMap<>();
    private String filePath;

    public FileTokenStorage() throws IOException
    {
        this.filePath = DEFAULT_FILE_PATH;
        loadFromFile();
    }

    public FileTokenStorage(String filePath) throws IOException
    {
        this.filePath = filePath;
        loadFromFile();
    }

    private void loadFromFile() throws IOException
    {
        FileInputStream fs;
        try
        {
            fs = new FileInputStream(filePath);
        }
        catch(IOException ex)
        {
            System.out.println("File Store file " + filePath + " not found, will be created.");
            return;
        }

        // Load the whole data contents and parse them as JSON.
        int fileLength = (int) (new File(filePath)).length();
        byte[] data = new byte[fileLength];
        fs.read(data);
        fs.close();
        String jsonString = new String(data);
        JSONObject json = new JSONObject(jsonString);

        // Load all tokens/rs from json data.
        tokens.clear();
        JSONArray servers = json.getJSONArray("servers");
        for(Object server : servers)
        {
            JSONObject serverData = (JSONObject) server;
            TokenInfo resourceServer = new TokenInfo(serverData);
            tokens.put(resourceServer.rsId, resourceServer);
        }
    }

    public boolean storeToFile()
    {
        try
        {
            JSONObject servers = new JSONObject();
            JSONArray serverList = new JSONArray();
            servers.put("servers", serverList);
            for(String rsId : tokens.keySet())
            {
                TokenInfo resourceServer = tokens.get(rsId);
                serverList.put(resourceServer.toJSON());
            }

            FileWriter file = new FileWriter(filePath, false);
            file.write(servers.toString());
            file.flush();
            file.close();

            return true;
        }
        catch(Exception ex)
        {
            System.out.println("Error storing tokens: " + ex.toString());
            return false;
        }
    }

    public Map<String, TokenInfo> getTokens()
    {
        return tokens;
    }

    /**
     * Returns a TokenInfo object based on the given token in CBOR format.
     */
    public TokenInfo getTokenInfo(CBORObject token)
    {
        for(String rsId : tokens.keySet())
        {
            TokenInfo tokenInfo = tokens.get(rsId);
            if(tokenInfo.token.equals(token))
            {
                return tokenInfo;
            }
        }

        return null;
    }

    /**
     * Deletes a TokenInfo entry given the token in CBOR format.
     * @param token
     */
    public void removeToken(CBORObject token)
    {
        for(String rsId : tokens.keySet())
        {
            TokenInfo rs = tokens.get(rsId);
            if(rs.token.equals(token))
            {
                tokens.remove(rsId);
                return;
            }
        }

        throw new IllegalArgumentException("Token to remove not found: " + token.toString());
    }
}
