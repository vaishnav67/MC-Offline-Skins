package com.customskin.offline.client;

import com.customskin.offline.OfflineSkins;
import com.customskin.offline.skin.SkinData;
import com.google.common.hash.Hashing;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public class ClientSkinManager {
    private static final Map<UUID, SkinData> CLIENT_SKIN_CACHE = new ConcurrentHashMap<>();

    // Cached Reflection Fields for 0-overhead lookups
    private static Field fieldPlayerTextures;
    private static Field fieldSkinType;
    private static Field fieldPlayerTexturesLoaded;
    private static boolean fieldsInitialized = false;

    private static synchronized void initReflectionFields() {
        if (fieldsInitialized) return;
        try {
            for (Field f : NetworkPlayerInfo.class.getDeclaredFields()) {
                f.setAccessible(true);
                if (Map.class.isAssignableFrom(f.getType())) {
                    fieldPlayerTextures = f;
                } else if (f.getType() == String.class) {
                    fieldSkinType = f;
                } else if (f.getType() == boolean.class) {
                    fieldPlayerTexturesLoaded = f;
                }
            }
            fieldsInitialized = true;
        } catch (Exception e) {
            OfflineSkins.LOGGER.error("Failed to initialize NetworkPlayerInfo reflection fields", e);
        }
    }

    public static File getSkinCacheDir() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "cached_skins");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static void applySkin(UUID uuid, String url, String model) {
        try {
            File cacheDir = getSkinCacheDir();
            String hash = Hashing.sha1().hashString(url, StandardCharsets.UTF_8).toString();
            File cacheFile = new File(cacheDir, hash + ".png");
            ResourceLocation resLoc = new ResourceLocation("offlineskins", hash);

            TextureManager tm = Minecraft.getMinecraft().getTextureManager();
            ITextureObject existing = tm.getTexture(resLoc);
            if (existing == null) {
                ThreadDownloadImageData downloadData = new ThreadDownloadImageData(
                        cacheFile,
                        url,
                        DefaultPlayerSkin.getDefaultSkin(uuid),
                        new ImageBufferDownload()
                );
                tm.loadTexture(resLoc, downloadData);
            }

            SkinData data = new SkinData(url, model);
            data.setResourceLocation(resLoc);
            CLIENT_SKIN_CACHE.put(uuid, data);

            updateNetworkPlayerInfo(uuid, data);
        } catch (Exception e) {
            OfflineSkins.LOGGER.error("Error applying skin for UUID " + uuid, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void updateNetworkPlayerInfo(UUID uuid, SkinData data) {
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        if (connection == null) return;

        NetworkPlayerInfo npi = connection.getPlayerInfo(uuid);
        if (npi == null || data.getResourceLocation() == null) return;

        initReflectionFields();

        try {
            if (fieldPlayerTextures != null) {
                Map<MinecraftProfileTexture.Type, ResourceLocation> map =
                        (Map<MinecraftProfileTexture.Type, ResourceLocation>) fieldPlayerTextures.get(npi);
                if (map != null) {
                    map.put(MinecraftProfileTexture.Type.SKIN, data.getResourceLocation());
                }
            }
            if (fieldSkinType != null) {
                fieldSkinType.set(npi, data.getModel());
            }
            if (fieldPlayerTexturesLoaded != null) {
                fieldPlayerTexturesLoaded.setBoolean(npi, true);
            }
        } catch (Exception e) {
            OfflineSkins.LOGGER.error("Failed to update NetworkPlayerInfo for " + uuid, e);
        }
    }

    public static void removeSkin(UUID uuid) {
        CLIENT_SKIN_CACHE.remove(uuid);
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        if (connection == null) return;

        NetworkPlayerInfo npi = connection.getPlayerInfo(uuid);
        if (npi == null) return;

        initReflectionFields();

        try {
            if (fieldPlayerTextures != null) {
                Map<?, ?> map = (Map<?, ?>) fieldPlayerTextures.get(npi);
                if (map != null) {
                    map.remove(MinecraftProfileTexture.Type.SKIN);
                }
            }
            if (fieldSkinType != null) {
                fieldSkinType.set(npi, DefaultPlayerSkin.getSkinType(uuid));
            }
            if (fieldPlayerTexturesLoaded != null) {
                fieldPlayerTexturesLoaded.setBoolean(npi, false);
            }
        } catch (Exception e) {
            OfflineSkins.LOGGER.error("Failed to reset NetworkPlayerInfo for " + uuid, e);
        }
    }

    public static SkinData getSkin(UUID uuid) {
        return CLIENT_SKIN_CACHE.get(uuid);
    }
}