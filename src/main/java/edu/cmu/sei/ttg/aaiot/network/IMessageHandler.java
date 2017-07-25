package edu.cmu.sei.ttg.aaiot.network;

import java.net.InetAddress;

/**
 * Created by sebastianecheverria on 7/24/17.
 */
public interface IMessageHandler {
    void handleMessage(String message, InetAddress sourceIP, int sourcePort);
}
