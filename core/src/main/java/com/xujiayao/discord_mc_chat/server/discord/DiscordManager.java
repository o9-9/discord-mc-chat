package com.xujiayao.discord_mc_chat.server.discord;

import com.xujiayao.discord_mc_chat.utils.config.ConfigManager;
import com.xujiayao.discord_mc_chat.utils.config.ModeManager;
import com.xujiayao.discord_mc_chat.utils.i18n.I18nManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.xujiayao.discord_mc_chat.Constants.LOGGER;

/**
 * Manages Discord using JDA (Java Discord API).
 *
 * @author Xujiayao
 */
public class DiscordManager {

	private static JDA jda;

	/**
	 * Initializes the Discord bot.
	 *
	 * @return true if initialization is successful, false otherwise.
	 */
	public static boolean init() {
		String token = ConfigManager.getString("discord.bot.token");
		if (token.isBlank()) {
			LOGGER.error("Discord bot token is not set in the config file!");
			return false;
		}

		try {
			jda = JDABuilder.createDefault(token)
					.enableIntents(
							GatewayIntent.MESSAGE_CONTENT,
							GatewayIntent.GUILD_MEMBERS
					)
					.setMemberCachePolicy(MemberCachePolicy.ALL)
					.addEventListeners(new DiscordEventHandler())
					.build();

			// Blocks until JDA is ready
			try (ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "DMCC-FutureChecker"))) {
				CompletableFuture<Void> readyFuture = CompletableFuture.runAsync(() -> {
					try {
						jda.awaitReady();
					} catch (InterruptedException e) {
						LOGGER.error("Discord bot initialization was interrupted", e);
					}
				});

				CompletableFuture<Void> checkFuture = CompletableFuture.runAsync(() -> {
					if (!readyFuture.isDone()) {
						LOGGER.warn("Waiting for JDA to be ready, this may take a while (maximum 1 minute)...");
					}
				}, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS, executor));

				readyFuture.join();
				checkFuture.cancel(false);
			}

			LOGGER.info("Discord bot is ready. Logged in as tag: \"{}\"", jda.getSelfUser().getAsTag());
		} catch (Exception e) {
			LOGGER.error("Discord bot initialization was interrupted", e);
		}

		// Blocks until commands are updated
		try (ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "DMCC-FutureChecker"))) {
			List<CommandData> commands = new ArrayList<>();
			commands.add(Commands.slash("help", I18nManager.getDmccTranslation("commands.help.description")));
			commands.add(Commands.slash("reload", I18nManager.getDmccTranslation("commands.reload.description")));
			if ("standalone".equals(ModeManager.getMode())) {
				commands.add(Commands.slash("shutdown", I18nManager.getDmccTranslation("commands.shutdown.description")));
			}

			CompletableFuture<List<Command>> updateFuture = jda.updateCommands().addCommands(commands).submit();
			CompletableFuture<Void> checkFuture = CompletableFuture.runAsync(() -> {
				if (!updateFuture.isDone()) {
					LOGGER.warn("Registering Discord DMCC commands, this may take a while (maximum 1 minute)...");
				}
			}, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS, executor));
			updateFuture.join();
			checkFuture.cancel(false);

			LOGGER.info("Discord DMCC commands registered successfully!");
			return true;
		} catch (Exception e) {
			LOGGER.error("Failed to register Discord DMCC commands", e);
		}

		return false;
	}

	/**
	 * Shuts down the Discord bot.
	 */
	public static void shutdown() {
		if (jda != null) {
			jda.shutdown();
			try {
				if (ConfigManager.getBoolean("shutdown.graceful_shutdown")) {
					// Allow up to 10 minutes for ongoing requests to complete
					boolean ignored = jda.awaitShutdown(Duration.ofMinutes(10));
				} else {
					// Allow up to 5 seconds for ongoing requests to complete
					boolean ignored = jda.awaitShutdown(Duration.ofSeconds(5));
				}
			} catch (Exception ignored) {
			}
			jda.shutdownNow();
		}
	}
}
