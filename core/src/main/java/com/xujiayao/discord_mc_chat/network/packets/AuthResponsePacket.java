package com.xujiayao.discord_mc_chat.network.packets;

/**
 * Sent by client with the calculated hash.
 *
 * @author Xujiayao
 */
public class AuthResponsePacket extends Packet {
	public String hash;

	public AuthResponsePacket(String hash) {
		this.hash = hash;
	}
}
