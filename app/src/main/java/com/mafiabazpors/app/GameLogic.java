package com.mafiabazpors.app;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure deal logic, isolated so it can be tested independently from Android UI. */
public final class GameLogic {
    public static final int SAFE_CARD_LIMIT = 100_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GameLogic() {}

    public static long countCards(Map<String, Integer> counts) {
        long total = 0;
        for (Integer count : counts.values()) {
            if (count != null && count > 0) total += count;
        }
        return total;
    }

    public static List<Role> createDeck(List<Role> roleBank, Map<String, Integer> counts) {
        HashMap<String, Role> byId = new HashMap<>();
        for (Role role : roleBank) byId.put(role.id, role);
        long total = countCards(counts);
        if (total > SAFE_CARD_LIMIT) {
            throw new IllegalArgumentException("تعداد کارت‌ها از حد ایمن حافظهٔ این نسخه بیشتر است.");
        }
        ArrayList<Role> deck = new ArrayList<>((int) total);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            int count = entry.getValue() == null ? 0 : entry.getValue();
            if (count < 0) throw new IllegalArgumentException("تعداد نقش نمی‌تواند منفی باشد.");
            if (count == 0) continue;
            Role role = byId.get(entry.getKey());
            if (role == null || !role.enabled) {
                throw new IllegalArgumentException("یکی از نقش‌های انتخاب‌شده حذف یا غیرفعال شده است.");
            }
            for (int i = 0; i < count; i++) deck.add(role.copy());
        }
        return deck;
    }

    public static void secureShuffle(List<Role> deck) {
        for (int i = deck.size() - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            Collections.swap(deck, i, j);
        }
    }

    public static boolean hasSameSize(int players, Map<String, Integer> counts) {
        return players > 0 && players <= SAFE_CARD_LIMIT && countCards(counts) == players;
    }
}
