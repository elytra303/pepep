package rich.screens.clickgui.impl.autobuy;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuctionUtils {
    public static final Pattern funTimePricePattern = Pattern.compile("\\$(\\d+(?:[\\s,]\\d{3})*(?:\\.\\d{2})?)");
    private static final Pattern digitPattern = Pattern.compile("(\\d[\\d\\s,.]*)");

    public static int getPrice(ItemStack stack) {
        String priceStr = null;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            for (Text line : lore.lines()) {
                String lineStr = line.getString();
                if (lineStr.contains("$") || lineStr.toLowerCase().contains("цена")) {
                    Matcher matcher = funTimePricePattern.matcher(lineStr);
                    if (matcher.find()) {
                        priceStr = matcher.group(1);
                        break;
                    }
                    matcher = digitPattern.matcher(lineStr);
                    if (matcher.find()) {
                        priceStr = matcher.group(1);
                        break;
                    }
                }
            }
        }
        if (priceStr == null || priceStr.isEmpty()) {
            String itemName = stack.getName().getString();
            if (itemName != null) {
                Matcher matcher = funTimePricePattern.matcher(itemName);
                if (matcher.find()) {
                    priceStr = matcher.group(1);
                }
            }
        }
        if (priceStr == null || priceStr.isEmpty()) {
            var components = stack.getComponents();
            if (components != null) {
                String componentString = components.toString();
                if (componentString.contains("$")) {
                    Matcher matcher = funTimePricePattern.matcher(componentString);
                    if (matcher.find()) {
                        priceStr = matcher.group(1);
                    }
                }
            }
        }
        if (priceStr == null || priceStr.isEmpty()) return -1;
        try {
            priceStr = priceStr.replaceAll("[\\s,.$]", "").trim();
            if (priceStr.isEmpty()) return -1;
            return Integer.parseInt(priceStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String cleanString(String str) {
        if (str == null) return "";
        return str.toLowerCase().trim()
                .replaceAll("§.", "")
                .replaceAll("[^a-zа-яё0-9\\s\\[\\]★⚒+]", "")
                .replaceAll("\\s+", " ");
    }

    public static boolean isArmorItem(ItemStack stack) {
        return stack.getItem() == Items.NETHERITE_HELMET ||
                stack.getItem() == Items.NETHERITE_CHESTPLATE ||
                stack.getItem() == Items.NETHERITE_LEGGINGS ||
                stack.getItem() == Items.NETHERITE_BOOTS ||
                stack.getItem() == Items.DIAMOND_HELMET ||
                stack.getItem() == Items.DIAMOND_CHESTPLATE ||
                stack.getItem() == Items.DIAMOND_LEGGINGS ||
                stack.getItem() == Items.DIAMOND_BOOTS ||
                stack.getItem() == Items.IRON_HELMET ||
                stack.getItem() == Items.IRON_CHESTPLATE ||
                stack.getItem() == Items.IRON_LEGGINGS ||
                stack.getItem() == Items.IRON_BOOTS ||
                stack.getItem() == Items.GOLDEN_HELMET ||
                stack.getItem() == Items.GOLDEN_CHESTPLATE ||
                stack.getItem() == Items.GOLDEN_LEGGINGS ||
                stack.getItem() == Items.GOLDEN_BOOTS ||
                stack.getItem() == Items.CHAINMAIL_HELMET ||
                stack.getItem() == Items.CHAINMAIL_CHESTPLATE ||
                stack.getItem() == Items.CHAINMAIL_LEGGINGS ||
                stack.getItem() == Items.CHAINMAIL_BOOTS ||
                stack.getItem() == Items.LEATHER_HELMET ||
                stack.getItem() == Items.LEATHER_CHESTPLATE ||
                stack.getItem() == Items.LEATHER_LEGGINGS ||
                stack.getItem() == Items.LEATHER_BOOTS ||
                stack.getItem() == Items.TURTLE_HELMET;
    }

    public static boolean hasThornsEnchantment(ItemStack stack) {
        var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) {
            return false;
        }
        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            String enchantId = entry.getIdAsString();
            if (enchantId != null) {
                String lowerEnchantId = enchantId.toLowerCase();
                if (lowerEnchantId.contains("thorns") || lowerEnchantId.contains("шип")) {
                    return true;
                }
            }
        }
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                String loreStr = line.getString().toLowerCase();
                if (loreStr.contains("thorns") || loreStr.contains("шип")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasVanishingCurse(ItemStack stack) {
        var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) {
            return false;
        }
        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            String enchantId = entry.getIdAsString();
            if (enchantId != null && enchantId.toLowerCase().contains("vanishing")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUnbreakableItem(ItemStack stack) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.getBoolean("Unbreakable", false)) {
                return true;
            }
        }
        String name = stack.getName().getString().toLowerCase();
        return name.contains("нерушим") || name.contains("[⚒]");
    }

    public static boolean isSplashPotion(ItemStack stack) {
        return stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION;
    }

    public static Map<RegistryEntry<StatusEffect>, EffectData> getPotionEffects(ItemStack stack) {
        Map<RegistryEntry<StatusEffect>, EffectData> effects = new HashMap<>();
        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) {
            return effects;
        }
        for (StatusEffectInstance effect : potionContents.customEffects()) {
            effects.put(effect.getEffectType(), new EffectData(effect.getAmplifier(), effect.getDuration()));
        }
        return effects;
    }

    public static boolean hasEffect(ItemStack stack, RegistryEntry<StatusEffect> effectType, int minAmplifier) {
        Map<RegistryEntry<StatusEffect>, EffectData> effects = getPotionEffects(stack);
        EffectData data = effects.get(effectType);
        return data != null && data.amplifier >= minAmplifier;
    }

    public static boolean matchesPotionEffects(ItemStack auctionItem, List<PotionEffectRequirement> requirements) {
        if (!isSplashPotion(auctionItem)) {
            return false;
        }
        Map<RegistryEntry<StatusEffect>, EffectData> auctionEffects = getPotionEffects(auctionItem);
        if (auctionEffects.isEmpty()) {
            return false;
        }
        for (PotionEffectRequirement req : requirements) {
            EffectData data = auctionEffects.get(req.effect);
            if (data == null) {
                return false;
            }
            if (data.amplifier < req.minAmplifier) {
                return false;
            }
        }
        return true;
    }

    public static class EffectData {
        public final int amplifier;
        public final int duration;

        public EffectData(int amplifier, int duration) {
            this.amplifier = amplifier;
            this.duration = duration;
        }
    }

    public static class PotionEffectRequirement {
        public final RegistryEntry<StatusEffect> effect;
        public final int minAmplifier;

        public PotionEffectRequirement(RegistryEntry<StatusEffect> effect, int minAmplifier) {
            this.effect = effect;
            this.minAmplifier = minAmplifier;
        }
    }

    public static boolean compareItem(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) return false;
        if (isArmorItem(a) && hasThornsEnchantment(a)) {
            return false;
        }
        String aName = a.getName().getString();
        aName = funTimePricePattern.matcher(aName).replaceAll("").trim();
        String bName = b.getName().getString();
        String aNameClean = cleanString(aName);
        String bNameClean = cleanString(bName);
        if (bNameClean.contains("⚒") || bNameClean.contains("нерушим")) {
            if (!isUnbreakableItem(a) && !hasVanishingCurse(a)) {
                return false;
            }
            if (aNameClean.contains("нерушим") && bNameClean.contains("нерушим")) {
                return aNameClean.contains("элитр") && bNameClean.contains("элитр");
            }
        }
        var aLore = a.get(DataComponentTypes.LORE);
        var bLoreComp = b.get(DataComponentTypes.LORE);
        boolean hasLore = bLoreComp != null && !bLoreComp.lines().isEmpty();
        if (isSplashPotion(a) && isSplashPotion(b)) {
            return comparePotionsByEffects(a, b);
        }
        if (hasLore) {
            List<Text> expectedLore = bLoreComp.lines();
            if (aLore == null || aLore.lines().isEmpty()) {
                return false;
            }
            List<String> auctionLoreStrings = aLore.lines().stream()
                    .map(text -> cleanString(text.getString()))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            String auctionLoreJoined = String.join(" ", auctionLoreStrings);
            boolean hasOriginalMarker = false;
            boolean hasUnbreakableMarker = false;
            for (String line : auctionLoreStrings) {
                if (line.contains("оригинальный предмет") || line.contains("★")) {
                    hasOriginalMarker = true;
                }
                if (line.contains("нерушим") || line.contains("⚒")) {
                    hasUnbreakableMarker = true;
                }
            }
            int matchCount = 0;
            int requiredMatches = 0;
            for (Text expected : expectedLore) {
                String expectedStr = cleanString(expected.getString());
                if (expectedStr.isEmpty()) continue;
                boolean isOriginalMarker = expectedStr.contains("оригинальный предмет") || expectedStr.contains("★");
                boolean isUnbreakableMarker = expectedStr.contains("нерушим") || expectedStr.contains("⚒");
                if (isOriginalMarker) {
                    if (!hasOriginalMarker) {
                        return false;
                    }
                    matchCount++;
                    requiredMatches++;
                    continue;
                }
                if (isUnbreakableMarker) {
                    if (!hasUnbreakableMarker && !isUnbreakableItem(a) && !hasVanishingCurse(a)) {
                        return false;
                    }
                    matchCount++;
                    requiredMatches++;
                    continue;
                }
                requiredMatches++;
                boolean found = false;
                for (String auctionLine : auctionLoreStrings) {
                    if (auctionLine.contains(expectedStr) || expectedStr.contains(auctionLine)) {
                        found = true;
                        break;
                    }
                }
                if (!found && auctionLoreJoined.contains(expectedStr)) {
                    found = true;
                }
                if (found) {
                    matchCount++;
                }
            }
            double matchRatio = requiredMatches > 0 ? (double) matchCount / requiredMatches : 1.0;
            if (matchRatio < 0.5) {
                return false;
            }
            if (hasOriginalMarker) {
                var aEnchants = a.get(DataComponentTypes.ENCHANTMENTS);
                var bEnchants = b.get(DataComponentTypes.ENCHANTMENTS);
                if (bEnchants != null && !bEnchants.isEmpty()) {
                    if (aEnchants == null || aEnchants.isEmpty()) {
                        return false;
                    }
                    Map<String, Integer> aEnchantMap = new HashMap<>();
                    for (RegistryEntry<Enchantment> entry : aEnchants.getEnchantments()) {
                        String enchantId = entry.getIdAsString();
                        if (enchantId != null) {
                            String enchantName = enchantId.replace("minecraft:", "").toLowerCase();
                            int level = aEnchants.getLevel(entry);
                            aEnchantMap.put(enchantName, level);
                        }
                    }
                    Map<String, Integer> bEnchantMap = new HashMap<>();
                    for (RegistryEntry<Enchantment> entry : bEnchants.getEnchantments()) {
                        String enchantId = entry.getIdAsString();
                        if (enchantId != null) {
                            String enchantName = enchantId.replace("minecraft:", "").toLowerCase();
                            int level = bEnchants.getLevel(entry);
                            bEnchantMap.put(enchantName, level);
                        }
                    }
                    if (bEnchantMap.isEmpty()) {
                        return true;
                    }
                    int enchantMatchCount = 0;
                    for (Map.Entry<String, Integer> bEntry : bEnchantMap.entrySet()) {
                        String bEnchantName = bEntry.getKey();
                        Integer aLevel = aEnchantMap.get(bEnchantName);
                        if (aLevel != null && aLevel >= 1) {
                            enchantMatchCount++;
                        }
                    }
                    double enchantMatchRatio = (double) enchantMatchCount / bEnchantMap.size();
                    if (enchantMatchRatio < 1) {
                        return false;
                    }
                }
            }
        } else {
            if (!aNameClean.contains(bNameClean) && !bNameClean.contains(aNameClean)) {
                return false;
            }
        }
        return true;
    }

    private static boolean comparePotionsByEffects(ItemStack auctionPotion, ItemStack templatePotion) {
        Map<RegistryEntry<StatusEffect>, EffectData> auctionEffects = getPotionEffects(auctionPotion);
        Map<RegistryEntry<StatusEffect>, EffectData> templateEffects = getPotionEffects(templatePotion);
        if (templateEffects.isEmpty()) {
            return false;
        }
        if (auctionEffects.isEmpty()) {
            return false;
        }
        for (Map.Entry<RegistryEntry<StatusEffect>, EffectData> entry : templateEffects.entrySet()) {
            RegistryEntry<StatusEffect> requiredEffect = entry.getKey();
            int requiredAmplifier = entry.getValue().amplifier;
            EffectData auctionData = auctionEffects.get(requiredEffect);
            if (auctionData == null) {
                return false;
            }
            if (auctionData.amplifier < requiredAmplifier) {
                return false;
            }
        }
        return true;
    }
}