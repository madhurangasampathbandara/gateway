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
package org.wso2.carbon.transport.http.netty.listener;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.log4j.Logger;
import org.wso2.carbon.kernel.CarbonCallback;
import org.wso2.carbon.kernel.CarbonMessage;
import org.wso2.carbon.kernel.CarbonMessageProcessor;
import org.wso2.carbon.transport.common.CarbonMessageImpl;
import org.wso2.carbon.transport.http.netty.common.HTTPContentChunk;
import org.wso2.carbon.transport.http.netty.common.Util;
import org.wso2.carbon.transport.http.netty.common.WorkerPool;
import org.wso2.carbon.transport.http.netty.sender.TargetInitializer;
import org.wso2.carbon.transport.http.netty.common.Constants;
import org.wso2.carbon.transport.http.netty.common.Pipe;
import org.wso2.carbon.transport.http.netty.common.Worker;

import java.net.InetSocketAddress;

public class SourceHandler extends ChannelInboundHandlerAdapter {
    private static Logger log = Logger.getLogger(SourceHandler.class);

    private CarbonMessageProcessor carbonMessageProcessor;
    private CarbonMessage cMsg;
    private Bootstrap bootstrap;
    private ChannelFuture channelFuture;
    private Channel channel;

    private TargetInitializer tInit;
    private CarbonCallback responseCallback;

    public SourceHandler(CarbonMessageProcessor carbonMessageProcessor) {
        this.carbonMessageProcessor = carbonMessageProcessor;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        bootstrap = new Bootstrap();
        tInit = new TargetInitializer(carbonMessageProcessor.getSender(), ctx);
        bootstrap.group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .handler(tInit);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000);
        bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
        bootstrap.option(ChannelOption.SO_RCVBUF, 1048576);
//        NettyListener.getListenerChannelGroup().add(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) msg;
            cMsg = new CarbonMessageImpl(Constants.PROTOCOL_NAME);
            cMsg.setPort(((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
            cMsg.setHost(((InetSocketAddress) ctx.channel().remoteAddress()).getHostName());
            cMsg.setURI(httpRequest.getUri());
            cMsg.setProperty(Constants.HTTP_VERSION, httpRequest.getProtocolVersion().text());
            cMsg.setProperty(Constants.HTTP_METHOD, httpRequest.getMethod().name());
            cMsg.setProperty(Constants.TRANSPORT_HEADERS, Util.getHeaders(httpRequest));
            cMsg.setProperty(Constants.CHNL_HNDLR_CTX, ctx);
            cMsg.setProperty(Constants.SRC_HNDLR, this);
            cMsg.setProperty(Constants.TRG_INIT, tInit);

            cMsg.setPipe(new Pipe(Constants.SOURCE_PIPE));

            responseCallback = new ResponseCallback(ctx);

            WorkerPool.submitJob(new Worker(carbonMessageProcessor, cMsg, responseCallback));
        } else if (msg instanceof HttpContent) {
            HTTPContentChunk chunk;
            if (cMsg != null) {
                if (msg instanceof LastHttpContent) {
                    LastHttpContent lastHttpContent = (LastHttpContent) msg;
                    HttpHeaders trailingHeaders = lastHttpContent.trailingHeaders();
                    for (String val : trailingHeaders.names()) {
                        ((Pipe) cMsg.getPipe()).
                                addTrailingHeader(val, trailingHeaders.get(val));
                    }
                    chunk =  new HTTPContentChunk(lastHttpContent);
                } else {
                    HttpContent httpContent = (HttpContent) msg;
                    chunk = new HTTPContentChunk(httpContent);
                }
                cMsg.getPipe().addContentChunk(chunk);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("Source channel closed.");
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public void setChannelFuture(ChannelFuture channelFuture) {
        this.channelFuture = channelFuture;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

}
