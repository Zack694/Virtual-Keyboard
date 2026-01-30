package com.commandgui;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class CustomSounds {
    
    public static final Identifier KEYBOARD_CLICK_ID = Identifier.of("commandgui", "keyboard_click");
    public static final SoundEvent KEYBOARD_CLICK = SoundEvent.of(KEYBOARD_CLICK_ID);
    
    public static void initialize() {
        Registry.register(Registries.SOUND_EVENT, KEYBOARD_CLICK_ID, KEYBOARD_CLICK);
    }
}