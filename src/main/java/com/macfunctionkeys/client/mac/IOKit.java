package com.macfunctionkeys.client.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public interface IOKit extends Library {
	IOKit INSTANCE = Native.load("IOKit", IOKit.class);

	int kIOMasterPortDefault = 0;
	int kIOHIDParamConnectType = 1;
	int KERN_SUCCESS = 0;
	String kIOHIDSystemClass = "IOHIDSystem";
	String kIOHIDFKeyModeKey = "HIDFKeyMode";

	int mach_task_self();

	Pointer IOServiceMatching(String name);

	int IOServiceGetMatchingService(int masterPort, Pointer matching);

	int IOServiceOpen(int service, int owningTask, int type, IntByReference connect);

	int IOServiceClose(int connect);

	int IOObjectRelease(int object);

	int IOHIDCopyCFTypeParameter(int connect, Pointer key, PointerByReference parameter);

	int IOHIDSetCFTypeParameter(int connect, Pointer key, Pointer parameter);
}
