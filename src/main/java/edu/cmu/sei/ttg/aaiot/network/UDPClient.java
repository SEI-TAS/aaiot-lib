package edu.cmu.sei.ttg.aaiot.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Created by sebastianecheverria on 7/24/17.
 */
public class UDPClient {
    private static final int DATA_SIZE = 1024;
    private static final int TIMEOUT = 5 * 1000;

    private InetAddress serverIP;
    private int serverPort;
    private DatagramSocket socket;

    public UDPClient(InetAddress serverIP, int serverPort) throws IOException
    {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.socket = new DatagramSocket();
    }

    public void sendData(String data) throws IOException
    {
        byte[] dataToSend = data.getBytes();

        int bytesSent = 0;
        while (bytesSent < dataToSend.length)
        {
            int packageSize = Math.min(DATA_SIZE, dataToSend.length - bytesSent);
            byte[] packageToSend = Arrays.copyOfRange(dataToSend, bytesSent, bytesSent + packageSize);
            DatagramPacket sendPacket = new DatagramPacket(packageToSend, packageSize, serverIP, serverPort);
            socket.send(sendPacket);
            bytesSent += packageSize;
        }
    }

    public String receiveData() throws IOException
    {
        byte[] receivedData = new byte[DATA_SIZE];
        DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);

        socket.setSoTimeout(TIMEOUT);
        socket.receive(receivedPacket);
        socket.setSoTimeout(0);

        byte[] realData = Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
        String data = new String(realData);
        return data;
    }

    public void close()
    {
        socket.close();
    }
}
