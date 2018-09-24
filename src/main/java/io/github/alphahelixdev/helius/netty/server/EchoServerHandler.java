package io.github.alphahelixdev.helius.netty.server;

import io.github.alphahelixdev.helius.netty.RequestProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class EchoServerHandler extends ChannelInboundHandlerAdapter {
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf in = (ByteBuf) msg;
		String sentData = in.toString(CharsetUtil.UTF_8);
		String returnee = sentData + "-::=::-" + "{}";
		
		RequestProcessor reprocessor = EchoServer.process(sentData);
		
		if(reprocessor != null)
			returnee = sentData + "-::=::-" + reprocessor.getProcessedData();
		
		ChannelFuture f = ctx.writeAndFlush(Unpooled.copiedBuffer(returnee, CharsetUtil.UTF_8)).sync();
		
		if(!f.isSuccess())
			try {
				throw f.cause();
			} catch(Throwable throwable) {
				throwable.printStackTrace();
			}
	}
}
