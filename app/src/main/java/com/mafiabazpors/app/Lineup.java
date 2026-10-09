package com.mafiabazpors.app;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Lineup {
    public String id;
    public String name;
    public int playerCount;
    public HashMap<String, Integer> counts;
    public long updatedAt;

    public Lineup(String id, String name, int playerCount, Map<String, Integer> counts) {
        this.id = id;
        this.name = name;
        this.playerCount = Math.max(1, playerCount);
        this.counts = new HashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                this.counts.put(entry.getKey(), entry.getValue());
            }
        }
        this.updatedAt = System.currentTimeMillis();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("name", name);
        object.put("playerCount", playerCount);
        object.put("updatedAt", updatedAt);
        JSONObject roleCounts = new JSONObject();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            roleCounts.put(entry.getKey(), entry.getValue());
        }
        object.put("counts", roleCounts);
        return object;
    }

    public static Lineup fromJson(JSONObject object) throws JSONException {
        HashMap<String, Integer> counts = new HashMap<>();
        JSONObject roleCounts = object.optJSONObject("counts");
        if (roleCounts != null) {
            Iterator<String> keys = roleCounts.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                int count = roleCounts.optInt(key, 0);
                if (count > 0) counts.put(key, count);
            }
        }
        Lineup lineup = new Lineup(object.optString("id", "lineup-" + System.nanoTime()),
                object.optString("name", "ترکیب بدون نام"),
                Math.max(1, object.optInt("playerCount", 10)), counts);
        lineup.updatedAt = object.optLong("updatedAt", System.currentTimeMillis());
        return lineup;
    }
}
