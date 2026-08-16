package com.isowire.example;

import com.isowire.iso.ISOMessage;
import com.isowire.iso.ISORequestListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleRequestListener implements ISORequestListener {
    private static final Logger logger = LoggerFactory.getLogger(SampleRequestListener.class);

    @Override
    public ISOMessage processRequest(ISOMessage request) {
        String mti = request.getMTI();
        logger.info("Processing request: MTI={}, STAN={}", mti, request.getString(11));

        // Easy response building using clone methods
        ISOMessage response = request.createResponse();
        response.set(39, "00"); // Approved

        logger.info("Generated response: MTI={}, ResponseCode={}", response.getMTI(), response.get(39));
        return response;
    }
}
