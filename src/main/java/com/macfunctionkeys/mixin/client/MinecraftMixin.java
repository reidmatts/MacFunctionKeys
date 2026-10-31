package com.macfunctionkeys.mixin.client;

import com.macfunctionkeys.client.ToastOverlay;
import com.macfunctionkeys.client.mac.MacFunctionKeyManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Unique
	private boolean macfunctionkeys$startedNotified = false;

	@Inject(method = "tick", at = @At("TAIL"))
	private void macfunctionkeys$onEndClientTick(CallbackInfo ci) {
		Minecraft self = (Minecraft) (Object) this;
		if (!this.macfunctionkeys$startedNotified) {
			this.macfunctionkeys$startedNotified = true;
			ToastOverlay.init();
			MacFunctionKeyManager.init();
			boolean focused = self.isWindowActive();
			MacFunctionKeyManager.onWindowFocusChanged(focused);
			if (focused) {
				ToastOverlay.showToast(MacFunctionKeyManager.isInversionEnabled());
			}
		}
		MacFunctionKeyManager.tick(self);
	}

	@Inject(method = "stop", at = @At("HEAD"))
	private void macfunctionkeys$onClientStopping(CallbackInfo ci) {
		MacFunctionKeyManager.shutdown();
	}
}
