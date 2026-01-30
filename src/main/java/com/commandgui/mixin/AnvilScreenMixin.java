package com.commandgui.mixin;

import com.commandgui.VirtualKeyboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends HandledScreen<ScreenHandler> {
    
    @Shadow private TextFieldWidget nameField;
    
    @Unique
    private VirtualKeyboard virtualKeyboard;
    
    @Unique
    private boolean keyboardVisible = false; // Default hidden in anvil
    
    @Unique
    private ButtonWidget toggleButton;
    
    @Unique
    private int maxLength = 50; // Default anvil max length
    
    protected AnvilScreenMixin() {
        super(null, null, null);
    }
    
    @Inject(method = "setup", at = @At("TAIL"))
    private void onSetup(CallbackInfo ci) {
        AnvilScreen screen = (AnvilScreen) (Object) this;
        
        // Get max length using reflection
        try {
            Field maxLengthField = TextFieldWidget.class.getDeclaredField("maxLength");
            maxLengthField.setAccessible(true);
            maxLength = maxLengthField.getInt(nameField);
        } catch (Exception e) {
            maxLength = 50; // Fallback to default
        }
        
        // Initialize keyboard (same position as chat)
        int keyboardX = (this.width - 325) / 2;
        int keyboardY = this.height - 135;
        
        virtualKeyboard = new VirtualKeyboard(keyboardX, keyboardY, new VirtualKeyboard.KeyboardListener() {
            @Override
            public void onKeyTyped(String key) {
                if (nameField != null && !key.isEmpty()) {
                    // Check if adding this character would exceed max length
                    if (nameField.getText().length() < maxLength) {
                        nameField.write(key);
                    }
                }
            }
            
            @Override
            public void onBackspace() {
                if (nameField != null && !nameField.getText().isEmpty()) {
                    int cursor = nameField.getCursor();
                    if (cursor > 0) {
                        String text = nameField.getText();
                        String newText = text.substring(0, cursor - 1) + text.substring(cursor);
                        nameField.setText(newText);
                        nameField.setCursor(cursor - 1, false);
                    }
                }
            }
            
            @Override
            public void onEnter() {
                // Just unfocus the field, don't close the screen
                if (nameField != null) {
                    nameField.setFocused(false);
                }
            }
        }, screen);
        
        // Add toggle button at top right corner (hotbar slot size: 20x20)
        int buttonX = this.x + this.backgroundWidth - 22;
        int buttonY = this.y - 22;
        
        toggleButton = ButtonWidget.builder(
            Text.literal(keyboardVisible ? "H" : "S"),
            button -> {
                keyboardVisible = !keyboardVisible;
                button.setMessage(Text.literal(keyboardVisible ? "H" : "S"));
            }
        ).dimensions(buttonX, buttonY, 20, 20).build();
        
        this.addDrawableChild(toggleButton);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Call parent render
        super.render(context, mouseX, mouseY, delta);
        
        // Render keyboard after everything else (only if visible)
        if (virtualKeyboard != null && keyboardVisible) {
            virtualKeyboard.tick();
            virtualKeyboard.render(context, mouseX, mouseY);
            virtualKeyboard.renderButtons(context, mouseX, mouseY, delta);
        }
        
        // Draw tooltip for toggle button
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (virtualKeyboard != null && keyboardVisible) {
            if (virtualKeyboard.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (virtualKeyboard != null && keyboardVisible) {
            if (virtualKeyboard.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (virtualKeyboard != null && keyboardVisible) {
            AnvilScreen screen = (AnvilScreen) (Object) this;
            if (virtualKeyboard.mouseDragged(mouseX, mouseY, button, deltaX, deltaY, screen.width, screen.height)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}