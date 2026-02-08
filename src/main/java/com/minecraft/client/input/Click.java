package net.minecraft.client.input;

/**
 * Temporary Click record for 1.21.11 compatibility
 * This should match Minecraft's Click record structure
 */
public record Click(double mouseX, double mouseY, int button) {
}