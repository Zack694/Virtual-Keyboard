package com.commandgui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class CommandPackets {
    public static final Identifier EXECUTE_COMMAND_ID = Identifier.of("commandgui", "execute_command");
    
    public record ExecuteCommandPayload(String command, String value) implements CustomPayload {
        public static final Id<ExecuteCommandPayload> ID = new Id<>(EXECUTE_COMMAND_ID);
        public static final PacketCodec<RegistryByteBuf, ExecuteCommandPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.command);
                buf.writeString(value.value);
            },
            buf -> new ExecuteCommandPayload(buf.readString(), buf.readString())
        );
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    
    public static void sendCommandPacket(String command, String value) {
        ClientPlayNetworking.send(new ExecuteCommandPayload(command, value != null ? value : ""));
    }
}