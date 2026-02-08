package com.commandgui.mixin;

import com.commandgui.VirtualKeyboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {

    @Shadow private int currentPage;
    @Shadow private List<String> pages;

    @Unique
    private VirtualKeyboard virtualKeyboard;

    @Unique
    private boolean keyboardVisible = false; // Default hidden

    @Unique
    private ButtonWidget toggleButton;

    @Unique
    private static final int MAX_PAGE_LENGTH = 1024; // Minecraft's page character limit

    @Unique
    private TextFieldWidget titleField; // For signing mode

    @Unique
    private boolean isSigningMode = false;

    protected BookEditScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        BookEditScreen screen = (BookEditScreen) (Object) this;

        // Try to get the title field using reflection (for signing mode)
        try {
            Field titleFieldField = BookEditScreen.class.getDeclaredField("titleField");
            titleFieldField.setAccessible(true);
            titleField = (TextFieldWidget) titleFieldField.get(screen);
            isSigningMode = (titleField != null);
        } catch (Exception e) {
            // No title field, we're in editing mode
            titleField = null;
            isSigningMode = false;
        }

        // Initialize keyboard (same position as chat)
        int keyboardX = (this.width - 325) / 2;
        int keyboardY = this.height - 135;

        virtualKeyboard = new VirtualKeyboard(keyboardX, keyboardY, new VirtualKeyboard.KeyboardListener() {
            @Override
            public void onKeyTyped(String key) {
                // Check if we're in signing mode
                if (isSigningMode && titleField != null && titleField.isFocused()) {
                    // Type into title field
                    try {
                        Field maxLengthField = TextFieldWidget.class.getDeclaredField("maxLength");
                        maxLengthField.setAccessible(true);
                        int maxLength = maxLengthField.getInt(titleField);

                        if (titleField.getText().length() < maxLength) {
                            titleField.write(key);
                        }
                    } catch (Exception e) {
                        // Fallback: just try to write
                        if (titleField.getText().length() < 32) { // Default title max
                            titleField.write(key);
                        }
                    }
                } else {
                    // Type into book page
                    if (pages != null && currentPage >= 0 && currentPage < pages.size()) {
                        String currentPageText = pages.get(currentPage);
                        if (currentPageText.length() < MAX_PAGE_LENGTH) {
                            try {
                                BookEditScreen bookScreen = (BookEditScreen) (Object) BookEditScreenMixin.this;
                                for (char c : key.toCharArray()) {
                                    Method charTypedMethod = BookEditScreen.class.getDeclaredMethod("charTyped", char.class, int.class);
                                    charTypedMethod.setAccessible(true);
                                    charTypedMethod.invoke(bookScreen, c, 0);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            @Override
            public void onBackspace() {
                if (isSigningMode && titleField != null && titleField.isFocused()) {
                    // Backspace in title field
                    if (!titleField.getText().isEmpty()) {
                        int cursor = titleField.getCursor();
                        if (cursor > 0) {
                            String text = titleField.getText();
                            String newText = text.substring(0, cursor - 1) + text.substring(cursor);
                            titleField.setText(newText);
                            titleField.setCursor(cursor - 1, false);
                        }
                    }
                } else {
                    // Backspace in book page
                    try {
                        BookEditScreen bookScreen = (BookEditScreen) (Object) BookEditScreenMixin.this;
                        Method keyPressedMethod = BookEditScreen.class.getDeclaredMethod("keyPressed", int.class, int.class, int.class);
                        keyPressedMethod.setAccessible(true);
                        keyPressedMethod.invoke(bookScreen, 259, 14, 0); // 259 = GLFW_KEY_BACKSPACE
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onEnter() {
                if (isSigningMode && titleField != null && titleField.isFocused()) {
                    // Unfocus title field
                    titleField.setFocused(false);
                } else {
                    // New line in book
                    try {
                        BookEditScreen bookScreen = (BookEditScreen) (Object) BookEditScreenMixin.this;
                        Method keyPressedMethod = BookEditScreen.class.getDeclaredMethod("keyPressed", int.class, int.class, int.class);
                        keyPressedMethod.setAccessible(true);
                        keyPressedMethod.invoke(bookScreen, 257, 28, 0); // 257 = GLFW_KEY_ENTER
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, screen);

        // Position button similar to anvil: relative to book GUI bounds
        // The book GUI is 192 pixels wide, centered on screen
        int bookLeft = (this.width - 192) / 2;
        int bookTop = (this.height - 192) / 2;

        // Position at top-right corner of book GUI area (like anvil positioning)
        int buttonX = bookLeft + 192 - 22; // Right edge of book minus button width
        int buttonY = bookTop - 22; // Above the book GUI

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
            String tooltipText = keyboardVisible ? "Hide Keyboard" : "Show Keyboard";
            if (isSigningMode && titleField != null && titleField.isFocused()) {
                tooltipText += " (Title)";
            }
            context.drawTooltip(
                this.textRenderer, 
                Text.literal(tooltipText), 
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
            if (virtualKeyboard.mouseDragged(mouseX, mouseY, button, deltaX, deltaY, this.width, this.height)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}