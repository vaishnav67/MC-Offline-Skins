package com.customskin.offline.network;

import com.customskin.offline.client.ClientSkinManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;

import java.util.UUID;

public class PacketSyncSkin implements IMessage {
    private UUID uuid;
    private String url;
    private String model;

    public PacketSyncSkin() {}

    public PacketSyncSkin(UUID uuid, String url, String model) {
        this.uuid = uuid;
        this.url = url;
        this.model = model;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.uuid = new UUID(buf.readLong(), buf.readLong());
        this.url = ByteBufUtils.readUTF8String(buf);
        this.model = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        ByteBufUtils.writeUTF8String(buf, url);
        ByteBufUtils.writeUTF8String(buf, model);
    }

    public static class Handler implements IMessageHandler<PacketSyncSkin, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncSkin message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientSkinManager.applySkin(message.uuid, message.url, message.model);
            });
            return null;
        }
    }
}
