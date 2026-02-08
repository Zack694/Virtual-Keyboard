package com.commandgui.mixin;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ButtonWidget.class)
public class ButtonWidgetMixin {
    
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void suppressKeyboardButtonSound(SoundManager soundManager, CallbackInfo ci) {
        ButtonWidget self = (ButtonWidget) (Object) this;
        
        // Check if this button is from our virtual keyboard by checking the message
        // Our keyboard buttons have very short messages (1-4 chars typically)
        String message = self.getMessage().getString();
        
        // Suppress sound for keyboard-style buttons (short text like keys)
        // This is a heuristic - you might want to make this more specific
        if (message != null && (
            message.length() <= 4 || 
            message.equals("⌫") || 
            message.equals("↵") || 
            message.equals("⇧") || 
            message.equals("⇪") ||
            message.equals("____") ||
            message.equals("⇥") ||
            message.equals("Esc") ||
            message.equals("Ctl") ||
            message.equals("Win") ||
            message.equals("Alt") ||
            message.equals("Fn") ||
            message.equals("☰")
        )) {
            ci.cancel(); // Suppress the default sound
        }
    }
}
