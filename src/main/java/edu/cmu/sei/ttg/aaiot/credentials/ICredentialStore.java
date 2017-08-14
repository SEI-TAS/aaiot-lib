package edu.cmu.sei.ttg.aaiot.credentials;

import COSE.OneKey;

import java.net.InetAddress;

/**
 * Created by sebastianecheverria on 7/25/17.
 */
public interface ICredentialStore
{
    boolean storeAS(String id, byte[] psk, InetAddress ipAddress);
    String getASid();
    OneKey getASPSK();
    byte[] getRawASPSK();
    InetAddress getASIP();
}
