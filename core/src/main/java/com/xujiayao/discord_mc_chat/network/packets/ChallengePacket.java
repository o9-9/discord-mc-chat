package com.xujiayao.discord_mc_chat.network.packets;

/**
 * Sent by server to challenge the client.
 *
 * @author Xujiayao
 */
public class ChallengePacket extends Packet {
	public String salt;

	public ChallengePacket(String salt) {
		this.salt = salt;
	}
}
