package com.macfunctionkeys.client.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface CocoaRuntime extends Library {
	CocoaRuntime INSTANCE = Native.load("objc", CocoaRuntime.class);

	long NSEventModifierFlagFunction = 1L << 23;

	Pointer objc_getClass(String name);

	Pointer sel_registerName(String name);

	Pointer objc_msgSend(Pointer receiver, Pointer selector);

	static long getCurrentNSEventModifierFlags() {
		try {
			Pointer nsEventClass = INSTANCE.objc_getClass("NSEvent");
			Pointer modifierFlagsSel = INSTANCE.sel_registerName("modifierFlags");
			if (nsEventClass != null && modifierFlagsSel != null) {
				Pointer raw = INSTANCE.objc_msgSend(nsEventClass, modifierFlagsSel);
				return Pointer.nativeValue(raw);
			}
		} catch (Throwable ignored) {
		}
		return 0L;
	}
}
