package com.customskin.offline.skin;

import com.customskin.offline.OfflineSkins;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MojangSkinFetcher {
    private static final JsonParser PARSER = new JsonParser();

    public static SkinData resolve(String input, String modelOverride) throws Exception {
        input = input.trim();

        // Direct URL
        if (input.startsWith("http://") || input.startsWith("https://")) {
            String cleanUrl = input.replace("http://", "https://");
            String model = (modelOverride != null) ? modelOverride.trim().toLowerCase() : "default";
            return new SkinData(cleanUrl, model);
        }

        // 1. Try official Mojang API
        try {
            return fetchFromMojang(input, modelOverride);
        } catch (Exception mojangEx) {
            OfflineSkins.LOGGER.warn("Mojang API lookup failed for '{}' ({}). Trying fallback API...", input, mojangEx.getMessage());
            // 2. Fallback to Ashcon API
            return fetchFromAshcon(input, modelOverride);
        }
    }

    private static SkinData fetchFromMojang(String username, String modelOverride) throws Exception {
        String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + username;
        JsonObject uuidObj = getJson(uuidUrl);
        if (uuidObj == null || !uuidObj.has("id")) {
            throw new IllegalArgumentException("Username not found on Mojang: " + username);
        }
        String trimmedUuid = uuidObj.get("id").getAsString();

        String sessionUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + trimmedUuid;
        JsonObject sessionObj = getJson(sessionUrl);
        if (sessionObj == null || !sessionObj.has("properties")) {
            throw new IllegalArgumentException("Textures unavailable on Mojang session server.");
        }

        JsonArray properties = sessionObj.getAsJsonArray("properties");
        String base64Textures = null;
        for (int i = 0; i < properties.size(); i++) {
            JsonObject prop = properties.get(i).getAsJsonObject();
            if ("textures".equals(prop.get("name").getAsString())) {
                base64Textures = prop.get("value").getAsString();
                break;
            }
        }

        if (base64Textures == null) {
            throw new IllegalArgumentException("No textures property found.");
        }

        String decodedJson = new String(Base64.getDecoder().decode(base64Textures), StandardCharsets.UTF_8);
        JsonObject root = PARSER.parse(decodedJson).getAsJsonObject();
        JsonObject textures = root.getAsJsonObject("textures");
        if (!textures.has("SKIN")) {
            throw new IllegalArgumentException("User does not have a skin.");
        }

        JsonObject skinObj = textures.getAsJsonObject("SKIN");
        String skinUrl = skinObj.get("url").getAsString().replace("http://", "https://");

        String model = "default";
        if (modelOverride != null) {
            model = modelOverride.trim().toLowerCase();
        } else if (skinObj.has("metadata")) {
            JsonObject meta = skinObj.getAsJsonObject("metadata");
            if (meta.has("model") && "slim".equalsIgnoreCase(meta.get("model").getAsString())) {
                model = "slim";
            }
        }

        return new SkinData(skinUrl, model);
    }

    private static SkinData fetchFromAshcon(String username, String modelOverride) throws Exception {
        String url = "https://api.ashcon.app/mojang/v2/user/" + username;
        JsonObject obj = getJson(url);
        if (obj == null || !obj.has("textures")) {
            throw new IllegalArgumentException("Failed to find skin for '" + username + "' via fallback API.");
        }

        JsonObject textures = obj.getAsJsonObject("textures");
        JsonObject skin = textures.getAsJsonObject("skin");
        String skinUrl = skin.get("url").getAsString().replace("http://", "https://");

        String model = "default";
        if (modelOverride != null) {
            model = modelOverride.trim().toLowerCase();
        } else if (textures.has("slim") && textures.get("slim").getAsBoolean()) {
            model = "slim";
        }

        return new SkinData(skinUrl, model);
    }

    private static JsonObject getJson(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("HTTP code " + conn.getResponseCode());
            }
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                return PARSER.parse(reader).getAsJsonObject();
            }
        } finally {
            conn.disconnect();
        }
    }
}