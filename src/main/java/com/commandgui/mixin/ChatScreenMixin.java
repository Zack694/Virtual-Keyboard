package com.commandgui.mixin;

import com.commandgui.VirtualKeyboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    private VirtualKeyboard virtualKeyboard;

    @Unique
    private boolean keyboardVisible = false; // Default hidden

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ChatScreen screen = (ChatScreen) (Object) this;

        // Initialize keyboard at the bottom center of the screen
        int keyboardX = (this.width - 325) / 2;
        int keyboardY = this.height - 135;

        virtualKeyboard = new VirtualKeyboard(keyboardX, keyboardY, new VirtualKeyboard.KeyboardListener() {
            @Override
            public void onKeyTyped(String key) {
                if (chatField != null) {
                    // Insert the character at the cursor position
                    chatField.write(key);
                }
            }

            @Override
            public void onBackspace() {
                if (chatField != null) {
                    // Delete one character before cursor
                    String currentText = chatField.getText();
                    int cursor = chatField.getCursor();
                    
                    if (cursor > 0 && currentText.length() > 0) {
                        String newText = currentText.substring(0, cursor - 1) + currentText.substring(cursor);
                        chatField.setText(newText);
                        chatField.setCursor(cursor - 1);
                    }
                }
            }

            @Override
            public void onEnter() {
                if (chatField != null && !chatField.getText().isEmpty()) {
                    // Send the chat message
                    String message = chatField.getText();
                    if (client != null && client.player != null) {
                        client.player.networkHandler.sendChatMessage(message);
                    }
                    // Close the chat screen
                    if (client != null) {
                        client.setScreen(null);
                    }
                }
            }
        }, screen);

        // Make keyboard visible by default
        keyboardVisible = true;
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

    @Override
    public boolean mouseReleased(Click click) {
        if (virtualKeyboard != null && keyboardVisible) {
            if (virtualKeyboard.mouseReleased(click.x(), click.y(), click.button())) {
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (virtualKeyboard != null && keyboardVisible) {
            if (virtualKeyboard.mouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY, this.width, this.height)) {
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean wasAlreadyHandled) {
        if (virtualKeyboard != null && keyboardVisible) {
            if (virtualKeyboard.mouseClicked(click.x(), click.y(), click.button())) {
                return true;
            }
        }
        return super.mouseClicked(click, wasAlreadyHandled);
    }
}
