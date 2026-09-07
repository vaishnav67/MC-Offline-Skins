package com.customskin.offline.network;

import com.customskin.offline.client.ClientSkinManager;
import com.customskin.offline.skin.SkinData;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PacketSyncAllSkins implements IMessage {
    private Map<UUID, SkinData> skins;

    public PacketSyncAllSkins() {
        this.skins = new HashMap<>();
    }

    public PacketSyncAllSkins(Map<UUID, SkinData> skins) {
        this.skins = skins;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.skins = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            UUID id = new UUID(buf.readLong(), buf.readLong());
            String url = ByteBufUtils.readUTF8String(buf);
            String model = ByteBufUtils.readUTF8String(buf);
            skins.put(id, new SkinData(url, model));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(skins.size());
        for (Map.Entry<UUID, SkinData> entry : skins.entrySet()) {
            buf.writeLong(entry.getKey().getMostSignificantBits());
            buf.writeLong(entry.getKey().getLeastSignificantBits());
            ByteBufUtils.writeUTF8String(buf, entry.getValue().getUrl());
            ByteBufUtils.writeUTF8String(buf, entry.getValue().getModel());
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncAllSkins, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncAllSkins message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                message.skins.forEach((uuid, data) -> {
                    ClientSkinManager.applySkin(uuid, data.getUrl(), data.getModel());
                });
            });
            return null;
        }
    }
}
