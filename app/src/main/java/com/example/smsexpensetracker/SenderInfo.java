package com.example.smsexpensetracker;

/**
 * A simple data class to hold information about an SMS sender, including their most recent message.
 */
public class SenderInfo {
    private final String address;
    private final String latestMessage;

    public SenderInfo(String address, String latestMessage) {
        this.address = address;
        this.latestMessage = latestMessage;
    }

    public String getAddress() {
        return address;
    }

    public String getLatestMessage() {
        return latestMessage;
    }
}
