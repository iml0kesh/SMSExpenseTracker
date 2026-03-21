package com.example.smsexpensetracker.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Prefs {

    private static final String FILE = "SET_prefs";
    private static final String KEY_SENDERS = "senders";
    private static final String KEY_BANKS   = "banks";
    private static final String PREFIX_BUDGET = "budget_";

    private static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // --- Senders ---

    public static Set<String> getSenders(Context ctx) {
        return new HashSet<>(sp(ctx).getStringSet(KEY_SENDERS, new HashSet<>()));
    }

    public static void saveSenders(Context ctx, Set<String> senders) {
        sp(ctx).edit().putStringSet(KEY_SENDERS, senders).apply();
    }

    public static void addSenders(Context ctx, Set<String> toAdd) {
        Set<String> current = getSenders(ctx);
        current.addAll(toAdd);
        saveSenders(ctx, current);
    }

    public static void removeSendersForBank(Context ctx, String bank) {
        Set<String> senders = getSenders(ctx);
        Set<String> kept = new HashSet<>();
        for (String s : senders) {
            if (!s.toUpperCase().contains(bank.toUpperCase())) kept.add(s);
        }
        saveSenders(ctx, kept);

        // Also remove from banks set
        Set<String> banks = getBanks(ctx);
        banks.remove(bank);
        saveBanks(ctx, banks);
    }

    // --- Banks ---

    public static Set<String> getBanks(Context ctx) {
        return new HashSet<>(sp(ctx).getStringSet(KEY_BANKS, new HashSet<>()));
    }

    public static void saveBanks(Context ctx, Set<String> banks) {
        sp(ctx).edit().putStringSet(KEY_BANKS, banks).apply();
    }

    public static void addBank(Context ctx, String bank) {
        Set<String> banks = getBanks(ctx);
        banks.add(bank);
        saveBanks(ctx, banks);
    }

    // --- Budgets ---

    public static void setBudget(Context ctx, String category, double amount) {
        sp(ctx).edit().putFloat(PREFIX_BUDGET + category, (float) amount).apply();
    }

    public static double getBudget(Context ctx, String category) {
        return sp(ctx).getFloat(PREFIX_BUDGET + category, 0f);
    }

    // --- First launch ---

    public static boolean isFirstLaunch(Context ctx) {
        return getSenders(ctx).isEmpty();
    }
}
