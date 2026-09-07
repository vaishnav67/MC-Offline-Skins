package com.customskin.offline.network;

import com.customskin.offline.OfflineSkins;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(OfflineSkins.MODID);
    private static int packetId = 0;

    public static void init() {
        INSTANCE.registerMessage(PacketSyncSkin.Handler.class, PacketSyncSkin.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(PacketSyncAllSkins.Handler.class, PacketSyncAllSkins.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(PacketResetSkin.Handler.class, PacketResetSkin.class, packetId++, Side.CLIENT);
    }

    public static void sendToAll(IMessage message) {
        INSTANCE.sendToAll(message);
    }

    public static void sendTo(IMessage message, EntityPlayerMP player) {
        INSTANCE.sendTo(message, player);
    }
}
