package com.customskin.offline;

import com.customskin.offline.network.NetworkHandler;
import com.customskin.offline.network.PacketSyncAllSkins;
import com.customskin.offline.skin.SkinStorage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            NetworkHandler.sendTo(new PacketSyncAllSkins(SkinStorage.get().getAllSkins()), player);
        }
    }
}
