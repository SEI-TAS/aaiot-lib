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
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(port));
        builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});
        builder.setPskStore(new StaticPskStore(keyId, key));

        DTLSConnector connector = new DTLSConnector(builder.build());
        NetworkConfig networkConfig = NetworkConfig.getStandard();
//        networkConfig.set(); // To set a longer timeout for 802.15.4

        LOGGER.info("Setting up DTLS endpoint");
        CoapEndpoint endpoint = new CoapEndpoint(connector, networkConfig);

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

