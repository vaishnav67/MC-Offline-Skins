package com.customskin.offline;

import com.customskin.offline.command.CommandSkin;
import com.customskin.offline.network.NetworkHandler;
import com.customskin.offline.skin.SkinStorage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.Logger;

@Mod(modid = OfflineSkins.MODID, name = OfflineSkins.NAME, version = OfflineSkins.VERSION, acceptableRemoteVersions = "*")
public class OfflineSkins {
    public static final String MODID = "offlineskins";
    public static final String NAME = "Offline Skins";
    public static final String VERSION = "@VERSION@";

    public static Logger LOGGER;

    @Mod.Instance
    public static OfflineSkins instance;

    @SidedProxy(
            clientSide = "com.customskin.offline.ClientProxy",
            serverSide = "com.customskin.offline.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        NetworkHandler.init();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandSkin());
        SkinStorage.get().load(event.getServer());
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        SkinStorage.get().save();
    }
}