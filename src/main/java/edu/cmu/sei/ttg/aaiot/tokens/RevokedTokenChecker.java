package edu.cmu.sei.ttg.aaiot.tokens;

import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.network.CoapsPskClient;
import se.sics.ace.AceException;
import se.sics.ace.Constants;
import se.sics.ace.rs.TokenRepository;

import java.util.*;

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
    private IRemovedTokenTracker removedTokenTracker;

    /**
     * Constructor when using a TokenRepository to store tokens.
     */
    public RevokedTokenChecker(String asServerName, int asServerPort, String myId, byte[] myKey, IRemovedTokenTracker removedTokenTracker, TokenRepository tokens)
    {
        this.asServerName = asServerName;
        this.asServerPort = asServerPort;
        this.myId = myId;
        this.myPSK = myKey;
        this.removedTokenTracker = removedTokenTracker;
        this.tokenRepository = tokens;
        this.tokenStorage = null;
    }

    /**
     * Constructor when using a FileTokenStorage to store tokens.
     */
    public RevokedTokenChecker(String asServerName, int asServerPort, String myId, byte[] myKey, IRemovedTokenTracker removedTokenTracker, FileTokenStorage tokens)
    {
        this.asServerName = asServerName;
        this.asServerPort = asServerPort;
        this.myId = myId;
        this.myPSK = myKey;
        this.removedTokenTracker = removedTokenTracker;
        this.tokenStorage = tokens;
        this.tokenRepository = null;
    }

    /**
     * Starts the checking thread.
     */
    public void startChecking()
    {
        checkerThread = new Thread(this, "checkerThread");
        checkerThread.start();
    }

    /**
     * Stops the checking thread.
     */
    public void stopChecking()
    {
        stopChecking = true;
        checkerThread.interrupt();
    }

    /**
     * Actual execution of the checker thread.
     */
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

                System.out.println("Checking for revoked tokens.");
                CoapsPskClient client = new CoapsPskClient(asServerName, asServerPort, myId, myPSK);

                if(tokenStorage != null)
                {
                    Map<String, TokenInfo> tokens = tokenStorage.getTokens();
                    for(String rsId : tokens.keySet())
                    {
                        TokenInfo rs = tokens.get(rsId);
                        boolean checkSuccessful = checkAndPurgeToken(client, rs.token, rsId);
                        if(!checkSuccessful)
                        {
                            // Connection issue, break to avoid retrying if there is no good connection.
                            break;
                        }
                    }
                }
                else if(tokenRepository != null)
                {
                    Set<String> tokenCtis = tokenRepository.getCtis();
                    for(String cti : tokenCtis)
                    {
                        CBORObject cborCti = CBORObject.FromObject(Base64.getDecoder().decode(cti));
                        boolean checkSuccessful = checkAndPurgeToken(client, cborCti, null);
                        if(!checkSuccessful)
                        {
                            // Connection issue, break to avoid retrying if there is no good connection.
                            break;
                        }
                    }
                }

                client.stop();
                System.out.println("Finished checking for revoked tokens.");
            }
            catch(Exception ex)
            {
                System.out.println("Error checking token status: " + ex.toString());
            }
        }
    }

    /**
     * Checks if a given token is still marked as valid by the AS, and purges it if it is not.
     * @param client
     * @param token
     * @param rsId
     * @return
     * @throws AceException
     */
    private boolean checkAndPurgeToken(CoapsPskClient client, CBORObject token, String rsId) throws AceException
    {
        boolean isActive = false;
        try
        {
            isActive = isTokenActive(client, token, rsId);
        }
        catch(Exception ex)
        {
            // Issue sending this request, AS may be out of reach.
            return false;
        }

        if(!isActive)
        {
            // If the token is marked as inactive, we want to remove it from our list.
            String tokenId = null;
            System.out.println("WARNING: Revoked or expired token found, removing from local repo.");
            if(tokenStorage != null)
            {
                tokenId = tokenStorage.getTokenInfo(token).getTokenId();
                tokenStorage.removeToken(token);
                tokenStorage.storeToFile();
            }
            else if(tokenRepository != null)
            {
                String cti = Base64.getEncoder().encodeToString(token.GetByteString());
                tokenRepository.removeToken(cti);
                tokenId = cti;
            }

            // We also want to notify to a listener that we have revoked a token.
            removedTokenTracker.notifyRemovedToken(tokenId, rsId);
        }

        return true;
    }

    /**
     * Sends an introspection request only to check if the token is still marked as valid or not. If invalid, this could
     * be from a revoked or from an expired token.
     */
    private boolean isTokenActive(CoapsPskClient client, CBORObject token, String rsId) throws AceException
    {
        CBORObject params = CBORObject.NewMap();
        params.Add(Constants.TOKEN, token);

        if(rsId != null)
        {
            // TODO: formalize this to use experimental RFC to indicate who the token was issued to.
            params.Add((short) 40, CBORObject.FromObject(rsId));
        }

        CBORObject reply = client.sendRequest("introspect", "post", params);

        Map<String, CBORObject> mapReply = Constants.unabbreviate(reply);
        return mapReply.get("active").AsBoolean();
    }
}
