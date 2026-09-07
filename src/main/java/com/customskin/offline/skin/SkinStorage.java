package com.customskin.offline.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinStorage {
    private static final SkinStorage INSTANCE = new SkinStorage();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<UUID, SkinData> skins = new ConcurrentHashMap<>();
    private File storageFile;

    public static SkinStorage get() {
        return INSTANCE;
    }

    public void load(MinecraftServer server) {
        skins.clear();
        File worldDir = server.getActiveAnvilConverter().getFile(server.getFolderName(), ".");
        this.storageFile = new File(worldDir, "custom_skins.json");

        if (storageFile.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(storageFile), StandardCharsets.UTF_8)) {
                Map<UUID, SkinData> loaded = GSON.fromJson(reader, new TypeToken<Map<UUID, SkinData>>(){}.getType());
                if (loaded != null) {
                    skins.putAll(loaded);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        if (storageFile == null) return;
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(storageFile), StandardCharsets.UTF_8)) {
            GSON.toJson(skins, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSkin(UUID uuid, SkinData data) {
        skins.put(uuid, data);
        save();
    }

    public void clearSkin(UUID uuid) {
        skins.remove(uuid);
        save();
    }

    public SkinData getSkin(UUID uuid) {
        return skins.get(uuid);
    }

    public Map<UUID, SkinData> getAllSkins() {
        return skins;
    }
}
