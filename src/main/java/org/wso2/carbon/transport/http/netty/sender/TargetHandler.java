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
package org.wso2.carbon.transport.http.netty.sender;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.log4j.Logger;
import org.wso2.carbon.kernel.CarbonCallback;
import org.wso2.carbon.kernel.CarbonMessage;
import org.wso2.carbon.kernel.TransportSender;
import org.wso2.carbon.transport.common.CarbonMessageImpl;
import org.wso2.carbon.transport.http.netty.common.Constants;
import org.wso2.carbon.transport.http.netty.common.HTTPContentChunk;
import org.wso2.carbon.transport.http.netty.common.Pipe;
import org.wso2.carbon.transport.http.netty.common.Util;
import org.wso2.carbon.transport.http.netty.common.Worker;
import org.wso2.carbon.transport.http.netty.common.WorkerPool;

import java.net.InetSocketAddress;

public class TargetHandler extends ChannelInboundHandlerAdapter {
    private static Logger log = Logger.getLogger(TargetHandler.class);

    private TransportSender sender;
    private ChannelHandlerContext inboundChannelHandlerContext;
    private CarbonMessage cMsg;
    private CarbonCallback callback;

    public TargetHandler(TransportSender sender, ChannelHandlerContext inboundChannelHandlerContext) {
        this.sender = sender;
        this.inboundChannelHandlerContext = inboundChannelHandlerContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse httpResponse = (HttpResponse) msg;

            cMsg = new CarbonMessageImpl(Constants.PROTOCOL_NAME);
            cMsg.setDirection(CarbonMessageImpl.OUT);
            cMsg.setPort(((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
            cMsg.setHost(((InetSocketAddress) ctx.channel().remoteAddress()).getHostName());
            cMsg.setProperty(Constants.HTTP_STATUS_CODE, httpResponse.getStatus().code());
            cMsg.setProperty(Constants.TRANSPORT_HEADERS, Util.getHeaders(httpResponse));
            cMsg.setProperty(Constants.CHNL_HNDLR_CTX, inboundChannelHandlerContext);
            cMsg.setPipe(new Pipe(Constants.TARGET_PIPE));

//            CarbonCallback callback = sender.consumeCallback(ctx.channel());
            WorkerPool.submitJob(new Worker(sender.getCarbonMessageProcessor(), cMsg, callback));
        } else if (msg instanceof HttpContent) {
            HTTPContentChunk chunk;
            if (cMsg != null) {
                if (msg instanceof LastHttpContent) {
                    LastHttpContent lastHttpContent = (LastHttpContent) msg;
                    chunk = new HTTPContentChunk(lastHttpContent);
                } else {
                    DefaultHttpContent httpContent = (DefaultHttpContent) msg;
                    chunk = new HTTPContentChunk(httpContent);
                }
                cMsg.getPipe().addContentChunk(chunk);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("Target channel closed.");
    }

    public void setCallback(CarbonCallback callback) {
        this.callback = callback;
    }
}
