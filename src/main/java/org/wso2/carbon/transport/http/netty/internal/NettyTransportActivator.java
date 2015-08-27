/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.transport.http.netty.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.transport.http.netty.listener.NettyListener;
import org.wso2.carbon.transport.http.netty.listener.ssl.SSLConfig;
import org.wso2.carbon.transports.CarbonTransport;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * OSGi BundleActivator of the Netty transport component
 */
public class NettyTransportActivator implements BundleActivator {
    private static final Log log = LogFactory.getLog(NettyTransportActivator.class);

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        for(NettyListener listener : createNettyServices()) {
            bundleContext.registerService(CarbonTransport.class, listener, null);
        }
    }

    /**
     * Parse the  netty-transports.xml config file & create the Netty transport instances
     * @return Netty transport instances
     */
    private Set<NettyListener> createNettyServices() {
        final Set<NettyListener> listeners = new HashSet<>();
        DefaultHandler handler = new DefaultHandler() {

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                super.startElement(uri, localName, qName, attributes);
                if (qName.equals("listener")) {
                    String id = attributes.getValue("id");
                    String host = attributes.getValue("host");
                    String port = attributes.getValue("port");
                    String bossThreadPoolSize = attributes.getValue("bossThreadPoolSize");
                    String workerThreadPoolSize = attributes.getValue("workerThreadPoolSize");
                    String execHandlerThreadPoolSize = attributes.getValue("execHandlerThreadPoolSize");

                    String scheme = attributes.getValue("scheme");
                    String keystoreFile = attributes.getValue("keystoreFile");
                    String keystorePass = attributes.getValue("keystorePass");
                    String certPass = attributes.getValue("certPass");
                    String trustStoreFile = attributes.getValue("trustStoreFile");
                    String trustStorePass = attributes.getValue("trustStorePass");

                    NettyListener.Config nettyConfig = new NettyListener.Config(id);
                    if (host != null) {
                        nettyConfig.setHost(host);
                    }
                    if (port != null) {
                        nettyConfig.setPort(Integer.parseInt(port));
                    }
                    if (bossThreadPoolSize != null) {
                        nettyConfig.setBossThreads(Integer.parseInt(bossThreadPoolSize));
                    }
                    if (workerThreadPoolSize != null) {
                        nettyConfig.setWorkerThreads(Integer.parseInt(workerThreadPoolSize));
                    }
                    if (execHandlerThreadPoolSize != null) {
                        nettyConfig.setExecThreads(Integer.parseInt(execHandlerThreadPoolSize));
                    }

                    if (scheme != null && scheme.equalsIgnoreCase("https")) {
                        if (certPass == null) {
                            certPass = keystorePass;
                        }
                        if (keystoreFile == null || keystorePass == null) {
                            throw new IllegalArgumentException("keyStoreFile or keyStorePass not defined for HTTPS scheme");
                        }
                        File keyStore = new File(keystoreFile);
                        if (!keyStore.exists()) {
                            throw new IllegalArgumentException("KeyStore File " + keystoreFile + " not found");
                        }
                        SSLConfig sslConfig =
                                new SSLConfig(keyStore, keystorePass).setCertPass(certPass);
                        if (trustStoreFile != null) {
                            File trustStore = new File(trustStoreFile);
                            if (!trustStore.exists()) {
                                throw new IllegalArgumentException("trustStore File " + trustStoreFile + " not found");
                            }
                            if (trustStorePass == null) {
                                throw new IllegalArgumentException("trustStorePass is not defined for HTTPS scheme");
                            }
                            sslConfig.setTrustStore(trustStore).setTrustStorePass(trustStorePass);
                        }
                        nettyConfig.enableSsl(sslConfig);
                    }
                    listeners.add(new NettyListener(nettyConfig));
                }
            }
        };

        String nettyTransportsXML = "repository" + File.separator + "conf" + File.separator +
                "transports" + File.separator + "netty-transports.xml";
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(nettyTransportsXML, handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Cannot parse " + nettyTransportsXML, e);
        }
        return listeners;
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

    }
}
