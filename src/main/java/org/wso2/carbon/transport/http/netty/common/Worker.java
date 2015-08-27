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
import org.wso2.carbon.kernel.CarbonCallback;
import org.wso2.carbon.kernel.CarbonMessage;
import org.wso2.carbon.kernel.CarbonMessageProcessor;

public class Worker implements Runnable {
    private static Logger log = Logger.getLogger(Worker.class);

    private CarbonMessageProcessor carbonMessageProcessor;
    private CarbonMessage msg;
    private CarbonCallback callback;

    public Worker(CarbonMessageProcessor carbonMessageProcessor, CarbonMessage msg, CarbonCallback callback) {
        this.carbonMessageProcessor = carbonMessageProcessor;
        this.msg = msg;
        this.callback = callback;
    }

    public void run() {
        Thread.currentThread().setName("WorkerThread");
        if (msg.getDirection() == CarbonMessage.IN) {  // Message from SourceHandler
            carbonMessageProcessor.receive(msg, callback);
        } else {
            callback.done(msg);
        }
    }

}
