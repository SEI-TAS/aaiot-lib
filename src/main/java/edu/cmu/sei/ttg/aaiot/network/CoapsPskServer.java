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

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * COAP server using DTLS with PSK. Only allows profile with AES_128_CCM_8 params.
 * Also, only holds one key_id-key pair.
 * Created by sebastianecheverria on 8/28/17.
 */
public class CoapsPskServer extends CoapServer implements AutoCloseable
{
    private static final Logger LOGGER = Logger.getLogger(CoapsPskServer.class.getName());

    /**
     * Constructor.
     *
     */
    public CoapsPskServer(String keyId, byte[] key, Resource resource, int port)
    {
        LOGGER.info("Creating COAPS PSK Server");
        addEndpoint(setupDtlsEndpoint(port, keyId, key));
        add(resource);
    }

    /**
     * Sets up the DTLS connection with PSK, and AES_128_CCM_8 as the algorithm.
     * Only supports 1 keyId/key set.
     * Static so that it can also be used by a client to set up its endpoint.
     * @param port
     * @param keyId
     * @param key
     * @return
     */
    public static CoapEndpoint setupDtlsEndpoint(int port, String keyId, byte[] key)
    {
        LOGGER.info("Setting up DTLS connector builder");
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder().setAddress(new InetSocketAddress(port));
        builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});
        builder.setPskStore(new StaticPskStore(keyId, key));

        DTLSConnector connector = new DTLSConnector(builder.build());
        NetworkConfig networkConfig = NetworkConfig.getStandard();
//        networkConfig.set(); // To set a longer timeout for 802.15.4

        LOGGER.info("Setting up DTLS endpoint");
        CoapEndpoint endpoint = new CoapEndpoint.Builder().setConnector(connector)
                .setNetworkConfig(networkConfig).build();

        return endpoint;
    }

    /**
     * Stops all endpoints.
     */
    @Override
    public void close()
    {
        LOGGER.info("Closing down PSK server ...");
        this.stop();
    }
}

