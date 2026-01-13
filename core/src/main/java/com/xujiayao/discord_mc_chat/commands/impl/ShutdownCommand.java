package com.xujiayao.discord_mc_chat.commands.impl;

import com.xujiayao.discord_mc_chat.DMCC;
import com.xujiayao.discord_mc_chat.commands.Command;
import com.xujiayao.discord_mc_chat.commands.CommandSender;
import com.xujiayao.discord_mc_chat.standalone.StandaloneDMCC;
import com.xujiayao.discord_mc_chat.utils.i18n.I18nManager;

/**
 * Shutdown command implementation (standalone only).
 *
 * @author Xujiayao
 */
public class ShutdownCommand implements Command {

	@Override
	public String name() {
		return "shutdown";
	}

	@Override
	public CommandArgument[] args() {
		return new CommandArgument[0];
	}

	@Override
	public String description() {
		return I18nManager.getDmccTranslation("commands.shutdown.description");
	}

	@Override
	public void execute(CommandSender sender, String... args) {
		System.exit(0);
	}
}
