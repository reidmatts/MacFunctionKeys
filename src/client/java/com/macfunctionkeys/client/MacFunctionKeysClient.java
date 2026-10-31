package com.macfunctionkeys.client;

import com.macfunctionkeys.client.mac.MacFunctionKeyManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class MacFunctionKeysClient implements ClientModInitializer {
	public static final String MOD_ID = "macfunctionkeys";

	@Override
	public void onInitializeClient() {
		ToastOverlay.init();
		MacFunctionKeyManager.init();

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			boolean focused = client.isWindowActive();
			MacFunctionKeyManager.onWindowFocusChanged(focused);
			if (focused) {
				ToastOverlay.showToast(MacFunctionKeyManager.isInversionEnabled());
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(MacFunctionKeyManager::tick);

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MacFunctionKeyManager.shutdown());
	}
}
