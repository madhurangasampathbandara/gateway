/**
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.mediation.controller;

import io.netty.channel.ChannelInitializer;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.wso2.carbon.kernel.CarbonMessageProcessor;
import org.wso2.carbon.transport.http.netty.listener.NettyListener;
import org.wso2.carbon.transport.http.netty.listener.SourceInitializer;
import org.wso2.carbon.transport.http.netty.sender.Sender;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class POCController {

    public static Properties props = new Properties();
    private static String ID = "HTTP-netty";

    public static void main(String[] args) throws Exception {
        if (args.length == 2) {
            if (args[0].equals("jaxrs")) {
                Sender sender = new Sender();
                CarbonMessageProcessor carbonMessageProcessor = new POCMediationEngine(sender);

                carbonMessageProcessor = new POCJaxRSEngine(sender);

                File propFile = new File(args[1]);
                try {
                    FileInputStream fis = new FileInputStream(propFile);
                    props.load(fis);
                } catch (Exception e) {
                    showUsage();
                    e.printStackTrace();
                    System.exit(0);
                }

                NettyListener.Config nettyConfig = new NettyListener.Config("netty-gw").setPort(9090);
                NettyListener nettyListener = new NettyListener(nettyConfig);
                nettyListener.setDefaultInitializer(new SourceInitializer(carbonMessageProcessor));
                nettyListener.start();

                while (true) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else if (args[0].equals("camel")) {

                File propFile = new File(args[1]);
                try {
                    FileInputStream fis = new FileInputStream(propFile);
                    props.load(fis);
                } catch (Exception e) {
                    showUsage();
                    e.printStackTrace();
                    System.exit(0);
                }

                CamelContext context = new DefaultCamelContext();
                context.disableJMX();

                context.addRoutes(new RouteBuilder() {
                    public void configure() {

                        from("wso2-gw:http://204.13.85.2:9090/service")
                                .choice()
                                .when(header("routeId").regex("r1"))
                                .to("wso2-gw:http://204.13.85.5:5050/services/echo")
                                .when(header("routeId").regex("r2"))
                                .to("wso2-gw:http://204.13.85.5:6060/services/echo")
                                .otherwise()
                                .to("wso2-gw:http://204.13.85.5:7070/services/echo");

                        from("wso2-gw:http://localhost:9090/service")
                                .choice()
                                .when(header("routeId").regex("r1"))
                                .to("wso2-gw:http://localhost:8080/services/echo")
                                .when(header("routeId").regex("r2"))
                                .to("wso2-gw:http://localhost:6060/services/echo")
                                .otherwise()
                                .to("wso2-gw:http://localhost:7070/services/echo");

                    }
                });
                context.start();
                while (true) {
                    Thread.sleep(100000);
                }
            }
        } else {
            showUsage();
        }
    }

    private static void showUsage() {
        System.out.println("\n\n");
        System.out.println("Usage: java -jar server.jar <default |  " + "jaxrs> /path/to/properties.prop");
        System.out.println("\n");
    }

    public CarbonMessageProcessor startPOCController() {
        Sender sender = new Sender();
        CarbonMessageProcessor carbonMessageProcessor = new CamelMediationEngine(sender);

        Map<String, ChannelInitializer> channelInitializers = new HashMap<String, ChannelInitializer>();
        channelInitializers.put("SourceInitializer", new SourceInitializer(carbonMessageProcessor));

        NettyListener.Config nettyConfig = new NettyListener.Config("netty-gw").setPort(9090);
        NettyListener nettyListener = new NettyListener(nettyConfig);
        nettyListener.setDefaultInitializer(new SourceInitializer(carbonMessageProcessor));
        nettyListener.start();

        return carbonMessageProcessor;
    }

}
