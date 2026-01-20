package com.xujiayao.discord_mc_chat.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.xujiayao.discord_mc_chat.Constants;
import com.xujiayao.discord_mc_chat.network.packets.AuthResponsePacket;
import com.xujiayao.discord_mc_chat.network.packets.ChallengePacket;
import com.xujiayao.discord_mc_chat.network.packets.DisconnectPacket;
import com.xujiayao.discord_mc_chat.network.packets.HandshakePacket;
import com.xujiayao.discord_mc_chat.network.packets.KeepAlivePacket;
import com.xujiayao.discord_mc_chat.network.packets.LoginSuccessPacket;
import com.xujiayao.discord_mc_chat.network.packets.Packet;
import com.xujiayao.discord_mc_chat.utils.CryptUtils;
import com.xujiayao.discord_mc_chat.utils.config.ConfigManager;
import com.xujiayao.discord_mc_chat.utils.config.ModeManager;
import com.xujiayao.discord_mc_chat.utils.i18n.I18nManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import static com.xujiayao.discord_mc_chat.Constants.LOGGER;

/**
 * Handles server-side network events and handshake protocol.
 *
 * @author Xujiayao
 */
public class ServerHandler extends SimpleChannelInboundHandler<Packet> {

	private final NettyServer server;
	private String expectedNonce;
	private boolean authenticated = false;
	private String clientName;

	public ServerHandler(NettyServer server) {
		this.server = server;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		// Wait for handshake
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (clientName != null) {
			LOGGER.info(I18nManager.getDmccTranslation("network.server.client_disconnected_normal", clientName));
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
		switch (packet) {
			case HandshakePacket p -> {
				if (!isWhitelisted(p.serverName)) {
					LOGGER.warn(I18nManager.getDmccTranslation("network.server.whitelist_refused", p.serverName));
					ctx.writeAndFlush(new DisconnectPacket("network.disconnect.not_whitelisted", p.serverName));
					ctx.close();
					return;
				}

				if (!Constants.VERSION.equals(p.version)) {
					LOGGER.warn(I18nManager.getDmccTranslation("network.server.version_mismatch", p.version, Constants.VERSION));
					ctx.writeAndFlush(new DisconnectPacket("network.disconnect.version_mismatch", p.version, Constants.VERSION));
					ctx.close();
					return;
				}

				this.clientName = p.serverName;
				this.expectedNonce = CryptUtils.generateRandomString(16);
				ctx.writeAndFlush(new ChallengePacket(this.expectedNonce));
			}
			case AuthResponsePacket p -> {
				String correctHash = CryptUtils.sha256(this.expectedNonce + server.getSharedSecret());

				if (correctHash.equals(p.hash)) {
					this.authenticated = true;
					LOGGER.info(I18nManager.getDmccTranslation("network.server.auth_success", clientName));
					ctx.writeAndFlush(new LoginSuccessPacket(ConfigManager.getString("language")));
					// TODO: Add to active clients list
				} else {
					LOGGER.warn(I18nManager.getDmccTranslation("network.server.auth_failed", clientName));
					ctx.writeAndFlush(new DisconnectPacket("network.disconnect.auth_failed"));
					ctx.close();
				}
			}
			case KeepAlivePacket ignored -> {
				// No-op, just resets idle timer
			}
			case null, default -> {
				// Handle other packets if authenticated
				if (!authenticated) {
					ctx.writeAndFlush(new DisconnectPacket("network.disconnect.not_authenticated"));
					ctx.close();
				}
			}
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent e) {
			if (e.state() == IdleState.READER_IDLE) {
				LOGGER.warn(I18nManager.getDmccTranslation("network.server.client_timeout", clientName != null ? clientName : "unknown"));
				ctx.close();
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		LOGGER.error("Exception in ServerHandler", cause);
		ctx.close();
	}

	private boolean isWhitelisted(String serverName) {
		if ("single_server".equals(ModeManager.getMode()) && "Internal".equals(serverName)) {
			return true;
		}

		JsonNode serversNode = ConfigManager.getConfigNode("multi_server.servers");
		if (serversNode.isArray()) {
			for (JsonNode node : serversNode) {
				if (serverName.equals(node.path("name").asText())) {
					return true;
				}
			}
		}
		return false;
	}
}
