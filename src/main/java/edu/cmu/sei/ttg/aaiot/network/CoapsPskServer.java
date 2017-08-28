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
 * Created by sebastianecheverria on 8/28/17.
 */
public class CoapsPskServer extends CoapServer implements AutoCloseable
{
    // TODO: use timeout.
    private static final int TIMEOUT = 10 * 1000;

    private static final Logger LOGGER = Logger.getLogger(CoapsPskServer.class.getName());

    private String name;
    private byte[] key;
    private Resource handler;
    private int port;

    /**
     * Constructor.
     *
     */
    public CoapsPskServer(String name, byte[] key, Resource handler, int port)
    {
        this.name = name;
        this.key = key;
        this.handler = handler;
        this.port = port;

        addEndpoint(setupDtlsEndpoint(port, name, key));

        add(this.handler);
    }

    public static CoapEndpoint setupDtlsEndpoint(int port, String name, byte[] key)
    {
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(port));
        builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});
        builder.setPskStore(new StaticPskStore(name, key));

        DTLSConnector connector = new DTLSConnector(builder.build());
        CoapEndpoint endpoint = new CoapEndpoint(connector, NetworkConfig.getStandard());

        return endpoint;
    }

    @Override
    public void close()
    {
        LOGGER.info("Closing down pairing server ...");
        this.stop();
    }
}

