package com.macfunctionkeys.client.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface CoreFoundation extends Library {
	CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);

	int kCFNumberSInt32Type = 3;

	Pointer __CFStringMakeConstantString(String cStr);

	Pointer CFNumberCreate(Pointer allocator, int theType, Pointer valuePtr);

	boolean CFNumberGetValue(Pointer number, int theType, Pointer valuePtr);

	void CFRelease(Pointer cf);

	Pointer CFMachPortCreateRunLoopSource(Pointer allocator, Pointer port, long order);

	void CFMachPortInvalidate(Pointer port);

	Pointer CFRunLoopGetCurrent();

	void CFRunLoopAddSource(Pointer rl, Pointer source, Pointer mode);

	void CFRunLoopRemoveSource(Pointer rl, Pointer source, Pointer mode);

	void CFRunLoopRun();

	void CFRunLoopStop(Pointer rl);

	default Pointer cfStr(String str) {
		return __CFStringMakeConstantString(str);
	}
}
