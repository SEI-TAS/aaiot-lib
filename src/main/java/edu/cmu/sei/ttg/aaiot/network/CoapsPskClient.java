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

package edu.cmu.sei.ttg.aaiot.network;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.Endpoint;
import se.sics.ace.Constants;

import java.io.UnsupportedEncodingException;

/**
 * Simple COAP client using DTLS and PSK. Sets up a proper DTLS connection to a CoapsPskServer.
 * Created by sebastianecheverria on 8/28/17.
 */
public class CoapsPskClient
{
    private static String COAP_PREFIX = "coap";
    private static String COAPS_PREFIX = "coaps";

    protected String prefix;
    protected CoapClient coapClient;
    protected String serverName;
    protected int serverPort;
    protected String keyId;
    protected byte[] key;

    /**
     * Constructor.
     * @param serverName The IP or hostname of the server.
     * @param serverPort The port of the server.
     * @param keyId The id of the PSK to use.
     * @param key The raw bytes of the 128-bit PSK.
     */
    public CoapsPskClient(String serverName, int serverPort, String keyId, byte[] key)
    {
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.keyId = keyId;
        this.key = key;

        this.coapClient = new CoapClient();
        this.prefix = COAP_PREFIX;

        // Set up DTLS if needed.
        if(keyId != null && key != null)
        {
            coapClient.setEndpoint(CoapsPskServer.setupDtlsEndpoint(0, keyId, key));
            this.prefix = COAPS_PREFIX;
        }
    }

    /**
     * Sends a COAPS request and returns a CBOR object with the response.
     * @param resource The name of the resource to access.
     * @param method The method to use: "post" or "get".
     * @param payload A CBOR object containing payload, if method is "post".
     * @return
     */
    public CBORObject sendRequest(String resource, String method, CBORObject payload) throws CoapException
    {
        // Support IPv6 addresses properly.
        String formattedServerName = serverName;
        if(serverName.contains(":") && !serverName.startsWith("["))
        {
            formattedServerName = "[" + serverName + "]";
        }

        String uri = prefix + "://" + formattedServerName + ":" + serverPort + "/" + resource;
        coapClient.setURI(uri);

        System.out.println("Sending request to server: " + uri);
        CoapResponse response = null;
        if(method.toLowerCase().equals("post"))
        {
            response = coapClient.post(payload.EncodeToBytes(), MediaTypeRegistry.APPLICATION_CBOR);
        }
        else if(method.toLowerCase().equals("put"))
        {
            response = coapClient.put(payload.EncodeToBytes(), MediaTypeRegistry.APPLICATION_CBOR);
        }
        else if(method.toLowerCase().equals("get"))
        {
            response = coapClient.get();
        }
        else
        {
            throw new RuntimeException("Method '" + method + "' not supported.");
        }

        // Check if there was no reply.
        if(response == null)
        {
            String errorMsg = "Server did not respond, timed out, or cancelled connection.";
            System.out.println(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        System.out.println("Response: " + Utils.prettyPrint(response));
        byte[] responsePayload = response.getPayload();
        System.out.print("Response payload in hex: ");
        for(byte item : responsePayload)
        {
            System.out.printf("%02X ", item);
        }
        System.out.println();

        if(response.getCode() != CoAP.ResponseCode.CREATED &&
                response.getCode() != CoAP.ResponseCode.VALID &&
                response.getCode() != CoAP.ResponseCode.DELETED &&
                response.getCode() != CoAP.ResponseCode.CHANGED &&
                response.getCode() != CoAP.ResponseCode.CONTENT)
        {
            // Get error details and throw as exception for someone higher up to handle.
            try
            {
                CBORObject errorDetails = CBORObject.DecodeFromBytes(CBORObject.FromObject(response.getPayload()).GetByteString());
                System.out.println("Error received in response: " + response.getCode());
                System.out.println("Error map: " + errorDetails.toString());
                String errorName = "";
                if (errorDetails.ContainsKey(CBORObject.FromObject(Constants.ERROR)))
                {
                    errorName = Constants.ERROR_CODES[errorDetails.get(CBORObject.FromObject(Constants.ERROR)).AsInt32()];
                }
                String errorDescription = "";
                if (errorDetails.ContainsKey(CBORObject.FromObject(Constants.ERROR_DESCRIPTION)))
                {
                    errorDescription = errorDetails.get(CBORObject.FromObject(Constants.ERROR_DESCRIPTION)).AsString();
                }

                throw new CoapException("Error received in response: " + response.getCode(), response.getCode(),
                        errorName, errorDescription);
            }
            catch(Exception e)
            {
                // If reply is not CBOR, we assume it is a UTF-8 string.
                System.out.println("Error details was not CBOR: " + e.toString());
                System.out.println("Treating error details as string.");
                try
                {
                    String errorDescription = new String(response.getPayload(), "UTF-8");
                    throw new CoapException("Error received in response: " + response.getCode(), response.getCode(),
                            "", errorDescription);
                }
                catch(UnsupportedEncodingException ex)
                {
                    System.out.println("Error details was not UTF-8 string either. Giving up on how to handle it.");
                }
            }
        }

        // We assume by now things went well.
        CBORObject responseData = null;
        try
        {
            responseData = CBORObject.DecodeFromBytes(responsePayload);
            System.out.println("Response Payload as CBOR: " + responseData);
        }
        catch(Exception e)
        {
            // If reply is not CBOR, we assume it is a UTF-8 string.
            System.out.println("Reply was not CBOR: " + e.toString());
            System.out.println("Treating response as string.");
            try
            {
                responseData = CBORObject.FromObject(new String(responsePayload, "UTF-8"));
            }
            catch(UnsupportedEncodingException ex)
            {
                System.out.println("Reply was not UTF-8 string either. Giving up on how to handle it. Returning as bytes");
                responseData = CBORObject.FromObject(responsePayload);
            }
        }

        return responseData;
    }

    /**
     * Stops the internal DTLS endpoint.
     */
    public void stop()
    {
        if(coapClient != null)
        {
            Endpoint endpoint = coapClient.getEndpoint();
            if(endpoint != null)
            {
                endpoint.stop();
            }
        }
    }

}
