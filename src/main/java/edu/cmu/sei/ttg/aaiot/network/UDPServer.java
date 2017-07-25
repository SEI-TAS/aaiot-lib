package edu.cmu.sei.ttg.aaiot.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

/**
 * Created by sebastianecheverria on 7/24/17.
 */
public class UDPServer {
    private static final int DATA_SIZE = 1024;
    private static final int PORT = 9876;

    private IMessageHandler handler;

    private boolean stopRequested = false;

    public UDPServer(IMessageHandler handler)
    {
        this.handler = handler;
    }

    public void waitForMessages() throws IOException
    {
        stopRequested = false;
        DatagramSocket serverSocket = new DatagramSocket(UDPServer.PORT);
        byte[] receivedDataBuffer = new byte[UDPServer.DATA_SIZE];
        while(!stopRequested)
        {
            DatagramPacket receivedPacket = new DatagramPacket(receivedDataBuffer, receivedDataBuffer.length);
            serverSocket.receive(receivedPacket);

            byte[] realData = Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
            String data = new String(realData);
            System.out.println("Received (length: " + receivedPacket.getLength() + " ): " + data);

            // TODO: check if the port here makes sense... it may be a temp port where messages were sent from, not
            // were the server is listening at.
            handler.handleMessage(data, receivedPacket.getAddress(), receivedPacket.getPort());
        }
    }

    public void stop()
    {
        stopRequested = true;
    }

}
