package com.commandgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class CommandExecutionScreen extends Screen {

    private int guiWidth = 176;
    private int guiHeight = 100;

    private TextFieldWidget commandField;
    private TextFieldWidget valueField;
    private VirtualKeyboard virtualKeyboard;

    public CommandExecutionScreen() {
        super(Text.literal("Command Executor"));
    }

    @Override
    protected void init() {
        int x = (this.width - guiWidth) / 2;
        int y = (this.height - guiHeight) / 2;

        // Command field
        commandField = new TextFieldWidget(this.textRenderer, x + 8, y + 20, 160, 16, Text.literal("Command"));
        commandField.setMaxLength(256);
        commandField.setPlaceholder(Text.literal("e.g., gamerule keepInventory"));
        this.addSelectableChild(commandField);
        this.setInitialFocus(commandField);

        // Value field
        valueField = new TextFieldWidget(this.textRenderer, x + 8, y + 42, 160, 16, Text.literal("Value"));
        valueField.setMaxLength(256);
        valueField.setPlaceholder(Text.literal("e.g., true or 64"));
        this.addSelectableChild(valueField);

        // Confirm button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Confirm"),
            button -> executeCommand()
        ).dimensions(x + 8, y + 76, 78, 20).build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Back"),
            button -> {
                if (this.client != null) {
                    this.client.setScreen(new PasscodeScreen());
                }
            }
        ).dimensions(x + 90, y + 76, 78, 20).build());

        // Initialize virtual keyboard (310px wide now)
        int keyboardX = (this.width - 310) / 2;
        int keyboardY = y + guiHeight + 10;

        virtualKeyboard = new VirtualKeyboard(keyboardX, keyboardY, new VirtualKeyboard.KeyboardListener() {
            @Override
            public void onKeyTyped(String key) {
                TextFieldWidget focused = getFocusedTextField();
                if (focused != null) {
                    focused.write(key);
                }
            }

            @Override
            public void onBackspace() {
                TextFieldWidget focused = getFocusedTextField();
                if (focused != null && !focused.getText().isEmpty()) {
                    int cursor = focused.getCursor();
                    if (cursor > 0) {
                        String text = focused.getText();
                        String newText = text.substring(0, cursor - 1) + text.substring(cursor);
                        focused.setText(newText);
                        focused.setCursor(cursor - 1, false);
                    }
                }
            }

            @Override
            public void onEnter() {
                executeCommand();
            }
        }, this);
    }

    private TextFieldWidget getFocusedTextField() {
        if (commandField.isFocused()) return commandField;
        if (valueField.isFocused()) return valueField;
        return null;
    }

    private void executeCommand() {
        String command = commandField.getText().trim();
        String value = valueField.getText().trim();

        if (command.isEmpty()) {
            return;
        }

        CommandPackets.sendCommandPacket(command, value);

        commandField.setText("");
        valueField.setText("");

        if (this.client != null && this.client.player != null) {
            String fullCommand = command;
            if (!value.isEmpty()) {
                fullCommand += " " + value;
            }
            this.client.player.sendMessage(Text.literal("§aCommand sent to server: /" + fullCommand), false);
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Update keyboard for key repeat
        if (virtualKeyboard != null) {
            virtualKeyboard.tick();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int x = (this.width - guiWidth) / 2;
        int y = (this.height - guiHeight) / 2;

        // Draw GUI background panel
        context.fill(x, y, x + guiWidth, y + guiHeight, 0xFFC6C6C6);

        // Draw border
        context.fill(x - 2, y - 2, x + guiWidth + 2, y, 0xFF373737);
        context.fill(x - 2, y + guiHeight, x + guiWidth + 2, y + guiHeight + 2, 0xFF373737);
        context.fill(x - 2, y, x, y + guiHeight, 0xFF373737);
        context.fill(x + guiWidth, y, x + guiWidth + 2, y + guiHeight, 0xFF373737);

        // Render widgets FIRST
        super.render(context, mouseX, mouseY, delta);

        // Draw title AFTER
        context.drawText(this.textRenderer, "Command Executor", x + 8, y + 6, 0x404040, true);

        // Draw text fields
        commandField.render(context, mouseX, mouseY, delta);
        valueField.render(context, mouseX, mouseY, delta);

        // Render virtual keyboard
        if (virtualKeyboard != null) {
            virtualKeyboard.render(context, mouseX, mouseY);
            virtualKeyboard.renderButtons(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (virtualKeyboard != null && virtualKeyboard.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        commandField.mouseClicked(mouseX, mouseY, button);
        valueField.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (virtualKeyboard != null && virtualKeyboard.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (virtualKeyboard != null && virtualKeyboard.mouseDragged(mouseX, mouseY, button, deltaX, deltaY, this.width, this.height)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (commandField.isFocused() || valueField.isFocused()) {
            if (commandField.keyPressed(keyInput) || 
                valueField.keyPressed(keyInput)) {
                return true;
            }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (commandField.isFocused()) {
            return commandField.charTyped(charInput);
        }
        if (valueField.isFocused()) {
            return valueField.charTyped(charInput);
        }
        return super.charTyped(charInput);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}