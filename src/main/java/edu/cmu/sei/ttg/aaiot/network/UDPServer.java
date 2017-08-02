package edu.cmu.sei.ttg.aaiot.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * Created by sebastianecheverria on 7/24/17.
 */
public class UDPServer {
    private static final int DATA_SIZE = 1024;
    private static final int TIMEOUT = 10 * 1000;

    private IMessageHandler handler;
    private int port;

    private boolean stopRequested = false;

    public UDPServer(IMessageHandler handler, int port)
    {
        this.handler = handler;
        this.port = port;
    }

    public void waitForMessages() throws IOException
    {
        stopRequested = false;
        DatagramSocket serverSocket = new DatagramSocket(port);
        serverSocket.setSoTimeout(TIMEOUT);
        byte[] receivedDataBuffer = new byte[UDPServer.DATA_SIZE];
        while(!stopRequested)
        {
            DatagramPacket receivedPacket = new DatagramPacket(receivedDataBuffer, receivedDataBuffer.length);

            try
            {
                serverSocket.receive(receivedPacket);
            }
            catch(SocketTimeoutException so)
            {
                System.out.println("Socket timed out, stop receiving messages");
                break;
            }

            byte[] realData = Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
            String data = new String(realData);
            System.out.println("Received (length: " + receivedPacket.getLength() + " ): " + data);

            handler.handleMessage(data, receivedPacket.getAddress(), receivedPacket.getPort());
        }

        serverSocket.close();
    }

    public void stop()
    {
        stopRequested = true;
    }

}
