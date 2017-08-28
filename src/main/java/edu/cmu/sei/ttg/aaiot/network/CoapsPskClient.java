package edu.cmu.sei.ttg.aaiot.network;

import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sebastianecheverria on 8/28/17.
 */
public class CoapsPskClient
{
    private CoapClient coapClient;
    private String name;
    private byte[] key;

    public CoapsPskClient(String name, byte[] key)
    {
        this.name = name;
        this.key = key;
    }

    public CBORObject sendRequest(String serverName, int serverPort,
                                               String resource, String method, CBORObject payload)
    {
        String uri = "coaps://" + serverName + ":" + serverPort + "/" + resource;
        coapClient = new CoapClient(uri);
        coapClient.setEndpoint(CoapsPskServer.setupDtlsEndpoint(0, name, key));

        System.out.println("Sending request to server: " + uri);
        CoapResponse response = null;
        if(method.equals("post"))
        {
            response = coapClient.post(payload.EncodeToBytes(), MediaTypeRegistry.APPLICATION_CBOR);
        }
        else if(method.equals("get"))
        {
            response = coapClient.get();
        }

        Map<String, CBORObject> map = null;
        if(response == null)
        {
            System.out.println("Server did not respond.");
            return null;
        }

        System.out.println("Response: " + Utils.prettyPrint(response));

        if(response.getCode() != CoAP.ResponseCode.CREATED &&
                response.getCode() != CoAP.ResponseCode.VALID &&
                response.getCode() != CoAP.ResponseCode.DELETED &&
                response.getCode() != CoAP.ResponseCode.CHANGED &&
                response.getCode() != CoAP.ResponseCode.CONTENT)
        {
            System.out.println("Error received in response: " + response.getCode());
            return null;
        }

        // We assume by now things went well.
        CBORObject responseData = null;
        try
        {
            responseData = CBORObject.DecodeFromBytes(response.getPayload());
        }
        catch(Exception e)
        {
            System.out.println("Reply was received in plain text.");
            return null;
        }

        System.out.println("Response CBOR Payload: " + responseData);
        return responseData;
    }

    public void stop()
    {
        if(coapClient != null)
        {
            coapClient.getEndpoint().stop();
        }
    }

}
