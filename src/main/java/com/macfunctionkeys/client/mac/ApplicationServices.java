package com.macfunctionkeys.client.mac;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface ApplicationServices extends Library {
	ApplicationServices INSTANCE = Native.load("ApplicationServices", ApplicationServices.class);

	int kCGSessionEventTap = 1;
	int kCGHeadInsertEventTap = 0;

	int kCGEventTapOptionDefault = 0;
	int kCGEventTapOptionListenOnly = 1;

	int kCGEventKeyDown = 10;
	int kCGEventKeyUp = 11;
	int kCGEventFlagsChanged = 12;
	int NX_SYSDEFINED = 14;
	int kCGEventTapDisabledByTimeout = 0xFFFFFFFE;
	int kCGEventTapDisabledByUserInput = 0xFFFFFFFF;

	int kCGKeyboardEventKeycode = 9;

	long kCGEventFlagMaskAlphaShift = 0x00010000L;
	long kCGEventFlagMaskShift = 0x00020000L;
	long kCGEventFlagMaskControl = 0x00040000L;
	long kCGEventFlagMaskAlternate = 0x00080000L;
	long kCGEventFlagMaskCommand = 0x00100000L;
	long kCGEventFlagMaskSecondaryFn = 0x00800000L;

	int kVK_Function = 0x3F;
	int kVK_F1 = 0x7A;
	int kVK_F2 = 0x78;
	int kVK_F3 = 0x63;
	int kVK_F4 = 0x76;
	int kVK_F5 = 0x60;
	int kVK_F6 = 0x61;
	int kVK_F7 = 0x62;
	int kVK_F8 = 0x64;
	int kVK_F9 = 0x65;
	int kVK_F10 = 0x6D;
	int kVK_F11 = 0x67;
	int kVK_F12 = 0x6F;

	int kVK_BrightnessDown = 0x91;
	int kVK_BrightnessUp = 0x90;
	int kVK_MissionControl = 0xA0;
	int kVK_Launchpad = 0x83;

	interface CGEventTapCallBack extends Callback {
		Pointer invoke(Pointer proxy, int type, Pointer event, Pointer refcon);
	}

	Pointer CGEventTapCreate(
			int tap,
			int place,
			int options,
			long eventsOfInterest,
			CGEventTapCallBack callback,
			Pointer userInfo
	);

	void CGEventTapEnable(Pointer tap, boolean enable);

	long CGEventGetIntegerValueField(Pointer event, int field);

	void CGEventSetIntegerValueField(Pointer event, int field, long value);

	long CGEventGetFlags(Pointer event);
}
