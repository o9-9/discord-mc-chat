package com.xujiayao.discord_mc_chat.network.packets;

/**
 * Sent by client to initiate connection.
 *
 * @author Xujiayao
 */
public class HandshakePacket extends Packet {
	public String serverName;
	public String version;

	public HandshakePacket(String serverName, String version) {
		this.serverName = serverName;
		this.version = version;
	}
}
