package com.xujiayao.discord_mc_chat.client;

import com.xujiayao.discord_mc_chat.Constants;
import com.xujiayao.discord_mc_chat.network.packets.AuthResponsePacket;
import com.xujiayao.discord_mc_chat.network.packets.ChallengePacket;
import com.xujiayao.discord_mc_chat.network.packets.DisconnectPacket;
import com.xujiayao.discord_mc_chat.network.packets.HandshakePacket;
import com.xujiayao.discord_mc_chat.network.packets.KeepAlivePacket;
import com.xujiayao.discord_mc_chat.network.packets.LoginSuccessPacket;
import com.xujiayao.discord_mc_chat.network.packets.Packet;
import com.xujiayao.discord_mc_chat.utils.CryptUtils;
import com.xujiayao.discord_mc_chat.utils.i18n.I18nManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.concurrent.CompletableFuture;

import static com.xujiayao.discord_mc_chat.Constants.LOGGER;

/**
 * Handles client-side network events and handshake protocol.
 *
 * @author Xujiayao
 */
public class ClientHandler extends SimpleChannelInboundHandler<Packet> {

	private final NettyClient client;
	private final CompletableFuture<Boolean> initialLoginFuture;

	public ClientHandler(NettyClient client, CompletableFuture<Boolean> initialLoginFuture) {
		this.client = client;
		this.initialLoginFuture = initialLoginFuture;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.writeAndFlush(new HandshakePacket(client.getServerName(), Constants.VERSION));
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		LOGGER.info(I18nManager.getDmccTranslation("network.client.disconnected_generic"));

		// Trigger reconnection if this was not an intentional stop
		if (client.isRunning()) {
			LOGGER.info(I18nManager.getDmccTranslation("network.client.reconnecting"));
			client.scheduleReconnect();
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
		if (packet instanceof ChallengePacket p) {
			String hash = CryptUtils.sha256(p.salt + client.getSharedSecret());
			ctx.writeAndFlush(new AuthResponsePacket(hash));

		} else if (packet instanceof LoginSuccessPacket p) {
			I18nManager.load(p.language);
			LOGGER.info(I18nManager.getDmccTranslation("network.client.connected"));

			if (!initialLoginFuture.isDone()) {
				initialLoginFuture.complete(true);
			}

		} else if (packet instanceof DisconnectPacket p) {
			String reason = I18nManager.getDmccTranslation(p.key, p.args);
			LOGGER.error(I18nManager.getDmccTranslation("network.client.disconnected_reason", reason));

			if (!initialLoginFuture.isDone()) {
				initialLoginFuture.complete(false);
			}
			ctx.close();
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent e) {
			if (e.state() == IdleState.WRITER_IDLE) {
				ctx.writeAndFlush(new KeepAlivePacket());
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		LOGGER.error(I18nManager.getDmccTranslation("network.client.connect_failed"), cause);
		if (!initialLoginFuture.isDone()) {
			initialLoginFuture.complete(false);
		}
		ctx.close();
	}
}
