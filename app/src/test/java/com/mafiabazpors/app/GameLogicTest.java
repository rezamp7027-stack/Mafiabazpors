package com.mafiabazpors.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class GameLogicTest {
    private Role citizen() {
        return new Role("citizen", "شهروند", "شهروندی", "پایه", "●", "", "", "", 0, true, true);
    }

    private Role mafia() {
        return new Role("mafia", "مافیا", "مافیایی", "پایه", "◆", "", "", "", 0, true, true);
    }

    @Test public void exactDeckCountsArePreservedWithRepeatedRoles() {
        List<Role> bank = Arrays.asList(citizen(), mafia());
        Map<String, Integer> counts = new HashMap<>();
        counts.put("citizen", 7);
        counts.put("mafia", 3);
        List<Role> deck = GameLogic.createDeck(bank, counts);
        assertEquals(10, deck.size());
        assertEquals(7, deck.stream().filter(r -> r.id.equals("citizen")).count());
        assertEquals(3, deck.stream().filter(r -> r.id.equals("mafia")).count());
    }

    @Test public void sizeValidationRejectsMismatchedCombinations() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("citizen", 5);
        counts.put("mafia", 2);
        assertTrue(GameLogic.hasSameSize(7, counts));
        assertFalse(GameLogic.hasSameSize(8, counts));
    }

    @Test public void deckContainsNoNullCardsAndDoesNotMutateCounts() {
        List<Role> bank = Arrays.asList(citizen(), mafia());
        Map<String, Integer> counts = new HashMap<>();
        counts.put("citizen", 3);
        counts.put("mafia", 2);
        List<Role> deck = GameLogic.createDeck(bank, counts);
        Set<String> ids = new HashSet<>();
        for (Role role : deck) {
            assertNotNull(role);
            ids.add(role.id);
        }
        assertEquals(new HashSet<>(Arrays.asList("citizen", "mafia")), ids);
        assertEquals(Integer.valueOf(3), counts.get("citizen"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deletedRolesCannotRemainInDeck() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("gone", 1);
        GameLogic.createDeck(new ArrayList<>(), counts);
    }
}
