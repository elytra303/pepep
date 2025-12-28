package rich.screens.clickgui.impl.autobuy.items;

import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.defaults.MiscProvider;
import rich.screens.clickgui.impl.autobuy.originalitems.DonatorProvider;
import rich.screens.clickgui.impl.autobuy.originalitems.PotionProvider;
import rich.screens.clickgui.impl.autobuy.originalitems.SphereProvider;
import rich.screens.clickgui.impl.autobuy.originalitems.TalismanProvider;
import rich.screens.clickgui.impl.autobuy.settings.AutoBuySettingsManager;
import rich.screens.clickgui.impl.autobuy.util.KrushProvider;

import java.util.ArrayList;
import java.util.List;

public class ItemRegistry {
    private static List<AutoBuyableItem> allItems = null;
    private static List<AutoBuyableItem> krushItems = null;
    private static List<AutoBuyableItem> talismanItems = null;
    private static List<AutoBuyableItem> sphereItems = null;
    private static List<AutoBuyableItem> miscItems = null;
    private static List<AutoBuyableItem> donatorItems = null;
    private static List<AutoBuyableItem> potionItems = null;
    private static boolean initialized = false;

    public static void ensureSettingsLoaded() {
        if (!initialized) {
            getAllItems();
            initialized = true;
        }
    }

    public static List<AutoBuyableItem> getAllItems() {
        if (allItems == null) {
            allItems = new ArrayList<>();
            allItems.addAll(getKrush());
            allItems.addAll(getTalismans());
            allItems.addAll(getSpheres());
            allItems.addAll(getMisc());
            allItems.addAll(getDonator());
            allItems.addAll(getPotions());
            loadAllSettings();
        }
        return allItems;
    }

    private static void loadAllSettings() {
        AutoBuySettingsManager manager = AutoBuySettingsManager.getInstance();
        for (AutoBuyableItem item : allItems) {
            manager.loadSettings(item.getDisplayName(), item.getSettings());
            if (manager.hasEnabledState(item.getDisplayName())) {
                item.setEnabled(manager.getEnabledState(item.getDisplayName()));
            }
        }
    }

    public static void reloadSettings() {
        if (allItems != null) {
            loadAllSettings();
        }
    }

    public static void saveItemState(AutoBuyableItem item) {
        item.setEnabled(!item.isEnabled());
        AutoBuySettingsManager manager = AutoBuySettingsManager.getInstance();
        manager.saveSettings(item.getDisplayName(), item.getSettings());
        manager.saveEnabledState(item.getDisplayName(), item.isEnabled());
    }

    public static void saveItemSettings(AutoBuyableItem item) {
        AutoBuySettingsManager manager = AutoBuySettingsManager.getInstance();
        manager.saveSettings(item.getDisplayName(), item.getSettings());
        manager.saveEnabledState(item.getDisplayName(), item.isEnabled());
    }

    public static List<AutoBuyableItem> getKrush() {
        if (krushItems == null) {
            krushItems = KrushProvider.getKrush();
        }
        return krushItems;
    }

    public static List<AutoBuyableItem> getTalismans() {
        if (talismanItems == null) {
            talismanItems = TalismanProvider.getTalismans();
        }
        return talismanItems;
    }

    public static List<AutoBuyableItem> getSpheres() {
        if (sphereItems == null) {
            sphereItems = SphereProvider.getSpheres();
        }
        return sphereItems;
    }

    public static List<AutoBuyableItem> getMisc() {
        if (miscItems == null) {
            miscItems = MiscProvider.getMisc();
        }
        return miscItems;
    }

    public static List<AutoBuyableItem> getDonator() {
        if (donatorItems == null) {
            donatorItems = DonatorProvider.getDonator();
        }
        return donatorItems;
    }

    public static List<AutoBuyableItem> getPotions() {
        if (potionItems == null) {
            potionItems = PotionProvider.getPotions();
        }
        return potionItems;
    }

    public static void clearCache() {
        allItems = null;
        krushItems = null;
        talismanItems = null;
        sphereItems = null;
        miscItems = null;
        donatorItems = null;
        potionItems = null;
        initialized = false;
    }
}