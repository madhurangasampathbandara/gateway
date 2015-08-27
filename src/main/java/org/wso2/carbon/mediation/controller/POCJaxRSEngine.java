/**
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.mediation.controller;

import io.netty.channel.Channel;
import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;
import org.wso2.carbon.kernel.CarbonMessageProcessor;
import org.wso2.carbon.kernel.CarbonCallback;
import org.wso2.carbon.kernel.CarbonMessage;
import org.wso2.carbon.kernel.TransportSender;
import org.wso2.carbon.transport.common.CarbonMessageImpl;
import org.wso2.carbon.mediation.controller.builder.XMLBuilder;

import java.util.HashMap;
import java.util.Map;

public class POCJaxRSEngine implements CarbonMessageProcessor {
    private static Logger log = Logger.getLogger(POCJaxRSEngine.class);

    Map<Channel, CarbonCallback> callbackMap = new HashMap<Channel, CarbonCallback>();

    private static String ENGINE_PROTOCOL = "http";
    private TransportSender sender;

    public POCJaxRSEngine(TransportSender sender) {
        this.sender = sender;
        sender.setCarbonMessageProcessor(this);
    }

    public boolean init(TransportSender sender) {
        return true;
    }

    public boolean receive(CarbonMessage msg, CarbonCallback callback) {

        if (msg.getDirection() == CarbonMessageImpl.IN) {
            log.info(msg.getProperty("HTTP_METHOD") + " "
                    + msg.getURI()  + " "
                    + msg.getProperty("HTTP_VERSION"));


            OMElement incoming = XMLBuilder.buildMessage(msg.getPipe());

//            CarbonMessage outMsg = new CarbonMessageImpl(ENGINE_PROTOCOL);
//
//            log.info("Forward Message");
//            outMsg.setHost("localhost");
//            outMsg.setPort(8280);
//            outMsg.setURI("/services/echo");
//            outMsg.setProperties(msg.getProperties());
//            outMsg.setProperty(ENGINE_PROTOCOL, "Custom-Header", "Hello");
//            sender.send(outMsg);
        }

        return true;
    }

    @Override
    public TransportSender getSender() {
        return this.sender;
    }
}
