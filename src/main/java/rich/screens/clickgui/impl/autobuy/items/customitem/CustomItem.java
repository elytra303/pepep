package rich.screens.clickgui.impl.autobuy.items.customitem;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.settings.AutoBuyItemSettings;
import rich.screens.clickgui.impl.autobuy.settings.AutoBuySettingsManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CustomItem implements AutoBuyableItem {
    private final String displayName;
    private final NbtCompound nbt;
    private final Item material;
    private final int price;
    private final PotionContentsComponent potionContents;
    private final List<Text> loreTexts;
    private final AutoBuyItemSettings settings;
    private boolean enabled;

    public CustomItem(String displayName, NbtCompound nbt, Item material, int price, PotionContentsComponent potionContents, List<Text> loreTexts) {
        this.displayName = displayName;
        this.nbt = nbt;
        this.material = material;
        this.price = price;
        this.potionContents = potionContents;
        this.loreTexts = loreTexts;
        this.enabled = true;
        this.settings = new AutoBuyItemSettings(price, material, displayName);
        AutoBuySettingsManager.getInstance().loadSettings(displayName, this.settings);
    }

    public CustomItem(String displayName, NbtCompound nbt, Item material, int price) {
        this(displayName, nbt, material, price, null, null);
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemStack createItemStack() {
        ItemStack stack = new ItemStack(material);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName));
        if (material == Items.POTION) {
            int color = switch (displayName) {
                case "Зелье отрыжки" -> 0xFF5D00;
                case "Зелье серной кислоты" -> 0x00C200;
                case "Зелье вспышки" -> 0xFFFFFF;
                case "Зелье мочи Флеша" -> 0x5CF7FF;
                case "Зелье победителя" -> 0x00FF00;
                case "Зелье агента" -> 0xFFFB00;
                case "Зелье медика" -> 0xFF00DE;
                case "Зелье киллера" -> 0xFF0000;
                default -> 0x385DC6;
            };
            stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.<StatusEffectInstance>of(), Optional.empty()));
        } else if (potionContents != null) {
            stack.set(DataComponentTypes.POTION_CONTENTS, potionContents);
        }
        if (loreTexts != null) {
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreTexts));
        }
        if (material == Items.TOTEM_OF_UNDYING) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        if (nbt != null) {
            NbtCompound nbtCopy = nbt.copy();
            if (material == Items.PLAYER_HEAD && nbtCopy.getCompound("SkullOwner").isPresent()) {
                NbtCompound skullOwner = nbtCopy.getCompound("SkullOwner").get();
                Optional<int[]> idArray = skullOwner.getIntArray("Id");
                UUID id;
                if (idArray.isPresent()) {
                    int[] arr = idArray.get();
                    id = uuidFromIntArray(arr);
                } else {
                    Optional<String> idString = skullOwner.getString("Id");
                    id = idString.map(UUID::fromString).orElse(UUID.randomUUID());
                }
                ImmutableMultimap.Builder<String, Property> builder = ImmutableMultimap.builder();
                Optional<NbtCompound> propertiesOpt = skullOwner.getCompound("Properties");
                if (propertiesOpt.isPresent()) {
                    NbtCompound properties = propertiesOpt.get();
                    Optional<NbtList> texturesOpt = properties.getList("textures");
                    if (texturesOpt.isPresent()) {
                        NbtList textures = texturesOpt.get();
                        if (!textures.isEmpty()) {
                            Optional<NbtCompound> textureNbtOpt = textures.getCompound(0);
                            if (textureNbtOpt.isPresent()) {
                                Optional<String> valueOpt = textureNbtOpt.get().getString("Value");
                                if (valueOpt.isPresent()) {
                                    builder.put("textures", new Property("textures", valueOpt.get()));
                                }
                            }
                        }
                    }
                }
                PropertyMap propertyMap = new PropertyMap(builder.build());
                GameProfile profile = new GameProfile(id, "", propertyMap);
                stack.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(profile));
                nbtCopy.remove("SkullOwner");
            }
            if (!nbtCopy.isEmpty()) {
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtCopy));
            }
        }
        return stack;
    }

    private static UUID uuidFromIntArray(int[] arr) {
        if (arr.length != 4) {
            return UUID.randomUUID();
        }
        long mostSig = ((long) arr[0] << 32) | (arr[1] & 0xFFFFFFFFL);
        long leastSig = ((long) arr[2] << 32) | (arr[3] & 0xFFFFFFFFL);
        return new UUID(mostSig, leastSig);
    }

    public int getPrice() {
        return price;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public AutoBuyItemSettings getSettings() {
        return settings;
    }
}