package com.commandgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class PasscodeScreen extends Screen {
    
    private static final String CORRECT_PASSCODE = "5599";
    private StringBuilder passcode = new StringBuilder();
    
    private int guiWidth = 140;
    private int guiHeight = 200;
    
    private TextFieldWidget passcodeDisplay;
    
    public PasscodeScreen() {
        super(Text.literal("Enter Passcode"));
    }
    
    @Override
    protected void init() {
        int x = (this.width - guiWidth) / 2;
        int y = (this.height - guiHeight) / 2 + 20;
        
        // Passcode display field (non-editable, numbers only)
        passcodeDisplay = new TextFieldWidget(this.textRenderer, x + 20, y + 25, 100, 16, Text.literal("Passcode"));
        passcodeDisplay.setMaxLength(4);
        passcodeDisplay.setEditable(false); // Not typeable
        passcodeDisplay.setText("____");
        this.addSelectableChild(passcodeDisplay);
        
        // Number buttons (3x3 grid + 0 button)
        int[][] numbers = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                final int num = numbers[row][col];
                int btnX = x + 20 + (col * 35);
                int btnY = y + 50 + (row * 30);
                
                this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(String.valueOf(num)),
                    button -> addDigit(num)
                ).dimensions(btnX, btnY, 30, 24).build());
            }
        }
        
        // 0 button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("0"),
            button -> addDigit(0)
        ).dimensions(x + 55, y + 140, 30, 24).build());
        
        // Clear button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Clear"),
            button -> {
                passcode.setLength(0);
                updateDisplay();
            }
        ).dimensions(x + 15, y + 170, 55, 20).build());
        
        // Submit button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Enter"),
            button -> checkPasscode()
        ).dimensions(x + 75, y + 170, 55, 20).build());
    }
    
    private void addDigit(int digit) {
        if (passcode.length() < 4) {
            passcode.append(digit);
            updateDisplay();
        }
    }
    
    private void updateDisplay() {
        if (passcodeDisplay != null) {
            String display = "*".repeat(passcode.length()) + "_".repeat(4 - passcode.length());
            passcodeDisplay.setText(display);
        }
    }
    
    private void checkPasscode() {
        if (passcode.toString().equals(CORRECT_PASSCODE)) {
            if (this.client != null) {
                this.client.setScreen(new CommandExecutionScreen());
            }
        } else {
            passcode.setLength(0);
            updateDisplay();
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int x = (this.width - guiWidth) / 2;
        int y = (this.height - guiHeight) / 2 + 20;
        
        // Draw GUI background panel
        context.fill(x, y, x + guiWidth, y + guiHeight, 0xFFC6C6C6);
        
        // Draw border
        context.fill(x - 2, y - 2, x + guiWidth + 2, y, 0xFF373737);
        context.fill(x - 2, y + guiHeight, x + guiWidth + 2, y + guiHeight + 2, 0xFF373737);
        context.fill(x - 2, y, x, y + guiHeight, 0xFF373737);
        context.fill(x + guiWidth, y, x + guiWidth + 2, y + guiHeight, 0xFF373737);
        
        // Render widgets FIRST
        super.render(context, mouseX, mouseY, delta);
        
        // Draw title AFTER (with shadow = true)
        String titleText = "Enter Passcode";
        int titleWidth = this.textRenderer.getWidth(titleText);
        context.drawText(this.textRenderer, titleText, x + (guiWidth - titleWidth) / 2, y + 8, 0x404040, true);
        
        // Render passcode display field
        passcodeDisplay.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}

/*
 * CUSTOMIZATION GUIDE:
 * 
 * GUI SIZE:
 * - guiWidth = 140  (change to make wider/narrower)
 * - guiHeight = 200 (change to make taller/shorter)
 * 
 * BUTTON POSITION:
 * - x coordinate: x + [offset]
 * - y coordinate: y + [offset]
 * Example: Move Clear button right 10px: change "x + 15" to "x + 25"
 * 
 * BUTTON SIZE:
 * - .dimensions(x, y, [width], [height])
 * Example: Make buttons bigger: change (30, 24) to (40, 30)
 * 
 * NUMBER BUTTON GRID SPACING:
 * - Horizontal: (col * 35) - change 35 to adjust spacing
 * - Vertical: (row * 30) - change 30 to adjust spacing
 */