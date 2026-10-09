package com.mafiabazpors.app;

import org.json.JSONException;
import org.json.JSONObject;

public final class Role {
    public String id;
    public String name;
    public String faction;
    public String category;
    public String icon;
    public String shortDescription;
    public String ability;
    public String winCondition;
    public int suggestedCount;
    public boolean enabled;
    public boolean repeatAllowed;
    /** Optional content URI chosen by the user; built-in portraits are bundled locally. */
    public String imageUri;

    public Role(String id, String name, String faction, String category, String icon,
                String shortDescription, String ability, String winCondition,
                int suggestedCount, boolean enabled, boolean repeatAllowed) {
        this.id = id;
        this.name = name;
        this.faction = faction;
        this.category = category;
        this.icon = icon;
        this.shortDescription = shortDescription;
        this.ability = ability;
        this.winCondition = winCondition;
        this.suggestedCount = Math.max(0, suggestedCount);
        this.enabled = enabled;
        this.repeatAllowed = repeatAllowed;
    }

    public Role copy() {
        Role copied = new Role(id, name, faction, category, icon, shortDescription, ability,
                winCondition, suggestedCount, enabled, repeatAllowed);
        copied.imageUri = imageUri;
        return copied;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("name", name);
        object.put("faction", faction);
        object.put("category", category);
        object.put("icon", icon);
        object.put("shortDescription", shortDescription);
        object.put("ability", ability);
        object.put("winCondition", winCondition);
        object.put("suggestedCount", suggestedCount);
        object.put("enabled", enabled);
        object.put("repeatAllowed", repeatAllowed);
        if (imageUri != null && !imageUri.trim().isEmpty()) object.put("imageUri", imageUri);
        return object;
    }

    public static Role fromJson(JSONObject object) throws JSONException {
        Role role = new Role(
                object.optString("id", "role-" + System.nanoTime()),
                object.optString("name", "نقش بدون نام"),
                object.optString("faction", "قابل تنظیم"),
                object.optString("category", "عمومی"),
                object.optString("icon", "◆"),
                object.optString("shortDescription", ""),
                object.optString("ability", "قابلیت توسط مدیر تعیین نشده است."),
                object.optString("winCondition", "شرایط پیروزی توسط مدیر تعیین نشده است."),
                Math.max(0, object.optInt("suggestedCount", 0)),
                object.optBoolean("enabled", true),
                object.optBoolean("repeatAllowed", true));
        role.imageUri = object.optString("imageUri", null);
        return role;
    }

}
