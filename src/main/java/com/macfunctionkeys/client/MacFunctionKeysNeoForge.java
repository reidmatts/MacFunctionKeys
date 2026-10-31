package com.macfunctionkeys.client;

import com.macfunctionkeys.client.mac.MacFunctionKeyManager;
import net.neoforged.fml.common.Mod;

@Mod(MacFunctionKeysClient.MOD_ID)
public class MacFunctionKeysNeoForge {
	public MacFunctionKeysNeoForge() {
		ToastOverlay.init();
		MacFunctionKeyManager.init();
	}
}
