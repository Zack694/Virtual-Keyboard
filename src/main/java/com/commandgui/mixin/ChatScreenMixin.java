package com.commandgui.mixin;

import com.commandgui.VirtualKeyboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Unique
    private VirtualKeyboard virtualKeyboard;

    @Unique
    private boolean keyboardVisible = false; // Default hidden

    protected ChatScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ChatScreen screen = (ChatScreen) (Object) this;

        // Initialize keyboard
        int keyboardX = (this.width - 325) / 2;
        int keyboardY = this.height - 135;

        virtualKeyboard = new VirtualKeyboard(keyboardX, keyboardY, new VirtualKeyboard.KeyboardListener() {
            @Override
            public void onKeyTyped(String key) {
                // This would need access to the chat input field
                // Implementation depends on how you want to integrate with chat
            }

            @Override
            public void onBackspace() {
                // Backspace implementation
            }

            @Override
            public void onEnter() {
                // Enter implementation
            }
        }, screen);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (virtualKeyboard != null && keyboardVisible) {
            virtualKeyboard.tick();
            virtualKeyboard.render(context, mouseX, mouseY);
            virtualKeyboard.renderButtons(context, mouseX, mouseY, delta);
        }
    }

    // FIXED for Minecraft 1.21.9+ API
    @Override
    public boolean mouseReleased(Click click) {
        if (virtualKeyboard != null && keyboardVisible) {
            if (virtualKeyboard.mouseReleased(click.x(), click.y(), click.button())) {
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    // FIXED for Minecraft 1.21.9+ API
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (virtualKeyboard != null && keyboardVisible) {
            if (virtualKeyboard.mouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY, this.width, this.height)) {
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }
}
