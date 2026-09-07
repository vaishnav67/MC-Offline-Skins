package com.customskin.offline.client;

import com.customskin.offline.skin.SkinData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientEventListener {
    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null) return;

        SkinData data = ClientSkinManager.getSkin(player.getUniqueID());
        if (data != null && data.getResourceLocation() != null) {
            NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
            if (connection != null) {
                NetworkPlayerInfo npi = connection.getPlayerInfo(player.getUniqueID());
                if (npi != null && !data.getResourceLocation().equals(npi.getLocationSkin())) {
                    ClientSkinManager.updateNetworkPlayerInfo(player.getUniqueID(), data);
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientSkinManager.removeSkin(Minecraft.getMinecraft().player.getUniqueID());
    }
}
