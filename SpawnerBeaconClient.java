package com.spawnerbeacon;

import com.mojang.blaze3d.platform.InputConstants;
import com.spawnerbeacon.gui.ConfigScreen;
import com.spawnerbeacon.render.BeamRenderer;
import com.spawnerbeacon.spawner.SpawnerTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class SpawnerBeaconClient implements ClientModInitializer {
    public static final String MOD_ID = "spawnerbeacon";
    private static KeyMapping openMenuKey;
    private static KeyMapping toggleKey;
    private static boolean physicalUHeld;

    @Override
    public void onInitializeClient() {
        BeaconConfig.load();
        SpawnerTracker.register();
        BeamRenderer.register();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        // On a German QWERTZ keyboard the physical key is Ü. Minecraft's
        // KEY_LBRACKET token represents that same physical key on the
        // standard US layout, so it remains the configurable default.
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.spawnerbeacon.open_menu",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_LBRACKET,
                category));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.spawnerbeacon.toggle_beams",
                InputConstants.Type.KEYSYM,
                -1,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Normal Minecraft/Fabric keybind. This is fully configurable
            // in Options -> Controls -> Key Binds.
            while (openMenuKey.consumeClick()) {
                openConfig(client);
            }

            // German QWERTZ fallback: poll the physical key directly.
            // This keeps Ü working even when Minecraft's keyboard-layout
            // translation does not match the default US key token.
            boolean uDown = client.screen == null
                    && client.level != null
                    && GLFW.glfwGetKey(client.getWindow().handle(), GLFW.GLFW_KEY_LEFT_BRACKET) == GLFW.GLFW_PRESS;
            if (uDown && !physicalUHeld) {
                openConfig(client);
            }
            physicalUHeld = uDown;

            while (toggleKey.consumeClick()) {
                BeaconConfig cfg = BeaconConfig.get();
                cfg.enabled = !cfg.enabled;
                cfg.save();
                if (client.player != null) {
                    client.player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                            cfg.enabled ? "message.spawnerbeacon.on" : "message.spawnerbeacon.off"));
                }
            }
        });
    }

    private static void openConfig(net.minecraft.client.Minecraft client) {
        if (client.screen == null && client.level != null) {
            client.setScreen(new ConfigScreen());
        }
    }
}
