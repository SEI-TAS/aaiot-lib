package edu.cmu.sei.ttg.aaiot.tokens;

import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.network.CoapsPskClient;
import se.sics.ace.AceException;
import se.sics.ace.Constants;
import se.sics.ace.rs.TokenRepository;

import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by sebastianecheverria on 9/11/17.
 */
public class RevokedTokenChecker implements Runnable
{
    private static final int TIME_BETWEEN_CHECKS = 10 * 1000;

    private Thread checkerThread;
    private boolean stopChecking = false;

    private String asServerName;
    private int asServerPort;
    private String myId;
    private byte[] myPSK;

    private TokenRepository tokenRepository;
    private FileTokenStorage tokenStorage;

    public RevokedTokenChecker(String asServerName, int asServerPort, String myId, byte[] myKey, TokenRepository tokens)
    {
        this.asServerName = asServerName;
        this.asServerPort = asServerPort;
        this.myId = myId;
        this.myPSK = myKey;
        this.tokenRepository = tokens;
        this.tokenStorage = null;
    }

    public RevokedTokenChecker(String asServerName, int asServerPort, String myId, byte[] myKey, FileTokenStorage tokens)
    {
        this.asServerName = asServerName;
        this.asServerPort = asServerPort;
        this.myId = myId;
        this.myPSK = myKey;
        this.tokenStorage = tokens;
        this.tokenRepository = null;
    }

    public void startChecking()
    {
        checkerThread = new Thread(this, "checkerThread");
        checkerThread.start();
    }

    public void stopChecking()
    {
        stopChecking = true;
        checkerThread.interrupt();
    }

    public void run()
    {
        while(!stopChecking)
        {
            try
            {
                try
                {
                    Thread.sleep(TIME_BETWEEN_CHECKS);
                }
                catch (InterruptedException ex)
                {
                    break;
                }

                CoapsPskClient client = new CoapsPskClient(asServerName, asServerPort, myId, myPSK);

                Set<CBORObject> tokenList = new HashSet<>();
                if(tokenStorage != null)
                {
                    Map<String, ResourceServer> tokens = tokenStorage.getTokens();
                    for(String rsId : tokens.keySet())
                    {
                        ResourceServer rs = tokens.get(rsId);
                        tokenList.add(rs.token);
                    }
                }
                else if(tokenRepository != null)
                {
                    Set<String> tokenCtis = tokenRepository.getCtis();
                    for(String cti : tokenCtis)
                    {
                        tokenList.add(CBORObject.FromObject(Base64.getDecoder().decode(cti)));
                    }
                }

                for(CBORObject token : tokenList)
                {
                    boolean isActive = false;
                    try
                    {
                        isActive = isTokenActive(client, token);
                    }
                    catch(Exception ex)
                    {
                        // Issue sending this request, AS may be out of reach. Stop checking for now.
                        break;
                    }

                    if(!isActive)
                    {
                        System.out.println("WARNING: Revoked or expired token found, removing from local repo.");
                        if(tokenStorage != null)
                        {
                            tokenStorage.removeToken(token);
                        }
                        else if(tokenRepository != null)
                        {
                            tokenRepository.removeToken(Base64.getEncoder().encodeToString(token.GetByteString()));
                        }
                    }
                }

                client.stop();
            }
            catch(Exception ex)
            {
                System.out.println("Error checking token status: " + ex.toString());
            }
        }
    }

    // Sends an introspection request only to check if the token is still marked as valid or not. If invalid, this could
    // be from a revoked or an expired token.
    private boolean isTokenActive(CoapsPskClient client, CBORObject token) throws AceException
    {
        CBORObject params = CBORObject.NewMap();
        params.Add(Constants.TOKEN, token);
        CBORObject reply = client.sendRequest("introspect", "post", params);

        Map<String, CBORObject> mapReply = Constants.unabbreviate(reply);
        return mapReply.get("active").AsBoolean();
    }
}
