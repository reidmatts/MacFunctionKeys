package net.fabricmc.fabric.api.client.rendering.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@FunctionalInterface
public interface HudRenderCallback {
	Event<HudRenderCallback> EVENT = EventFactory.createArrayBacked(HudRenderCallback.class, listeners -> (graphics, tickCounter) -> {
		for (HudRenderCallback listener : listeners) {
			listener.onHudRender(graphics, tickCounter);
		}
	});

	void onHudRender(GuiGraphicsExtractor graphics, DeltaTracker tickCounter);
}
