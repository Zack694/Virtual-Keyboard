package com.commandgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;

public class VirtualKeyboard {

    // Track which buttons are keyboard buttons (for mixin to check)
    public static final Set<ButtonWidget> KEYBOARD_BUTTONS = Collections.newSetFromMap(new WeakHashMap<>());

    private int x;
    private int y;
    private int width = 325;
    private int height = 115;

    private boolean isDragging = false;
    private int dragOffsetX;
    private int dragOffsetY;

    private List<ButtonWidget> keyButtons = new ArrayList<>();
    private Screen parentScreen;

    // Key repeat functionality (PC keyboard style)
    private String currentlyPressedKey = null;
    private long keyPressStartTime = 0;
    private long lastRepeatTime = 0;
    private static final long REPEAT_DELAY = 1000; // 1000ms (1 second) initial delay
    private static final long REPEAT_RATE = 33; // 33ms between repeats (~30 repeats per second)

    // Proper 60% keyboard layout (ANSI standard)
    private static final String[][] KEYBOARD_LAYOUT = {
        {"ESC", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BACK"},
        {"TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\"},
        {"CAPS", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "ENTER"},
        {"SHIFT", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "RSHIFT"},
        {"CTRL", "WIN", "ALT", "SPACE", "RALT", "FN", "MENU"}
    };

    // Shift character mappings
    private static final java.util.Map<String, String> SHIFT_MAP = java.util.Map.ofEntries(
        java.util.Map.entry("1", "!"),
        java.util.Map.entry("2", "@"),
        java.util.Map.entry("3", "#"),
        java.util.Map.entry("4", "$"),
        java.util.Map.entry("5", "%"),
        java.util.Map.entry("6", "^"),
        java.util.Map.entry("7", "&"),
        java.util.Map.entry("8", "*"),
        java.util.Map.entry("9", "("),
        java.util.Map.entry("0", ")"),
        java.util.Map.entry("-", "_"),
        java.util.Map.entry("=", "+"),
        java.util.Map.entry("[", "{"),
        java.util.Map.entry("]", "}"),
        java.util.Map.entry("\\", "|"),
        java.util.Map.entry(";", ":"),
        java.util.Map.entry("'", "\""),
        java.util.Map.entry(",", "<"),
        java.util.Map.entry(".", ">"),
        java.util.Map.entry("/", "?")
    );

    private boolean shiftPressed = false;
    private boolean capsLockOn = false;

    public interface KeyboardListener {
        void onKeyTyped(String key);
        void onBackspace();
        void onEnter();
    }

    private KeyboardListener listener;

    public VirtualKeyboard(int startX, int startY, KeyboardListener listener, Screen parentScreen) {
        this.x = startX;
        this.y = startY;
        this.listener = listener;
        this.parentScreen = parentScreen;
        buildKeyboard();
    }

    private void buildKeyboard() {
        keyButtons.clear();

        int keyWidth = 19;
        int keyHeight = 17;
        int spacing = 2;
        int currentY = y + 20;

        for (int row = 0; row < KEYBOARD_LAYOUT.length; row++) {
            String[] rowKeys = KEYBOARD_LAYOUT[row];
            int currentX = x + 8;

            for (int col = 0; col < rowKeys.length; col++) {
                String key = rowKeys[col];
                int btnWidth = keyWidth;

                // Special key widths
                switch (key) {
                    case "ESC" -> btnWidth = 19;
                    case "BACK" -> btnWidth = 38;
                    case "TAB" -> btnWidth = 29;
                    case "\\" -> btnWidth = 29;
                    case "CAPS" -> btnWidth = 33;
                    case "ENTER" -> btnWidth = 42;
                    case "SHIFT" -> btnWidth = 42;
                    case "RSHIFT" -> btnWidth = 50;
                    case "CTRL", "WIN", "ALT", "RALT", "FN" -> btnWidth = 25;
                    case "SPACE" -> btnWidth = 115;
                    case "MENU" -> btnWidth = 19;
                }

                // Get display text
                String displayText = getDisplayText(key);

                // Create button using builder
                final String keyFinal = key;
                ButtonWidget button = ButtonWidget.builder(
                    Text.literal(displayText),
                    btn -> {
                        playCustomSound();
                        handleKeyPress(keyFinal);
                    }
                )
                .dimensions(currentX, currentY, btnWidth, keyHeight)
                .build();
                
                // Mark this as a keyboard button for sound suppression
                KEYBOARD_BUTTONS.add(button);

                keyButtons.add(button);
                currentX += btnWidth + spacing;
            }

            currentY += keyHeight + spacing;
        }
    }

    private String getDisplayText(String key) {
        // Show shift symbols when shift is pressed
        if (shiftPressed && SHIFT_MAP.containsKey(key)) {
            return SHIFT_MAP.get(key);
        }

        return switch (key) {
            case "BACK" -> "⌫";
            case "ENTER" -> "↵";
            case "SHIFT", "RSHIFT" -> "⇧";
            case "CAPS" -> "⇪";
            case "SPACE" -> "____";
            case "TAB" -> "⇥";
            case "ESC" -> "Esc";
            case "CTRL" -> "Ctl";
            case "WIN" -> "Win";
            case "ALT", "RALT" -> "Alt";
            case "FN" -> "Fn";
            case "MENU" -> "☰";
            default -> key;
        };
    }

    private void playCustomSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            SoundManager soundManager = client.getSoundManager();
            try {
                // Play custom keyboard click sound
                soundManager.play(PositionedSoundInstance.master(CustomSounds.KEYBOARD_CLICK, 1.0f));
            } catch (Exception e) {
                // Fallback - shouldn't happen if sound is registered
                e.printStackTrace();
            }
        }
    }



