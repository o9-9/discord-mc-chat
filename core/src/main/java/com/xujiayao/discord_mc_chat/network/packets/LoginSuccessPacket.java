package com.xujiayao.discord_mc_chat.network.packets;

/**
 * Sent by server to confirm authentication success.
 *
 * @author Xujiayao
 */
public class LoginSuccessPacket extends Packet {
	public String language;

	public LoginSuccessPacket(String language) {
		this.language = language;
	}
}
