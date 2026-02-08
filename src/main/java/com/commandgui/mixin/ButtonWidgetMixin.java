package com.commandgui.mixin;

import com.commandgui.VirtualKeyboard;
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
        // Only suppress sound if this button is from our virtual keyboard
        if (this instanceof VirtualKeyboard.VirtualKeyboardButton) {
            ci.cancel(); // Suppress the default sound - custom sound plays in the callback
        }
    }
}
