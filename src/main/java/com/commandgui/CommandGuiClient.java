package com.commandgui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CommandGuiClient implements ClientModInitializer {
    
    public static final String MOD_ID = "commandgui";
    private static KeyBinding openGuiKey;
    
    @Override
    public void onInitializeClient() {
        // Register custom sounds
        CustomSounds.initialize();
        
        // Register the payload type on CLIENT side
        PayloadTypeRegistry.playC2S().register(
            CommandPackets.ExecuteCommandPayload.ID,
            CommandPackets.ExecuteCommandPayload.CODEC
        );
        
        // Register keybinding - Category is now KeyBinding.Category.MISC enum
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.commandgui.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            KeyBinding.Category.MISC // CHANGED: String to Category enum
        ));
        
        // Register tick event for keybind
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new PasscodeScreen());
                }
            }
        });
    }
}
