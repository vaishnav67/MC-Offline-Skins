package com.customskin.offline.network;

import com.customskin.offline.client.ClientSkinManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.*;

import java.util.UUID;

public class PacketResetSkin implements IMessage {
    private UUID uuid;

    public PacketResetSkin() {}

    public PacketResetSkin(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.uuid = new UUID(buf.readLong(), buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static class Handler implements IMessageHandler<PacketResetSkin, IMessage> {
        @Override
        public IMessage onMessage(PacketResetSkin message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientSkinManager.removeSkin(message.uuid);
            });
            return null;
        }
    }
}
