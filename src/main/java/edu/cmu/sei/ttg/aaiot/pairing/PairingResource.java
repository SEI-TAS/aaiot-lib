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

package edu.cmu.sei.ttg.aaiot.pairing;

import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.credentials.IASCredentialStore;
import edu.cmu.sei.ttg.aaiot.network.CoapsPskServer;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import se.sics.ace.Constants;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * COAP resource that handles pairing.
 * Created by sebastianecheverria on 8/28/17.
 */
public class PairingResource extends CoapResource
{
    private static final int TIMEOUT_IN_MS = 25 * 1000;
    private static final int SLEEP_TIME_IN_MS = 500;
    private static final String RESOURCE_NAME = "pair";

    public static final String PAIRING_KEY_ID = "Authentication01";
    public static final int PAIRING_PORT = 9877;
    public static final int AS_ID_KEY = 2;
    public static final int AS_PSK_KEY = -1;
    public static final int DEVICE_ID_KEY = 3;
    public static final int DEVICE_INFO_KEY = 4;

    private String myId;
    private String additionalInfo;
    private IASCredentialStore credentialStore;
    private CoapsPskServer coapsPskServer;

    private boolean isPairingFinished;

    /**
     * Constructor
     * @param key The raw bytes of the DTLS key.
     * @param additionalInfo Optional, additional info to be sent, or an empty string if not needed.
     * @param credentialStore Where to store the credentials received through pairing.
     */
    public PairingResource(byte[] key, String myId, String additionalInfo, IASCredentialStore credentialStore)
    {
        super(RESOURCE_NAME);
        LOGGER.info("Setting up Pairing Resource");
        coapsPskServer = new CoapsPskServer(PAIRING_KEY_ID, key, this, PAIRING_PORT);
        this.credentialStore = credentialStore;
        this.myId = myId;
        this.additionalInfo = additionalInfo;
        if(this.additionalInfo == null)
        {
            this.additionalInfo = "";
        }
    }

    /**
     * Starts a local server for pairing, and wait until pairing finshes or is aborted.
     * @return True if the pairing procedure was successful, false if not.
     */
    public boolean pair()
    {
        System.out.println("Starting pairing server");
        isPairingFinished = false;
        coapsPskServer.start();

        boolean success = true;
        Instant starts = Instant.now();
        while(!isPairingFinished)
        {
            try
            {
                Thread.sleep(SLEEP_TIME_IN_MS);
            }
            catch (InterruptedException e)
            {
                System.out.println("Wait to finish pairing interrupted.");
                success = false;
                break;
            }

            Duration ellapsed = Duration.between(starts, Instant.now());
            if(ellapsed.toMillis() > TIMEOUT_IN_MS)
            {
                System.out.println("Timeout reached, aborting pairing.");
                success = false;
                break;
            }
        }

        // Stop the pairing server.
        coapsPskServer.stop();
        System.out.println("Stopped pairing server");
        return success;
    }

    /**
     * Handles the actual pairing request, storing credentials and returning the specified info.
     * @param exchange the COAP exchange structure with the request info.
     */
    @Override
    public void handlePOST(CoapExchange exchange)
    {
        System.out.println("Receiving pairing request");
        CBORObject request = CBORObject.DecodeFromBytes(exchange.getRequestPayload());
        System.out.println("Request as CBOR: " + request.toString());
        String asId = new String(request.get(CBORObject.FromObject(AS_ID_KEY)).GetByteString());
        byte[] psk = request.get(CBORObject.FromObject(AS_PSK_KEY)).GetByteString();

        // Store PSK and AS info.
        System.out.println("Storing info for AS " + asId);
        CBORObject reply = CBORObject.NewMap();
        boolean wasStoringSuccessful = credentialStore.storeAS(asId, psk, exchange.getSourceAddress());
        if(wasStoringSuccessful)
        {
            reply.Add(DEVICE_ID_KEY, myId);
            reply.Add(DEVICE_INFO_KEY, additionalInfo);
        }
        else
        {
            reply.Add(Constants.ERROR, Constants.INVALID_REQUEST);
            reply.Add(Constants.ERROR_DESCRIPTION, "Could not store keys.");
        }

        System.out.println("Sending reply");
        exchange.respond(CoAP.ResponseCode.CONTENT, reply.EncodeToBytes());

        isPairingFinished = true;
    }
}
