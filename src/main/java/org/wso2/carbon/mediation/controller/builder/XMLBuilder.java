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
package org.wso2.carbon.mediation.controller.builder;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.log4j.Logger;
import org.wso2.carbon.transport.api.ContentChunk;
import org.wso2.carbon.transport.api.Pipe;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class XMLBuilder {
    private static Logger log = Logger.getLogger(XMLBuilder.class);

    public static OMElement buildMessage(Pipe pipe) {
        ContentChunk contentChunk = (ContentChunk) pipe.getContent();
        while (true) {
            ByteBuffer[] buf = contentChunk.getContentChunk();
            for (ByteBuffer b: buf) {
                InputStream is = new ByteBufferBackedInputStream(b);
                try {
                    OMElement ele = buildOMElement(is);
                    System.out.println(ele.toString());
                } catch (Exception e) {
                    log.error("Error while building.", e);
                }
            }
            if (contentChunk.isLastChunk()) {
                break;
            }
        }

        return null;
    }

    /**
     * @param inputStream Reads input stream that use to build OMElement
     * @return  OMElement that generated from input stream
     * @throws Exception at error in generating parser
     */
    private static OMElement buildOMElement(InputStream inputStream) throws Exception {
        XMLStreamReader parser;
        try {
            parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        }
        catch (XMLStreamException e) {
            String msg = "Error in initializing the parser to build the OMElement.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        finally {
            log.info("Reading data from configuration file");
        }
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        return builder.getDocumentElement();
    }
}
