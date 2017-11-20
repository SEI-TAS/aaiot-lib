package edu.cmu.sei.ttg.aaiot.tokens;

/**
 * Created by sebastianecheverria on 11/20/17.
 */
public interface IRemovedTokenTracker
{
    void notifyRemovedToken(String tokenId, String rsId);
}
