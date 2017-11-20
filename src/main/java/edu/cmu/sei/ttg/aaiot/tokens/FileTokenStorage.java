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
