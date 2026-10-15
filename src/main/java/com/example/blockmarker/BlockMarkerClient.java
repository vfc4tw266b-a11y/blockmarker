package com.example.blockmarker;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BlockMarkerClient implements ClientModInitializer {

    static final int DEFAULT_MENU_KEY = GLFW.GLFW_KEY_F9;
    static KeyMapping removeKey;
    static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        Markers.load();

        // Beide Tasten erscheinen unter Optionen > Steuerung > Tastenbelegung > "Block Marker"
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("blockmarker", "main"));

        removeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.blockmarker.remove", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F6, category));
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.blockmarker.menu", InputConstants.Type.KEYSYM, DEFAULT_MENU_KEY, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MarkerHud.lastTickNs = System.nanoTime();
            while (removeKey.consumeClick()) {
                MarkerHud.removeNearest();
            }
            while (menuKey.consumeClick()) {
                if (client.player != null) {
                    client.setScreenAndShow(new MarkerMenuScreen());
                }
            }
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath("blockmarker", "markers"),
                (graphics, deltaTracker) -> MarkerHud.render(graphics));
    }
}
