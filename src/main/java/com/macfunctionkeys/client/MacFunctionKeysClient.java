package com.macfunctionkeys.client;

import com.macfunctionkeys.client.mac.MacFunctionKeyManager;
import net.fabricmc.api.ClientModInitializer;

public class MacFunctionKeysClient implements ClientModInitializer {
	public static final String MOD_ID = "macfunctionkeys";

	@Override
	public void onInitializeClient() {
		ToastOverlay.init();
		MacFunctionKeyManager.init();
	}
}
