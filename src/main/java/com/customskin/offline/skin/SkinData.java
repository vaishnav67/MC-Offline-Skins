package com.customskin.offline.skin;

import net.minecraft.util.ResourceLocation;

public class SkinData {
    private String url;
    private String model;
    private transient ResourceLocation resourceLocation;

    public SkinData(String url, String model) {
        this.url = url;
        this.model = (model == null || model.equalsIgnoreCase("alex") || model.equalsIgnoreCase("slim")) ? "slim" : "default";
    }

    public String getUrl() { return url; }
    public String getModel() { return model; }
    public ResourceLocation getResourceLocation() { return resourceLocation; }
    public void setResourceLocation(ResourceLocation loc) { this.resourceLocation = loc; }
}
