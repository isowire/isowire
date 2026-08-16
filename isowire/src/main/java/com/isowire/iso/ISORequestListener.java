package com.isowire.iso;

/**
 * Interface for processing ISO8583 requests in MyPosServer.
 * Implementations handle business logic for incoming messages.
 */
public interface ISORequestListener {
    /**
     * Process an incoming ISO8583 request and return a response.
     *
     * @param request the incoming ISO8583 message
     * @return the response message, or null if no response should be sent
     */
    ISOMessage processRequest(ISOMessage request);

    /**
     * Called when a parsing/unpacking error occurs for an incoming message.
     * Default implementation is a no-op to maintain backward compatibility.
     *
     * @param clientAddress remote client address (may be null)
     * @param rawData raw bytes that failed to parse (may be null)
     * @param partial partially-parsed ISOMessage (may be empty or null)
     * @param cause exception that describes the parse error
     */
    default ISOMessage processError(String clientAddress, byte[] rawData, ISOMessage partial, Exception cause) {
        return null;
    }
}