    public void render(DrawContext context, int mouseX, int mouseY) {
        // Draw main panel background
        context.fill(x, y, x + width, y + height, 0xFF8B8B8B);

        // Draw drag bar
        context.fill(x, y, x + width, y + 16, 0xFF5A5A5A);

        // Draw border
        context.fill(x - 1, y - 1, x + width + 1, y, 0xFF373737);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFF373737);
        context.fill(x - 1, y, x, y + height, 0xFF373737);
        context.fill(x + width, y, x + width + 1, y + height, 0xFF373737);
    }

    public void renderButtons(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render all button widgets
        for (ButtonWidget button : keyButtons) {
            button.render(context, mouseX, mouseY, delta);
        }
    }

    public void tick() {
        // Handle key repeat
        if (currentlyPressedKey != null) {
            long currentTime = System.currentTimeMillis();
            long timeSincePress = currentTime - keyPressStartTime;

            if (timeSincePress > REPEAT_DELAY) {
                long timeSinceLastRepeat = currentTime - lastRepeatTime;
                if (timeSinceLastRepeat > REPEAT_RATE) {
                    handleKeyPress(currentlyPressedKey);
                    playCustomSound();
                    lastRepeatTime = currentTime;
                }
            }
        }
    }

    // FIXED for Minecraft 1.21.11 API
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Only handle clicks if they're within the keyboard bounds
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                // Check if clicking drag bar
                if (mouseY >= y && mouseY <= y + 16) {
                    isDragging = true;
                    dragOffsetX = (int)(mouseX - x);
                    dragOffsetY = (int)(mouseY - y);
                    return true;
                }

                // Check if clicking any key button
                for (ButtonWidget btn : keyButtons) {
                    if (btn.isMouseOver(mouseX, mouseY)) {
                        // Find which key was pressed
                        for (int i = 0; i < KEYBOARD_LAYOUT.length; i++) {
                            for (int j = 0; j < KEYBOARD_LAYOUT[i].length; j++) {
                                if (keyButtons.indexOf(btn) >= 0) {
                                    String key = KEYBOARD_LAYOUT[Math.min(i, KEYBOARD_LAYOUT.length - 1)][Math.min(j, KEYBOARD_LAYOUT[i].length - 1)];
                                    // Only start repeat for keys that should repeat
                                    if (shouldKeyRepeat(key)) {
                                        currentlyPressedKey = key;
                                        keyPressStartTime = System.currentTimeMillis();
                                        lastRepeatTime = keyPressStartTime;
                                    }
                                    break;
                                }
                            }
                        }

                        // Use new API - MouseInput is a record: new MouseInput(int button, int modifiers)
                        // modifiers = 0 for no modifiers (no shift/ctrl/alt held)
                        Click click = new Click(mouseX, mouseY, new MouseInput(button, 0));
                        btn.mouseClicked(click, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean shouldKeyRepeat(String key) {
        // Don't repeat modifier keys or special keys
        return !key.equals("SHIFT") && !key.equals("RSHIFT") && 
               !key.equals("CAPS") && !key.equals("ESC") && 
               !key.equals("TAB") && !key.equals("CTRL") && 
               !key.equals("WIN") && !key.equals("ALT") && 
               !key.equals("RALT") && !key.equals("FN") &&
               !key.equals("MENU");
    }

    // FIXED for Minecraft 1.21.11 API
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Stop key repeat
            currentlyPressedKey = null;

            // Release all buttons - use new API
            Click click = new Click(mouseX, mouseY, new MouseInput(button, 0));
            for (ButtonWidget btn : keyButtons) {
                btn.mouseReleased(click);
            }

            if (isDragging) {
                isDragging = false;
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, int screenWidth, int screenHeight) {
        if (isDragging) {
            x = (int)(mouseX - dragOffsetX);
            y = (int)(mouseY - dragOffsetY);

            x = Math.max(0, Math.min(x, screenWidth - width));
            y = Math.max(0, Math.min(y, screenHeight - height));

            buildKeyboard();
            return true;
        }
        return false;
    }

    private void handleKeyPress(String key) {
        if (listener == null) return;

        switch (key) {
            case "BACK" -> listener.onBackspace();
            case "ENTER" -> listener.onEnter();
            case "SHIFT", "RSHIFT" -> {
                shiftPressed = !shiftPressed;
                buildKeyboard();
                currentlyPressedKey = null; // Don't repeat modifier keys
            }
            case "CAPS" -> {
                capsLockOn = !capsLockOn;
                buildKeyboard();
                currentlyPressedKey = null; // Don't repeat modifier keys
            }
            case "ESC", "TAB", "CTRL", "WIN", "ALT", "RALT", "FN", "MENU" -> {
                currentlyPressedKey = null; // Don't repeat these keys
            }
            case "SPACE" -> listener.onKeyTyped(" ");
            case "\\" -> listener.onKeyTyped("\\");
            default -> {
                String output = key;

                // Handle shift symbols
                if (shiftPressed && SHIFT_MAP.containsKey(key)) {
                    output = SHIFT_MAP.get(key);
                } else if (key.length() == 1) {
                    boolean shouldUppercase = (shiftPressed && !capsLockOn) || (!shiftPressed && capsLockOn);
                    if (shouldUppercase) {
                        output = key.toUpperCase();
                    } else {
                        output = key.toLowerCase();
                    }
                }

                if (shiftPressed) {
                    shiftPressed = false;
                    buildKeyboard();
                }

                listener.onKeyTyped(output);
            }
        }
    }

    public void renderText(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        // Not needed anymore - ButtonWidget handles text rendering
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
