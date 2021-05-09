package me.jellysquid.mods.sodium.client.gui;

import me.jellysquid.mods.sodium.client.gui.options.OptionPage;

import java.util.ArrayList;
import java.util.List;

public class CustomOptions {
    private static final List<OptionPage> pages = new ArrayList<>();

    // Should be called in the onInitializeClient method for each mod
    public static void addPage(OptionPage page) {
        pages.add(page);
    }

    public static List<OptionPage> getPages() {
        return pages;
    }
}