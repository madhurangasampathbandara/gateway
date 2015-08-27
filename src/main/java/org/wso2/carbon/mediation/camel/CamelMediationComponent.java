package org.wso2.carbon.mediation.camel;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.wso2.carbon.mediation.controller.CamelMediationEngine;
import org.wso2.carbon.mediation.controller.POCController;

import java.util.Map;

/**
 * Represents the component that manages {@link CamelMediationEndpoint}.
 */
public class CamelMediationComponent extends DefaultComponent {

    CamelMediationEngine engine;

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Endpoint endpoint = new CamelMediationEndpoint(uri, this, engine);
        setProperties(endpoint, parameters);
        return endpoint;
    }

    //when the camel component (wso2-gw) starts this method will be called initially
    @Override protected void doStart() throws Exception {
        super.doStart();
        //start netty transport from here get a CamelMediationEngine object and set it to engine.
        this.engine = (CamelMediationEngine) new POCController().startPOCController();
    }
}
