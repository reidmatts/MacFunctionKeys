package net.fabricmc.fabric.api.client.rendering.v1;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FunctionalInterface
public interface HudRenderCallback {
	SimpleEvent EVENT = new SimpleEvent();

	void onHudRender(GuiGraphicsExtractor graphics, DeltaTracker tickCounter);

	final class SimpleEvent {
		private final List<HudRenderCallback> listeners = new CopyOnWriteArrayList<>();
		private final HudRenderCallback invoker = (graphics, tickCounter) -> {
			for (HudRenderCallback listener : this.listeners) {
				listener.onHudRender(graphics, tickCounter);
			}
		};

		public void register(HudRenderCallback listener) {
			this.listeners.add(listener);
		}

		public HudRenderCallback invoker() {
			return this.invoker;
		}
	}
}
