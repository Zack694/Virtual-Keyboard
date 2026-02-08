package com.commandgui.mixin;

import com.commandgui.VirtualKeyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
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

    protected ChatScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ChatScreen screen = (ChatScreen) (Object) this;

        // Initialize keyboard
        int keyboardX = (screen.width - 325) / 2;
        int keyboardY = screen.height - 135;

        virtualKeyboard = new VirtualKeyboard(keyboardX, keyboardY, new VirtualKeyboard.KeyboardListener() {
            @Override
            public void onKeyTyped(String key) {
                if (chatField != null && !key.isEmpty()) {
                    chatField.write(key);
                }
            }

            @Override
            public void onBackspace() {
                if (chatField != null && !chatField.getText().isEmpty()) {
                    int cursor = chatField.getCursor();
                    if (cursor > 0) {
                        String text = chatField.getText();
                        String newText = text.substring(0, cursor - 1) + text.substring(cursor);
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
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null) {
                            if (text.startsWith("/")) {
                                client.player.networkHandler.sendChatCommand(text.substring(1));
                            } else {
                                client.player.networkHandler.sendChatMessage(text);
                            }
                        }
                        client.setScreen(null);
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
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (virtualKeyboard != null) {
            if (virtualKeyboard.mouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (virtualKeyboard != null) {
            if (virtualKeyboard.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (virtualKeyboard != null) {
            ChatScreen screen = (ChatScreen) (Object) this;
            if (virtualKeyboard.mouseDragged(mouseX, mouseY, button, deltaX, deltaY, screen.width, screen.height)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}