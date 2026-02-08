package com.commandgui.mixin;

import com.commandgui.VirtualKeyboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.Click;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class SignEditScreenMixin extends Screen {

    @Shadow private String[] messages;
    @Shadow private int currentRow;

    @Unique
    private VirtualKeyboard virtualKeyboard;

    @Unique
    private boolean keyboardVisible = true; // Default shown for signs

    @Unique
    private ButtonWidget toggleButton;

    protected SignEditScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        AbstractSignEditScreen screen = (AbstractSignEditScreen) (Object) this;

        // Initialize keyboard (same position as chat)
        int keyboardX = (screen.width - 325) / 2;
        int keyboardY = screen.height - 135;

        virtualKeyboard = new VirtualKeyboard(keyboardX, keyboardY, new VirtualKeyboard.KeyboardListener() {
            @Override
            public void onKeyTyped(String key) {
                if (messages != null && currentRow >= 0 && currentRow < messages.length) {
                    String current = messages[currentRow];
                    // Sign line limit: 90 characters (Minecraft limit)
                    if (current.length() < 90) {
                        messages[currentRow] = current + key;
                    }
                }
            }

            @Override
            public void onBackspace() {
                if (messages != null && currentRow >= 0 && currentRow < messages.length) {
                    String current = messages[currentRow];
                    if (!current.isEmpty()) {
                        messages[currentRow] = current.substring(0, current.length() - 1);
                    }
                }
            }

            @Override
            public void onEnter() {
                // Move to next line (sign has 4 lines: 0-3)
                if (currentRow < 3) {
                    try {
                        // Use reflection to change currentRow
                        java.lang.reflect.Field field = AbstractSignEditScreen.class.getDeclaredField("currentRow");
                        field.setAccessible(true);
                        field.set(SignEditScreenMixin.this, currentRow + 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, screen);

        // Add toggle button at top right corner
        int buttonX = this.width - 22;
        int buttonY = 2;

        toggleButton = ButtonWidget.builder(
            Text.literal(keyboardVisible ? "H" : "S"),
            button -> {
                keyboardVisible = !keyboardVisible;
                button.setMessage(Text.literal(keyboardVisible ? "H" : "S"));
            }
        ).dimensions(buttonX, buttonY, 20, 20).build();

        this.addDrawableChild(toggleButton);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (virtualKeyboard != null && keyboardVisible) {
            virtualKeyboard.tick();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (virtualKeyboard != null && keyboardVisible) {
            virtualKeyboard.render(context, mouseX, mouseY);
            virtualKeyboard.renderButtons(context, mouseX, mouseY, delta);
        }

        // Draw tooltip
        if (toggleButton != null && toggleButton.isHovered()) {
            context.drawTooltip(
                this.textRenderer, 
                Text.literal(keyboardVisible ? "Hide Keyboard" : "Show Keyboard"), 
                mouseX, 
                mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean consumed) {
        if (virtualKeyboard != null && keyboardVisible) {
            if (virtualKeyboard.mouseClicked(click.mouseX(), click.mouseY(), click.button())) {
                return true;
            }
        }
        return super.mouseClicked(click, consumed);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (virtualKeyboard != null && keyboardVisible) {
            if (virtualKeyboard.mouseReleased(click.mouseX(), click.mouseY(), click.button())) {
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (virtualKeyboard != null && keyboardVisible) {
            AbstractSignEditScreen screen = (AbstractSignEditScreen) (Object) this;
            if (virtualKeyboard.mouseDragged(click.mouseX(), click.mouseY(), click.button(), deltaX, deltaY, screen.width, screen.height)) {
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }
}