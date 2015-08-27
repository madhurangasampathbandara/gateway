package org.wso2.carbon.mediation.camel;

import org.apache.camel.*;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.log4j.Logger;
import org.wso2.carbon.kernel.CarbonMessage;
import org.wso2.carbon.mediation.controller.CamelMediationEngine;

import java.util.Map;

/**
 * Represents the CamelMediationEndpoint endpoint.
 */
public class CamelMediationEndpoint extends DefaultEndpoint {

    private static Logger log = Logger.getLogger(CamelMediationEndpoint.class);

    private CamelMediationEngine engine;
    private CarbonCamelMessageUtil carbonCamelMessageUtil;

/*    public CamelMediationEndpoint() {
    }

    public CamelMediationEndpoint(String uri, CamelMediationComponent component) {
        super(uri, component);
    }

    public CamelMediationEndpoint(String endpointUri) {
        super(endpointUri);
    }*/

    public CamelMediationEndpoint(String uri, CamelMediationComponent component, CamelMediationEngine engine) {
        super(uri, component);
        this.engine = engine;
        carbonCamelMessageUtil = new CarbonCamelMessageUtil();
    }

    public Producer createProducer() throws Exception {
        CamelMediationProducer producer = new CamelMediationProducer(this, engine);
        return producer;
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        CamelMediationConsumer consumer = new CamelMediationConsumer(this, processor, engine);
        return consumer;
    }

    public boolean isSingleton() {
        return true;
    }

    public void setEngine(CamelMediationEngine camelMediationEngine) {
        this.engine = camelMediationEngine;
    }

    public CamelMediationEngine getEngine() {
        return engine;
    }

    public Exchange createExchange(Map<String, Object> headers, CarbonMessage cmsg) {
        Exchange exchange = createExchange();
        carbonCamelMessageUtil.setCamelHeadersToClientRequest(exchange, headers, cmsg);
        //carbonCamelMessageUtil.setCamelRequestBody(exchange, cmsg);
        //addHeadersToExchange(exchange.getIn(), headers);
        exchange.getIn().setBody(cmsg);
        return exchange;
    }

    private void addHeadersToExchange(Message in, Map<String, Object> headers) {
        in.setHeaders(headers);
    }

    public CarbonCamelMessageUtil getCarbonCamelMessageUtil() {
        return carbonCamelMessageUtil;
    }

}
