package com.commandgui.mixin;

import com.commandgui.PasscodeScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {
    
    @Unique
    private ButtonWidget commandGuiButton;
    
    public InventoryScreenMixin() {
        super(null, null, null);
    }
    
    @Inject(method = "init", at = @At("TAIL"))
    private void addCommandButton(CallbackInfo ci) {
        // Calculate position: below hotbar slot 5 (center-bottom of inventory)
        int backgroundX = this.x;
        int backgroundY = this.y;
        
        // Position button below hotbar slot 5
        int buttonX = backgroundX + 80 - 15;
        int buttonY = backgroundY + 162;
        
        commandGuiButton = ButtonWidget.builder(
            Text.literal("CMD"),
            button -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.setScreen(new PasscodeScreen());
                }
            }
        ).dimensions(buttonX, buttonY, 30, 12).build();
        
        this.addDrawableChild(commandGuiButton);
    }
    
    @Inject(method = "render", at = @At("TAIL"))
    private void renderCommandButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (commandGuiButton != null && commandGuiButton.isHovered()) {
            context.drawTooltip(this.textRenderer, Text.literal("Open Command GUI"), mouseX, mouseY);
        }
    }
}