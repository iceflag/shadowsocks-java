package com.shadowsocks.server;

import com.shadowsocks.common.encryption.CryptUtil;
import com.shadowsocks.common.encryption.ICrypt;
import com.shadowsocks.server.Config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectionHandler extends SimpleChannelInboundHandler {

	private static Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
	private AtomicReference<Channel> remoteChannel = new AtomicReference<>();
	private ByteBuf clientCache;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {

		String host = ctx.channel().attr(Config.HOST).get();
		Integer port = ctx.channel().attr(Config.PORT).get();
		ICrypt crypt = ctx.channel().attr(Config.CRYPT_KEY).get();
		clientCache = ctx.channel().attr(Config.BUF).get();

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(ctx.channel().eventLoop())
			.channel(NioSocketChannel.class)
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
			.option(ChannelOption.SO_KEEPALIVE, true)
			.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new RemoteHandler(ctx, crypt, clientCache));
				}
			});

		try {
			final InetAddress inetAddress = InetAddress.getByName(host);
			ChannelFuture channelFuture = bootstrap.connect(inetAddress, port);
			channelFuture.addListener((ChannelFutureListener) future -> {
				if (future.isSuccess()) {
					logger.debug("connect success host = " + host + ",port = " + port + ",inetAddress = " + inetAddress);
					remoteChannel.set(future.channel());
				} else {
					logger.debug("connect fail host = " + host + ",port = " + port + ",inetAddress = " + inetAddress);
					future.cancel(true);
					channelClose();
				}
			});

		} catch (Exception e) {
			logger.error("connect intenet error", e);
			channelClose();
		}

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		ICrypt crypt = ctx.channel().attr(Config.CRYPT_KEY).get();

		ByteBuf buff = (ByteBuf) msg;
		if (buff.readableBytes() <= 0) {
			return;
		}
		byte[] decrypt = CryptUtil.decrypt(crypt, msg);
		if (remoteChannel.get() != null) {
			remoteChannel.get().writeAndFlush(Unpooled.wrappedBuffer(decrypt));
		} else {
			clientCache.writeBytes(decrypt);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.close();
		logger.info("ConnectionHandler channelInactive close");
		channelClose();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
		channelClose();
		logger.error("ConnectionHandler error", cause);
	}

	private void channelClose() {
		try {
			if (remoteChannel.get() != null) {
				remoteChannel.get().close();
				remoteChannel = null;
			}
			if (clientCache != null) {
				clientCache.clear();
				clientCache = null;
			}
		} catch (Exception e) {
			logger.error("close channel error", e);
		}
	}
}
