package com.macfunctionkeys.client.mac;

import com.macfunctionkeys.client.ToastOverlay;
import com.mojang.blaze3d.platform.InputConstants;
import com.sun.jna.Memory;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public final class MacFunctionKeyManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("MacFunctionKeys");

	private static final int TRIPLE_TAP_COUNT = 3;
	private static final long MAX_TAP_DURATION_MS = 350L;
	private static final long MAX_INTER_TAP_INTERVAL_MS = 450L;

	private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
	private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

	private static volatile boolean inversionEnabled = true;
	private static volatile boolean windowFocused = false;
	private static volatile int savedSystemFKeyMode = -1;
	private static volatile boolean fKeyModeOverridden = false;

	private static volatile Pointer eventTapPort = null;
	private static volatile Pointer runLoopSource = null;
	private static volatile Pointer tapRunLoop = null;
	private static volatile boolean eventTapActive = false;
	private static ApplicationServices.CGEventTapCallBack eventTapCallbackRef;

	private static boolean fnCurrentlyDown = false;
	private static boolean otherKeyPressedDuringFn = false;
	private static long fnPressTimestampMs = 0L;
	private static long lastFnTapReleaseMs = 0L;
	private static int consecutiveFnTaps = 0;

	private static boolean fallbackFnDown = false;
	private static long fallbackFnPressMs = 0L;
	private static long fallbackLastReleaseMs = 0L;
	private static int fallbackTapCount = 0;

	private MacFunctionKeyManager() {
	}

	public static void init() {
		if (!Platform.isMac() || !INITIALIZED.compareAndSet(false, true)) {
			return;
		}

		int currentMode = readSystemHidFKeyMode();
		if (currentMode != -1) {
			savedSystemFKeyMode = currentMode;
		}

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				shutdown();
			} catch (Throwable ignored) {
			}
		}, "MacFunctionKeys-ShutdownHook"));

		startEventTapThread();
	}

	public static synchronized void onWindowFocusChanged(boolean focused) {
		if (!Platform.isMac() || SHUTTING_DOWN.get() || windowFocused == focused) {
			return;
		}
		windowFocused = focused;
		resetFnTapState();

		if (focused) {
			applyActiveMode();
		} else {
			restoreSystemHidFKeyMode();
		}
	}

	public static void tick(Minecraft client) {
		if (!Platform.isMac() || SHUTTING_DOWN.get()) {
			return;
		}
		boolean currentFocus = client.isWindowActive();
		if (currentFocus != windowFocused) {
			onWindowFocusChanged(currentFocus);
		}
		if (windowFocused && !eventTapActive) {
			pollNsEventFnModifierFallback();
		}
	}

	public static synchronized void toggleInversionMode() {
		inversionEnabled = !inversionEnabled;
		if (windowFocused) {
			applyActiveMode();
		}
		ToastOverlay.showToast(inversionEnabled);
	}

	public static boolean isInversionEnabled() {
		return inversionEnabled;
	}

	private static synchronized void applyActiveMode() {
		if (!windowFocused || !inversionEnabled) {
			restoreSystemHidFKeyMode();
			return;
		}

		if (!fKeyModeOverridden && savedSystemFKeyMode == -1) {
			int current = readSystemHidFKeyMode();
			if (current != -1) {
				savedSystemFKeyMode = current;
			}
		}

		if (writeSystemHidFKeyMode(1)) {
			fKeyModeOverridden = true;
		}
	}

	private static synchronized void restoreSystemHidFKeyMode() {
		if (!fKeyModeOverridden) {
			return;
		}
		int targetMode = (savedSystemFKeyMode != -1) ? savedSystemFKeyMode : 0;
		if (writeSystemHidFKeyMode(targetMode)) {
			fKeyModeOverridden = false;
		}
	}

	public static synchronized void shutdown() {
		if (!Platform.isMac()) {
			return;
		}
		SHUTTING_DOWN.set(true);
		windowFocused = false;

		restoreSystemHidFKeyMode();
		stopEventTapThread();
	}

	private static int openIoHidSystemConnect() {
		try {
			IOKit iokit = IOKit.INSTANCE;
			Pointer matching = iokit.IOServiceMatching(IOKit.kIOHIDSystemClass);
			if (matching == null) {
				return 0;
			}
			int service = iokit.IOServiceGetMatchingService(IOKit.kIOMasterPortDefault, matching);
			if (service == 0) {
				return 0;
			}
			IntByReference connectRef = new IntByReference();
			int kr = iokit.IOServiceOpen(service, iokit.mach_task_self(), IOKit.kIOHIDParamConnectType, connectRef);
			iokit.IOObjectRelease(service);
			if (kr != IOKit.KERN_SUCCESS) {
				return 0;
			}
			return connectRef.getValue();
		} catch (Throwable t) {
			LOGGER.debug("Failed to open IOHIDSystem service connection", t);
			return 0;
		}
	}

	private static int readSystemHidFKeyMode() {
		int connect = openIoHidSystemConnect();
		if (connect == 0) {
			return -1;
		}
		try {
			CoreFoundation cf = CoreFoundation.INSTANCE;
			IOKit iokit = IOKit.INSTANCE;
			Pointer key = cf.cfStr(IOKit.kIOHIDFKeyModeKey);
			PointerByReference paramRef = new PointerByReference();
			int kr = iokit.IOHIDCopyCFTypeParameter(connect, key, paramRef);
			if (kr != IOKit.KERN_SUCCESS || paramRef.getValue() == null) {
				return -1;
			}
			Pointer cfNum = paramRef.getValue();
			try (Memory mem = new Memory(4)) {
				if (cf.CFNumberGetValue(cfNum, CoreFoundation.kCFNumberSInt32Type, mem)) {
					return mem.getInt(0);
				}
			} finally {
				cf.CFRelease(cfNum);
			}
		} catch (Throwable t) {
			LOGGER.debug("Failed to read HIDFKeyMode", t);
		} finally {
			IOKit.INSTANCE.IOServiceClose(connect);
		}
		return -1;
	}

	private static boolean writeSystemHidFKeyMode(int mode) {
		int connect = openIoHidSystemConnect();
		if (connect == 0) {
			return false;
		}
		try {
			CoreFoundation cf = CoreFoundation.INSTANCE;
			IOKit iokit = IOKit.INSTANCE;
			Pointer key = cf.cfStr(IOKit.kIOHIDFKeyModeKey);
			try (Memory mem = new Memory(4)) {
				mem.setInt(0, mode);
				Pointer cfNum = cf.CFNumberCreate(Pointer.NULL, CoreFoundation.kCFNumberSInt32Type, mem);
				if (cfNum == null) {
					return false;
				}
				try {
					return iokit.IOHIDSetCFTypeParameter(connect, key, cfNum) == IOKit.KERN_SUCCESS;
				} finally {
					cf.CFRelease(cfNum);
				}
			}
		} catch (Throwable t) {
			LOGGER.debug("Failed to write HIDFKeyMode={}", mode, t);
			return false;
		} finally {
			IOKit.INSTANCE.IOServiceClose(connect);
		}
	}

	private static void startEventTapThread() {
		eventTapCallbackRef = MacFunctionKeyManager::handleCgEvent;

		Thread thread = new Thread(() -> {
			try {
				ApplicationServices appServices = ApplicationServices.INSTANCE;
				CoreFoundation cf = CoreFoundation.INSTANCE;

				long mask = (1L << ApplicationServices.kCGEventKeyDown)
						| (1L << ApplicationServices.kCGEventKeyUp)
						| (1L << ApplicationServices.kCGEventFlagsChanged)
						| (1L << ApplicationServices.NX_SYSDEFINED);

				Pointer tap = appServices.CGEventTapCreate(
						ApplicationServices.kCGSessionEventTap,
						ApplicationServices.kCGHeadInsertEventTap,
						ApplicationServices.kCGEventTapOptionDefault,
						mask,
						eventTapCallbackRef,
						Pointer.NULL
				);

				if (tap == null) {
					tap = appServices.CGEventTapCreate(
							ApplicationServices.kCGSessionEventTap,
							ApplicationServices.kCGHeadInsertEventTap,
							ApplicationServices.kCGEventTapOptionListenOnly,
							mask,
							eventTapCallbackRef,
							Pointer.NULL
					);
				}

				if (tap == null) {
					LOGGER.warn("Could not create CGEventTap; using NSEvent modifier fallback.");
					eventTapActive = false;
					return;
				}

				eventTapPort = tap;
				Pointer source = cf.CFMachPortCreateRunLoopSource(Pointer.NULL, tap, 0);
				if (source == null) {
					cf.CFRelease(tap);
					eventTapPort = null;
					return;
				}

				runLoopSource = source;
				Pointer currentRunLoop = cf.CFRunLoopGetCurrent();
				tapRunLoop = currentRunLoop;

				Pointer defaultMode = cf.cfStr("kCFRunLoopDefaultMode");
				cf.CFRunLoopAddSource(currentRunLoop, source, defaultMode);
				appServices.CGEventTapEnable(tap, true);
				eventTapActive = true;

				cf.CFRunLoopRun();
			} catch (Throwable t) {
				LOGGER.error("Error in MacFunctionKeys CGEventTap run loop", t);
			} finally {
				eventTapActive = false;
			}
		}, "MacFunctionKeys-CGEventTap");

		thread.setDaemon(true);
		thread.start();
	}

	private static void stopEventTapThread() {
		try {
			CoreFoundation cf = CoreFoundation.INSTANCE;
			ApplicationServices appServices = ApplicationServices.INSTANCE;

			Pointer tap = eventTapPort;
			Pointer source = runLoopSource;
			Pointer rl = tapRunLoop;

			eventTapPort = null;
			runLoopSource = null;
			tapRunLoop = null;
			eventTapActive = false;

			if (tap != null) {
				appServices.CGEventTapEnable(tap, false);
			}
			if (rl != null && source != null) {
				Pointer defaultMode = cf.cfStr("kCFRunLoopDefaultMode");
				cf.CFRunLoopRemoveSource(rl, source, defaultMode);
			}
			if (tap != null) {
				cf.CFMachPortInvalidate(tap);
			}
			if (rl != null) {
				cf.CFRunLoopStop(rl);
			}
			if (source != null) {
				cf.CFRelease(source);
			}
			if (tap != null) {
				cf.CFRelease(tap);
			}
		} catch (Throwable t) {
			LOGGER.debug("Error stopping CGEventTap", t);
		}
	}

	private static Pointer handleCgEvent(Pointer proxy, int type, Pointer event, Pointer refcon) {
		try {
			if (type == ApplicationServices.kCGEventTapDisabledByTimeout
					|| type == ApplicationServices.kCGEventTapDisabledByUserInput) {
				Pointer tap = eventTapPort;
				if (tap != null && !SHUTTING_DOWN.get()) {
					ApplicationServices.INSTANCE.CGEventTapEnable(tap, true);
				}
				return event;
			}

			if (!windowFocused || SHUTTING_DOWN.get() || event == null) {
				return event;
			}

			ApplicationServices appServices = ApplicationServices.INSTANCE;

			if (type == ApplicationServices.kCGEventFlagsChanged) {
				long keycode = appServices.CGEventGetIntegerValueField(event, ApplicationServices.kCGKeyboardEventKeycode);
				long flags = appServices.CGEventGetFlags(event);
				boolean fnFlagSet = (flags & ApplicationServices.kCGEventFlagMaskSecondaryFn) != 0L;

				if (keycode == ApplicationServices.kVK_Function || fnFlagSet != fnCurrentlyDown) {
					onFnStateChanged(fnFlagSet);
				} else if (fnCurrentlyDown) {
					otherKeyPressedDuringFn = true;
				}
				return event;
			}

			if (type == ApplicationServices.kCGEventKeyDown || type == ApplicationServices.kCGEventKeyUp) {
				if (fnCurrentlyDown) {
					otherKeyPressedDuringFn = true;
				}

				if (inversionEnabled) {
					long keycode = appServices.CGEventGetIntegerValueField(event, ApplicationServices.kCGKeyboardEventKeycode);
					int mappedVk = mapSpecialTopRowVirtualKey((int) keycode);
					if (mappedVk != -1) {
						appServices.CGEventSetIntegerValueField(event, ApplicationServices.kCGKeyboardEventKeycode, mappedVk);
						dispatchMinecraftFKeyFromVirtualKey(mappedVk, type == ApplicationServices.kCGEventKeyDown, appServices.CGEventGetFlags(event));
						return Pointer.NULL;
					}
				}
			}
		} catch (Throwable ignored) {
		}
		return event;
	}

	private static synchronized void onFnStateChanged(boolean fnDown) {
		long now = System.currentTimeMillis();
		if (fnDown && !fnCurrentlyDown) {
			fnCurrentlyDown = true;
			otherKeyPressedDuringFn = false;
			fnPressTimestampMs = now;
		} else if (!fnDown && fnCurrentlyDown) {
			fnCurrentlyDown = false;
			long pressDuration = now - fnPressTimestampMs;

			if (!otherKeyPressedDuringFn && pressDuration <= MAX_TAP_DURATION_MS) {
				if (now - lastFnTapReleaseMs <= MAX_INTER_TAP_INTERVAL_MS) {
					consecutiveFnTaps++;
				} else {
					consecutiveFnTaps = 1;
				}
				lastFnTapReleaseMs = now;

				if (consecutiveFnTaps >= TRIPLE_TAP_COUNT) {
					consecutiveFnTaps = 0;
					lastFnTapReleaseMs = 0L;
					Minecraft client = Minecraft.getInstance();
					if (client != null) {
						client.execute(MacFunctionKeyManager::toggleInversionMode);
					} else {
						toggleInversionMode();
					}
				}
			} else {
				consecutiveFnTaps = 0;
			}
		}
	}

	private static synchronized void resetFnTapState() {
		fnCurrentlyDown = false;
		otherKeyPressedDuringFn = false;
		consecutiveFnTaps = 0;
		lastFnTapReleaseMs = 0L;
	}

	private static void pollNsEventFnModifierFallback() {
		long flags = CocoaRuntime.getCurrentNSEventModifierFlags();
		boolean fnDown = (flags & CocoaRuntime.NSEventModifierFlagFunction) != 0L;
		long now = System.currentTimeMillis();

		if (fnDown && !fallbackFnDown) {
			fallbackFnDown = true;
			fallbackFnPressMs = now;
		} else if (!fnDown && fallbackFnDown) {
			fallbackFnDown = false;
			if (now - fallbackFnPressMs <= MAX_TAP_DURATION_MS) {
				if (now - fallbackLastReleaseMs <= MAX_INTER_TAP_INTERVAL_MS) {
					fallbackTapCount++;
				} else {
					fallbackTapCount = 1;
				}
				fallbackLastReleaseMs = now;
				if (fallbackTapCount >= TRIPLE_TAP_COUNT) {
					fallbackTapCount = 0;
					fallbackLastReleaseMs = 0L;
					toggleInversionMode();
				}
			} else {
				fallbackTapCount = 0;
			}
		}
	}

	private static int mapSpecialTopRowVirtualKey(int vk) {
		return switch (vk) {
			case ApplicationServices.kVK_BrightnessDown -> ApplicationServices.kVK_F1;
			case ApplicationServices.kVK_BrightnessUp -> ApplicationServices.kVK_F2;
			case ApplicationServices.kVK_MissionControl -> ApplicationServices.kVK_F3;
			case ApplicationServices.kVK_Launchpad -> ApplicationServices.kVK_F4;
			default -> -1;
		};
	}

	private static int mapVirtualKeyToMinecraftKey(int vk) {
		return switch (vk) {
			case ApplicationServices.kVK_F1 -> InputConstants.KEY_F1;
			case ApplicationServices.kVK_F2 -> InputConstants.KEY_F2;
			case ApplicationServices.kVK_F3 -> InputConstants.KEY_F3;
			case ApplicationServices.kVK_F4 -> InputConstants.KEY_F4;
			case ApplicationServices.kVK_F5 -> InputConstants.KEY_F5;
			case ApplicationServices.kVK_F6 -> InputConstants.KEY_F6;
			case ApplicationServices.kVK_F7 -> InputConstants.KEY_F7;
			case ApplicationServices.kVK_F8 -> InputConstants.KEY_F8;
			case ApplicationServices.kVK_F9 -> InputConstants.KEY_F9;
			case ApplicationServices.kVK_F10 -> InputConstants.KEY_F10;
			case ApplicationServices.kVK_F11 -> InputConstants.KEY_F11;
			case ApplicationServices.kVK_F12 -> InputConstants.KEY_F12;
			default -> -1;
		};
	}

	private static void dispatchMinecraftFKeyFromVirtualKey(int vk, boolean pressed, long cgFlags) {
		int mcKey = mapVirtualKeyToMinecraftKey(vk);
		if (mcKey == -1) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		int mods = 0;
		if ((cgFlags & ApplicationServices.kCGEventFlagMaskShift) != 0L) {
			mods |= InputConstants.MOD_SHIFT;
		}
		if ((cgFlags & ApplicationServices.kCGEventFlagMaskControl) != 0L) {
			mods |= InputConstants.MOD_CONTROL;
		}
		if ((cgFlags & ApplicationServices.kCGEventFlagMaskAlternate) != 0L) {
			mods |= InputConstants.MOD_ALT;
		}
		if ((cgFlags & ApplicationServices.kCGEventFlagMaskCommand) != 0L) {
			mods |= InputConstants.MOD_SUPER;
		}
		if ((cgFlags & ApplicationServices.kCGEventFlagMaskAlphaShift) != 0L) {
			mods |= InputConstants.MOD_CAPS_LOCK;
		}

		final int action = pressed ? InputConstants.PRESS : InputConstants.RELEASE;
		final int finalMods = mods;
		client.execute(() -> {
			if (client.getWindow() != null && client.keyboardHandler != null) {
				KeyEvent keyEvent = new KeyEvent(mcKey, vk, finalMods);
				client.keyboardHandler.keyPress(client.getWindow().handle(), action, keyEvent);
			}
		});
	}
}
