package com.macfunctionkeys.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
	@Inject(
			method = "extractRenderState",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;applyCursor(Lcom/mojang/blaze3d/platform/Window;)V"
			)
	)
	private void macfunctionkeys$renderHudCallbackWhenNotInLevel(
			DeltaTracker deltaTracker,
			boolean renderHud,
			boolean renderScreen,
			CallbackInfo ci,
			@Local GuiGraphicsExtractor graphics
	) {
		if (!renderHud && graphics != null) {
			HudRenderCallback.EVENT.invoker().onHudRender(graphics, deltaTracker);
		}
	}
}
