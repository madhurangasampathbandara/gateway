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
package org.wso2.carbon.transport.http.netty.common;

import org.apache.log4j.Logger;
import org.wso2.carbon.transport.api.ContentChunk;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Pipe implements org.wso2.carbon.transport.api.Pipe {
    private static Logger log = Logger.getLogger(Pipe.class);

    private String name = "Buffer";

    private BlockingQueue<ContentChunk> contentQueue = new LinkedBlockingQueue<ContentChunk>();

    private Map trailingheaders = new ConcurrentHashMap<String, String>();

    private AtomicBoolean isReadComplete = new AtomicBoolean(false);

    private AtomicBoolean isWriteComplete = new AtomicBoolean(false);

    public Pipe(String name) {
        this.name = name;
    }

    public ContentChunk getContent() {
        try {
            return contentQueue.take();
        } catch (InterruptedException e) {
            log.error("Error while retrieving chunk from queue.", e);
            return null;
        }
    }

    public void addContentChunk(ContentChunk contentChunk) {
        if (contentChunk.isLastChunk()) {
            isReadComplete.getAndSet(true);
        }
        contentQueue.add(contentChunk);
    }

    public boolean isWriteComplete() {
        return isWriteComplete.get();
    }

    public boolean isReadComplete() {
        return isReadComplete.get();
    }

    public void addTrailingHeader(String key, String value) {
        trailingheaders.put(key, value);
    }

    public Map getTrailingheaderMap() {
        return trailingheaders;
    }
}
