package com.aetherianartificer.townstead.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Set;

/**
 * Stable structural facts about MCA's Villager Editor so the editor mixins target
 * widgets and pages by identity, not by position or a single hardcoded page id.
 *
 * <p>MCA reorganized the editor between 7.7.x and the 1.21.1 line: the General page
 * gained voice tone/pitch sliders, and the old single {@code body}/{@code head} pages
 * became a "Character" subpage hub (hair moved to {@code hair_style}, face to
 * {@code eyes}, skin/size/breast stayed on {@code body}). Keying off these stable
 * identifiers makes the mixins work on both layouts without comparing version numbers
 * (which is unreliable anyway: {@code 1.21.1-SNAPSHOT} sorts below {@code 7.7.17}).
 */
public final class McaEditorCompat {
    private McaEditorCompat() {}

    /** Translation key of the General-page age control. Unchanged across MCA versions. */
    public static final String AGE_KEY = "gui.villager_editor.age";

    // Page ids that host each appearance section, spanning the old single-page layout
    // (7.7.x: "head" held both hair and face) and the new subpage hub.
    private static final Set<String> HAIR_PAGES = Set.of("head", "hair_style");
    private static final Set<String> FACE_PAGES = Set.of("head", "eyes");
    private static final Set<String> BODY_PAGES = Set.of("body");

    /** True for any page that carries MCA's hair controls. */
    public static boolean isHairPage(String page) {
        return HAIR_PAGES.contains(page);
    }

    /** True for any page that carries the {@code FACE} gene control (eyes/face). */
    public static boolean isFacePage(String page) {
        return FACE_PAGES.contains(page);
    }

    /** True for the page that carries skin/size/breast controls. */
    public static boolean isBodyPage(String page) {
        return BODY_PAGES.contains(page);
    }

    /**
     * The first slider on the screen whose label is the given translation key, or null.
     * MCA's {@code GeneSliderWidget} keeps the plain translatable label on
     * {@link AbstractSliderButton#getMessage()}, so this matches it across versions even
     * when the page carries several sliders.
     */
    public static AbstractSliderButton findSlider(Screen screen, String translationKey) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractSliderButton asb
                    && asb.getMessage().getContents() instanceof TranslatableContents tc
                    && translationKey.equals(tc.getKey())) {
                return asb;
            }
        }
        return null;
    }
}
