package edu.cmu.sei.ttg.aaiot.credentials;

import COSE.CoseException;
import COSE.KeyKeys;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;

/**
 * Created by sebastianecheverria on 8/11/17.
 */
public class FileCredentialStore implements ICredentialStore
{
    private static final String ID_KEY = "AS_ID";
    private static final String PSK_KEY = "AS_PSK";
    private static final String DEFAULT_FILE_PATH = "credentials.json";

    private String asId = null;
    private OneKey asPSK = null;
    private byte[] rawAsPSK = null;

    private String filePath;

    public FileCredentialStore() throws IOException, CoseException
    {
        this.filePath = DEFAULT_FILE_PATH;
        loadFromFile();
    }

    public FileCredentialStore(String filePath) throws IOException, CoseException
    {
        this.filePath = filePath;
        loadFromFile();
    }

    private void loadFromFile() throws CoseException, IOException
    {
        FileInputStream fs;
        try {
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

        this.asId = json.getString(ID_KEY);
        this.rawAsPSK = Base64.getDecoder().decode(json.getString(PSK_KEY));
        this.asPSK = createOneKeyFromBytes(this.rawAsPSK);
        System.out.println("Credentials loaded from file.");
    }

    @Override
    public boolean storeAS(String asId, byte[] psk)
    {
        try
        {
            this.asId = asId;
            this.rawAsPSK = psk;
            this.asPSK = createOneKeyFromBytes(psk);

            JSONObject json = new JSONObject();
            json.put(ID_KEY, asId);
            json.put(PSK_KEY, Base64.getEncoder().encodeToString(psk));

            FileWriter file = new FileWriter(filePath, false);
            file.write(json.toString());
            file.flush();
            file.close();

            return true;
        }
        catch(Exception ex)
        {
            System.out.println("Error storing AS key: " + ex.toString());
            return false;
        }
    }

    // Creates a OneKey from raw key data.
    public OneKey createOneKeyFromBytes(byte[] rawKey) throws COSE.CoseException
    {
        CBORObject keyData = CBORObject.NewMap();
        keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
        keyData.Add(KeyKeys.Octet_K.AsCBOR(), CBORObject.FromObject(rawKey));
        return new OneKey(keyData);
    }

    @Override
    public String getASid()
    {
        return asId;
    }

    @Override
    public OneKey getASPSK()
    {
        return asPSK;
    }

    @Override
    public byte[] getRawASPSK()
    {
        return rawAsPSK;
    }
}
