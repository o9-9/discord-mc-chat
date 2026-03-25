package com.xujiayao.discord_mc_chat.minecraft.mixins;

import com.mojang.brigadier.context.CommandContext;
import com.xujiayao.discord_mc_chat.minecraft.MinecraftEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.commands.SayCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Xujiayao
 */
@Mixin(SayCommand.class)
public class MixinSayCommand {

	@Inject(method = "lambda$register$1", at = @At("HEAD"))
	private static void lambda$register$1(CommandContext<CommandSourceStack> context, PlayerChatMessage playerChatMessage, CallbackInfo ci) {
		MinecraftEvents.COMMAND_MESSAGE.invoker().message(playerChatMessage.decoratedContent().getString(), context.getSource());
	}
}
