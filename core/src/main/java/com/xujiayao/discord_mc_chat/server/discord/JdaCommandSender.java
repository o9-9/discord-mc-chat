package com.xujiayao.discord_mc_chat.server.discord;

import com.xujiayao.discord_mc_chat.commands.CommandSender;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Command sender implementation for JDA slash commands.
 *
 * @author Xujiayao
 */
public class JdaCommandSender implements CommandSender {

	private final SlashCommandInteractionEvent event;

	public JdaCommandSender(SlashCommandInteractionEvent event) {
		this.event = event;
	}

	@Override
	public void reply(String message) {
		event.getHook().sendMessage("```" + message + "```").queue();
	}
}
