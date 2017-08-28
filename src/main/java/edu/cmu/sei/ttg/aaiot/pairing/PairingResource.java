package edu.cmu.sei.ttg.aaiot.pairing;

import com.upokecenter.cbor.CBORObject;
import edu.cmu.sei.ttg.aaiot.credentials.ICredentialStore;
import edu.cmu.sei.ttg.aaiot.network.CoapsPskServer;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Created by sebastianecheverria on 8/28/17.
 */
public class PairingResource extends CoapResource
{
    private static final int TIMEOUT = 15 * 1000;

    public static final int PAIRING_PORT = 9877;
    public static final String AS_ID_KEY = "id";
    public static final String AS_PSK_KEY = "psk";
    public static final String DEVICE_ID_KEY = "id";
    public static final String DEVICE_INFO_KEY = "info";

    private String name;
    private byte[] key;
    private String additionalInfo;
    private ICredentialStore credentialStore;
    private CoapsPskServer coapsPskServer;

    private boolean isPairingFinished;

    public PairingResource(String name, byte[] key, String additionalInfo, ICredentialStore credentialStore)
    {
        super("pair");
        this.name = name;
        this.key = key;
        this.additionalInfo = additionalInfo;
        this.credentialStore = credentialStore;
    }

    public boolean startPairing() throws IOException
    {
        System.out.println("Starting pairing server");
        isPairingFinished = false;
        coapsPskServer = new CoapsPskServer(name, key, this, PAIRING_PORT);
        coapsPskServer.start();

        boolean success = true;
        Instant starts = Instant.now();
        while(!isPairingFinished)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                System.out.println("Wait to finish pairing interrupted.");
                success = false;
                break;
            }

            Instant ends = Instant.now();
            Duration ellapsed = Duration.between(starts, ends);
            if(ellapsed.toMillis() > TIMEOUT)
            {
                System.out.println("Timeout reached, stopping pairing server");
                success = false;
                break;
            }
        }

        // Stop the pairing server.
        coapsPskServer.stop();
        System.out.println("Stopped pairing server");
        return success;
    }

    @Override
    public void handlePOST(CoapExchange exchange)
    {
        System.out.println("Receiving pairing request");
        CBORObject request = CBORObject.DecodeFromBytes(exchange.getRequestPayload());

        // Get AS ID and PSK.
        String asId = request.get(AS_ID_KEY).AsString();
        String psk = request.get(AS_PSK_KEY).AsString();

        // Store PSK and AS info.
        System.out.println("Storing info for AS " + asId);
        credentialStore.storeAS(asId, Base64.getDecoder().decode(psk), exchange.getSourceAddress());

        System.out.println("Sending reply");
        CBORObject reply = CBORObject.NewMap();
        reply.Add(DEVICE_ID_KEY, name);
        reply.Add(DEVICE_INFO_KEY, additionalInfo);
        exchange.respond(CoAP.ResponseCode.CONTENT, reply.EncodeToBytes());

        isPairingFinished = true;
    }
}
