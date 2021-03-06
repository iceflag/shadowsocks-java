import com.shadowsocks.common.constants.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocksClientMainTest {

	private static final Logger logger = LoggerFactory.getLogger(SocksClientMainTest.class);

	static final String HOST = System.getProperty("host", "127.0.0.1");
	static final int PORT = Integer.parseInt(System.getProperty("port", "1081"));

	public static void main(String[] args) {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new SocksTestInitializer());

			ChannelFuture f = b.connect(HOST, PORT)
				.addListener((ChannelFutureListener) future -> {
					if (future.isSuccess()) {
						logger.info(Constants.LOG_MSG + " Socks5 connection success......");
					} else {
						logger.error(Constants.LOG_MSG + "Socks5 connection failed......");
					}
				}).sync();

			// Wait until the connection is closed.
			f.channel().closeFuture().sync();
		} catch (Exception e) {
			logger.error("", e);
		} finally {
			// Shut down the event loop to terminate all threads.
			group.shutdownGracefully();
		}
	}
}
