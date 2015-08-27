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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.log4j.Logger;
import org.wso2.carbon.kernel.CarbonCallback;
import org.wso2.carbon.kernel.CarbonMessage;
import org.wso2.carbon.transport.api.Pipe;
import org.wso2.carbon.kernel.TransportSender;
import org.wso2.carbon.transport.http.netty.common.Constants;
import org.wso2.carbon.transport.http.netty.common.HTTPContentChunk;
import org.wso2.carbon.transport.http.netty.common.Util;
import org.wso2.carbon.transport.http.netty.listener.SourceHandler;

import java.net.InetSocketAddress;


public class Sender extends TransportSender {
    private static Logger log = Logger.getLogger(Sender.class);

    public Sender() {
        super(Constants.PROTOCOL_NAME);
    }

    public boolean init() {
        return true;
    }

    public boolean send(CarbonMessage msg, final CarbonCallback callback) {
        final ChannelHandlerContext inboundCtx = (ChannelHandlerContext)
                msg.getProperty(Constants.CHNL_HNDLR_CTX);

        final HttpRequest httpRequest = Util.createHttpRequest(msg);
        final Pipe pipe = msg.getPipe();

        final SourceHandler srcHandler = (SourceHandler) msg.getProperty(Constants.SRC_HNDLR);

        Bootstrap bootstrap = srcHandler.getBootstrap();

        InetSocketAddress address = new InetSocketAddress(msg.getHost(), msg.getPort());

        final TargetInitializer tInit = (TargetInitializer) msg.getProperty(Constants.TRG_INIT);

        if (srcHandler.getChannelFuture() == null) {
            ChannelFuture future = bootstrap.connect(address);
            final Channel outboundChannel = future.channel();
//           putCallback(outboundChannel, callback);
//           outboundChannel.attr(TargetHandler.callbackAttribute).set(callback);
            future.addListener(new ChannelFutureListener() {

                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        tInit.getTargetHandler().setCallback(callback);
                        outboundChannel.write(httpRequest);
                        while (true) {
                            HTTPContentChunk chunk = (HTTPContentChunk) pipe.getContent();
                            HttpContent httpContent = chunk.getHttpContent();
                            if (httpContent instanceof LastHttpContent) {
                                outboundChannel.writeAndFlush(httpContent);
                                break;
                            }
                            if (httpContent != null) {
                                outboundChannel.write(httpContent);
                            }
                        }
                        srcHandler.setChannelFuture(future);
                        srcHandler.setChannel(outboundChannel);
                    } else {
                        // Close the connection if the connection attempt has failed.
                        outboundChannel.close();
                    }
                }
            });

        } else {
            ChannelFuture future = srcHandler.getChannelFuture();
//            putCallback(srcHandler.getChannel(), callback);
//            srcHandler.getChannel().attr(TargetHandler.callbackAttribute).set(callback);
            tInit.getTargetHandler().setCallback(callback);
            if (future.isSuccess() && srcHandler.getChannel().isActive()) {
                srcHandler.getChannel().write(httpRequest);
                while (true) {
                    HTTPContentChunk chunk = (HTTPContentChunk) pipe.getContent();
                    HttpContent httpContent = chunk.getHttpContent();
                    if (httpContent instanceof LastHttpContent) {
                        srcHandler.getChannel().writeAndFlush(httpContent);
                        break;
                    }
                    srcHandler.getChannel().write(httpContent);
                }

            } else {
                final ChannelFuture futuretwo = bootstrap.connect(address);
                final Channel outboundChannel = futuretwo.channel();
//                putCallback(outboundChannel, callback);
//                outboundChannel.attr(TargetHandler.callbackAttribute).set(callback);
                tInit.getTargetHandler().setCallback(callback);
                futuretwo.addListener(new ChannelFutureListener() {

                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (futuretwo.isSuccess()) {
                            outboundChannel.write(httpRequest);
                            while (true) {
                                HTTPContentChunk chunk = (HTTPContentChunk) pipe.getContent();
                                HttpContent httpContent = chunk.getHttpContent();
                                if (httpContent instanceof LastHttpContent) {
                                    outboundChannel.writeAndFlush(httpContent);
                                    break;
                                }
                                outboundChannel.write(httpContent);
                            }
                            srcHandler.setChannelFuture(future);

                        } else {
                            // Close the connection if the connection attempt has failed.
                            outboundChannel.close();
                        }
                    }
                });
            }
        }

        return false;
    }

    public boolean sendBack(CarbonMessage msg) {
        final ChannelHandlerContext inboundChCtx = (ChannelHandlerContext)
                msg.getProperty(Constants.CHNL_HNDLR_CTX);
        final Pipe pipe = msg.getPipe();
        final HttpResponse response = Util.createHttpResponse(msg);

        inboundChCtx.write(response);
        while (true) {
            HTTPContentChunk chunk = (HTTPContentChunk) pipe.getContent();
            HttpContent httpContent = chunk.getHttpContent();
            if (httpContent != null) {
                if (httpContent instanceof LastHttpContent ||
                        httpContent instanceof DefaultLastHttpContent) {
                    inboundChCtx.writeAndFlush(httpContent);
                    break;
                }
                inboundChCtx.write(httpContent);
            }
        }
        return false;
    }
}
