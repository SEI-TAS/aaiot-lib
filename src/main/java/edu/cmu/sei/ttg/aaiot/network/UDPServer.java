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
        DatagramSocket serverSocket = null;
        try
        {
            serverSocket = new DatagramSocket(port);
            serverSocket.setSoTimeout(TIMEOUT);
            byte[] receivedDataBuffer = new byte[UDPServer.DATA_SIZE];
            while (!stopRequested)
            {
                DatagramPacket receivedPacket = new DatagramPacket(receivedDataBuffer, receivedDataBuffer.length);

                try
                {
                    serverSocket.receive(receivedPacket);
                } catch (SocketTimeoutException so)
                {
                    System.out.println("Socket timed out, stop receiving messages");
                    throw so;
                }

                byte[] realData = Arrays.copyOfRange(receivedPacket.getData(), 0, receivedPacket.getLength());
                String data = new String(realData);
                System.out.println("Received (length: " + receivedPacket.getLength() + " ): " + data);

                handler.handleMessage(data, receivedPacket.getAddress(), receivedPacket.getPort());
            }
        }
        finally
        {
            if(serverSocket != null)
            {
                serverSocket.close();
            }
        }
    }

    public void stop()
    {
        stopRequested = true;
    }

}
