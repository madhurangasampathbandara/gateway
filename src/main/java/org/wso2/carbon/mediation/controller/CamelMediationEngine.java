package org.wso2.carbon.mediation.controller;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.log4j.Logger;
import org.wso2.carbon.kernel.CarbonMessageProcessor;
import org.wso2.carbon.kernel.CarbonCallback;
import org.wso2.carbon.kernel.CarbonMessage;
import org.wso2.carbon.kernel.TransportSender;
import org.wso2.carbon.transport.common.CarbonMessageImpl;
import org.wso2.carbon.transport.http.netty.common.Constants;
import org.wso2.carbon.transport.http.netty.sender.Sender;
import org.wso2.carbon.mediation.camel.CamelMediationConsumer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * responsible for receive the client message and send it in to camel
 * and send back the response message to client
 */
public class CamelMediationEngine implements CarbonMessageProcessor {

    private static Logger log = Logger.getLogger(CamelMediationEngine.class);

    private TransportSender sender;
    private final ConcurrentHashMap<String, CamelMediationConsumer> consumers =
            new ConcurrentHashMap<String, CamelMediationConsumer>();

    public CamelMediationEngine(Sender sender) {
        this.sender = sender;
        this.sender.setCarbonMessageProcessor(this);
    }

    @Override public boolean init(TransportSender sender) {
        return true;
    }

    //Client messages will receive here
    public boolean receive(CarbonMessage cMsg, CarbonCallback requestCallback) {
        //start mediation
        if (log.isDebugEnabled()) {
            log.debug("Channel: {} received body: {}" + cMsg.getId().toString());
        }
        Map<String, Object> transportHeaders = (Map<String, Object>) cMsg.getProperty(Constants.TRANSPORT_HEADERS);
        CamelMediationConsumer consumer =
                decideConsumer(cMsg.getProtocol(), (String) transportHeaders.get("Host"), cMsg.getURI());
        if (consumer != null) {
            final Exchange exchange = consumer.getEndpoint().createExchange(transportHeaders, cMsg);
            exchange.setPattern(ExchangePattern.InOut);
            // we want to handle the UoW
            try {
                consumer.createUoW(exchange);
            } catch (Exception e) {
                log.error("Unit of Work creation failed");
            }
            processAsynchronously(exchange, consumer, requestCallback);

        }
        return true;
    }

    @Override public TransportSender getSender() {
        return sender;
    }

    private void processAsynchronously(final Exchange exchange, final CamelMediationConsumer consumer,
                                       final CarbonCallback requestCallback) {
        consumer.getAsyncProcessor().process(exchange, new AsyncCallback() {
            @Override public void done(boolean done) {

                CarbonMessageImpl mediatedResponse = exchange.getOut().getBody(CarbonMessageImpl.class);
                Map<String, Object> mediatedHeaders = exchange.getOut().getHeaders();
                mediatedResponse.setProperty(Constants.TRANSPORT_HEADERS, mediatedHeaders);

                try {
                    requestCallback.done(mediatedResponse);
                } finally {
                    consumer.doneUoW(exchange);
                }
            }
        });
    }

    private CamelMediationConsumer decideConsumer(String protocol, String host, String uri) {
        String messageURL = protocol + "://" + host + uri;
        for (String key : consumers.keySet()) {
            if (key.equals(messageURL.toString()) || key.contains(messageURL.toString())) {
                return consumers.get(key);
            }
        }
        log.info("No route found for the message URL : " + messageURL);
        return null;
    }

    public void addConsumer(String key, CamelMediationConsumer consumer) {
        consumers.put(key, consumer);
    }

    public void removeConsumer(String endpointKey) {

    }
}
