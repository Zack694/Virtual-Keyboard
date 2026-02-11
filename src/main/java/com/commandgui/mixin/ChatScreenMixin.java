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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    private VirtualKeyboard virtualKeyboard;

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
                        chatField.setCursor(cursor - 1, false);
                    }
                }
            }

            @Override
            public void onEnter() {
                if (chatField != null) {
                    String text = chatField.getText().trim();
                    if (!text.isEmpty()) {
                        if (client != null && client.player != null) {
                            // Properly distinguish between commands and chat messages
                            if (text.startsWith("/")) {
                                client.player.networkHandler.sendChatCommand(text.substring(1));
                            } else {
                                client.player.networkHandler.sendChatMessage(text);
                            }
                        }
                        // Clear the chat field after sending
                        chatField.setText("");
                        if (client != null) {
                            client.setScreen(null);
                        }
                    }
                }
            }
        }, screen);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (virtualKeyboard != null) {
            virtualKeyboard.tick();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (virtualKeyboard != null) {
            virtualKeyboard.render(context, mouseX, mouseY);
            virtualKeyboard.renderButtons(context, mouseX, mouseY, delta);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean wasAlreadyHandled, CallbackInfoReturnable<Boolean> cir) {
        if (virtualKeyboard != null) {
            if (virtualKeyboard.mouseClicked(click.x(), click.y(), click.button())) {
                cir.setReturnValue(true);
            }
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (virtualKeyboard != null) {
            if (virtualKeyboard.mouseReleased(click.x(), click.y(), click.button())) {
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (virtualKeyboard != null) {
            if (virtualKeyboard.mouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY, this.width, this.height)) {
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }
}
