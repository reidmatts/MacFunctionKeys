package com.macfunctionkeys.mixin.client;

import com.macfunctionkeys.client.mac.MacFunctionKeyManager;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
	@Inject(method = "onFocus", at = @At("RETURN"))
	private void macfunctionkeys$onWindowFocusChanged(boolean focused, CallbackInfo ci) {
		MacFunctionKeyManager.onWindowFocusChanged(focused);
	}

	@Inject(method = "close", at = @At("HEAD"))
	private void macfunctionkeys$onWindowClose(CallbackInfo ci) {
		MacFunctionKeyManager.shutdown();
	}
}
