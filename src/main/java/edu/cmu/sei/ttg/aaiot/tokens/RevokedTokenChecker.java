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
import edu.cmu.sei.ttg.aaiot.network.CoapException;
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
                        // Encode as BS as the standard requests it, even if the cti is already a BS.
                        CBORObject cborCti = CBORObject.FromObject(Base64.getDecoder().decode(cti));
                        CBORObject cborCtiAsBS = CBORObject.FromObject(cborCti.EncodeToBytes());
                        System.out.println("CTI: " + cborCti.toString());
                        System.out.println("CTI as BS: " + cborCti.toString());
                        boolean checkSuccessful = checkAndPurgeToken(client, cborCtiAsBS, null);
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
            isActive = isTokenActive(client, token);
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
    private boolean isTokenActive(CoapsPskClient client, CBORObject token) throws AceException, CoapException
    {
        CBORObject params = CBORObject.NewMap();
        params.Add(Constants.TOKEN, token);

        CBORObject reply = client.sendRequest("introspect", "post", params);
        return reply.get(CBORObject.FromObject(Constants.ACTIVE)).AsBoolean();
    }
}
