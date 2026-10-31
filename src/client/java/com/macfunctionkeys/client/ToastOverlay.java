package com.macfunctionkeys.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class ToastOverlay {
	public static final long TOAST_DURATION_MS = 2500L;
	private static final long FADE_IN_MS = 180L;
	private static final long FADE_OUT_MS = 350L;

	private static volatile long toastStartTimeMs = 0L;
	private static volatile boolean toastStandardFKeysMode = true;

	private ToastOverlay() {
	}

	public static void init() {
		HudRenderCallback.EVENT.register(ToastOverlay::render);
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath("macfunctionkeys", "mode_toast"),
				(graphics, deltaTracker) -> HudRenderCallback.EVENT.invoker().onHudRender(graphics, deltaTracker)
		);
	}

	public static void showToast(boolean standardFKeysActive) {
		toastStandardFKeysMode = standardFKeysActive;
		toastStartTimeMs = System.currentTimeMillis();
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		long start = toastStartTimeMs;
		if (start == 0L) {
			return;
		}

		long elapsed = System.currentTimeMillis() - start;
		if (elapsed < 0L || elapsed >= TOAST_DURATION_MS) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		if (font == null) {
			return;
		}

		float alphaFactor = 1.0F;
		if (elapsed < FADE_IN_MS) {
			alphaFactor = (float) elapsed / (float) FADE_IN_MS;
		} else if (elapsed > TOAST_DURATION_MS - FADE_OUT_MS) {
			alphaFactor = (float) (TOAST_DURATION_MS - elapsed) / (float) FADE_OUT_MS;
		}
		alphaFactor = Mth.clamp(alphaFactor, 0.05F, 1.0F);

		boolean active = toastStandardFKeysMode;
		String title = "MacFunctionKeys";
		String subtitle = active
				? "Mode: Standard F1-F12 Keys"
				: "Mode: macOS Media Keys";
		String hint = "Triple-tap Fn to toggle";

		int titleWidth = font.width(title);
		int subtitleWidth = font.width(subtitle);
		int hintWidth = font.width(hint);
		int contentWidth = Math.max(titleWidth, Math.max(subtitleWidth, hintWidth));

		int paddingX = 8;
		int paddingY = 6;
		int indicatorWidth = 3;
		int boxWidth = indicatorWidth + paddingX * 2 + contentWidth + 4;
		int boxHeight = paddingY * 2 + 30;

		int margin = 8;
		int x = graphics.guiWidth() - boxWidth - margin;
		int y = margin;

		int bgAlpha = (int) (215 * alphaFactor);
		int borderAlpha = (int) (230 * alphaFactor);
		int textAlpha = (int) (255 * alphaFactor);
		int subTextAlpha = (int) (210 * alphaFactor);

		int bgColor = (bgAlpha << 24) | 0x14161C;
		int accentRGB = active ? 0x38D96B : 0xF59E0B;
		int accentColor = (borderAlpha << 24) | accentRGB;
		int borderColor = (borderAlpha << 24) | 0x343946;

		int titleColor = (textAlpha << 24) | 0xFFFFFF;
		int subtitleColor = (textAlpha << 24) | (active ? 0x7CFC9A : 0xFCD34D);
		int hintColor = (subTextAlpha << 24) | 0x9CA3AF;

		graphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);
		graphics.outline(x, y, boxWidth, boxHeight, borderColor);
		graphics.fill(x, y, x + indicatorWidth, y + boxHeight, accentColor);

		int textX = x + indicatorWidth + paddingX;
		int textY = y + paddingY;

		graphics.text(font, title, textX, textY, titleColor, true);
		graphics.text(font, subtitle, textX, textY + 11, subtitleColor, true);
		graphics.text(font, hint, textX, textY + 22, hintColor, false);
	}
}
