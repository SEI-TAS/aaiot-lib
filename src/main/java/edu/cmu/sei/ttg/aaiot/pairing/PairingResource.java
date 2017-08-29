package edu.cmu.sei.ttg.aaiot.pairing;

import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.credentials.ICredentialStore;
import edu.cmu.sei.ttg.aaiot.network.CoapsPskServer;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * COAP resource that handles pairing.
 * Created by sebastianecheverria on 8/28/17.
 */
public class PairingResource extends CoapResource
{
    private static final int TIMEOUT_IN_MS = 15 * 1000;
    private static final int SLEEP_TIME_IN_MS = 500;
    private static final String RESOURCE_NAME = "pair";

    public static final int PAIRING_PORT = 9877;
    public static final String AS_ID_KEY = "id";
    public static final String AS_PSK_KEY = "psk";
    public static final String DEVICE_ID_KEY = "id";
    public static final String DEVICE_INFO_KEY = "info";

    private String myId;
    private String additionalInfo;
    private ICredentialStore credentialStore;
    private CoapsPskServer coapsPskServer;

    private boolean isPairingFinished;

    /**
     * Constructor
     * @param keyId The id of the PSK to be used in the DTLS connection.
     * @param key The raw bytes of the DTLS key.
     * @param additionalInfo Optional, additional info to be sent, or an empty string if not needed.
     * @param credentialStore Where to store the credentials received through pairing.
     */
    public PairingResource(String keyId, byte[] key, String myId, String additionalInfo, ICredentialStore credentialStore)
    {
        super(RESOURCE_NAME);
        coapsPskServer = new CoapsPskServer(keyId, key, this, PAIRING_PORT);
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
        String asId = request.get(AS_ID_KEY).AsString();
        String psk = request.get(AS_PSK_KEY).AsString();

        // Store PSK and AS info.
        System.out.println("Storing info for AS " + asId);
        credentialStore.storeAS(asId, Base64.getDecoder().decode(psk), exchange.getSourceAddress());

        System.out.println("Sending reply");
        CBORObject reply = CBORObject.NewMap();
        reply.Add(DEVICE_ID_KEY, myId);
        reply.Add(DEVICE_INFO_KEY, additionalInfo);
        exchange.respond(CoAP.ResponseCode.CONTENT, reply.EncodeToBytes());

        isPairingFinished = true;
    }
}
