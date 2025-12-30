package rich.util.modules.autobuy;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ItemDebugLogger {
    private static File logFile;
    private static PrintWriter writer;

    public static void init() {
        try {
            File logsDir = new File("autobuy_logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            logFile = new File(logsDir, "auction_scan_" + timestamp + ".txt");
            writer = new PrintWriter(new FileWriter(logFile, true));
            writer.println("========== AUTOBUY DEBUG LOG ==========");
            writer.println("Время создания: " + new Date());
            writer.println("=======================================\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logSlot(int slotId, ItemStack stack) {
        if (writer == null) init();

        try {
            writer.println("╔═══════════════════════════════════════════════════════════════");
            writer.println("║ СЛОТ #" + slotId);
            writer.println("╠═══════════════════════════════════════════════════════════════");
            
            if (stack.isEmpty()) {
                writer.println("║ ПУСТОЙ СЛОТ");
                writer.println("╚═══════════════════════════════════════════════════════════════\n");
                writer.flush();
                return;
            }

            writer.println("║ ПРЕДМЕТ: " + stack.getItem().toString());
            writer.println("║ НАЗВАНИЕ: " + stack.getName().getString());
            writer.println("║ КОЛИЧЕСТВО: " + stack.getCount());
            writer.println("║");

            var components = stack.getComponents();
            writer.println("║ ▼ ВСЕ КОМПОНЕНТЫ:");
            writer.println("║ " + components.toString());
            writer.println("║");

            var customName = stack.get(DataComponentTypes.CUSTOM_NAME);
            if (customName != null) {
                writer.println("║ CUSTOM_NAME: " + customName.getString());
            }

            var lore = stack.get(DataComponentTypes.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
                writer.println("║");
                writer.println("║ ▼ ОПИСАНИЕ (LORE):");
                int lineNum = 1;
                for (Text line : lore.lines()) {
                    String lineStr = line.getString();
                    writer.println("║   [" + lineNum + "] " + lineStr);
                    writer.println("║       RAW: " + line.toString());
                    lineNum++;
                }
            } else {
                writer.println("║ ОПИСАНИЕ: отсутствует");
            }

            var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants != null && !enchants.isEmpty()) {
                writer.println("║");
                writer.println("║ ▼ ЗАЧАРОВАНИЯ:");
                for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                    int level = enchants.getLevel(entry);
                    writer.println("║   • " + entry.getIdAsString() + " уровень " + level);
                }
            }

            var potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (potionContents != null) {
                writer.println("║");
                writer.println("║ ▼ ЭФФЕКТЫ ЗЕЛЬЯ:");
                if (!potionContents.customEffects().isEmpty()) {
                    potionContents.customEffects().forEach(effect -> {
                        writer.println("║   • " + effect.getEffectType().toString() + 
                                     " Ур." + (effect.getAmplifier() + 1) + 
                                     " Время:" + effect.getDuration());
                    });
                } else {
                    writer.println("║   нет кастомных эффектов");
                }
            }

            writer.println("║");
            writer.println("║ ▼ ПОПЫТКА НАЙТИ ЦЕНУ:");
            
            String componentStr = components.toString();
            writer.println("║   Ищем в компонентах...");
            if (componentStr.contains("$")) {
                int dollarIndex = componentStr.indexOf("$");
                String around = componentStr.substring(Math.max(0, dollarIndex - 50), 
                                                      Math.min(componentStr.length(), dollarIndex + 50));
                writer.println("║   Найден $: " + around);
            }
            
            if (lore != null) {
                writer.println("║   Ищем в описании...");
                for (Text line : lore.lines()) {
                    String lineStr = line.getString();
                    if (lineStr.contains("$") || lineStr.matches(".*\\d+.*")) {
                        writer.println("║   Строка с ценой?: " + lineStr);
                    }
                }
            }

            String itemName = stack.getName().getString();
            if (itemName.contains("$")) {
                writer.println("║   Найден $ в названии: " + itemName);
            }

            writer.println("╚═══════════════════════════════════════════════════════════════\n");
            writer.flush();

        } catch (Exception e) {
            writer.println("║ ОШИБКА ЛОГИРОВАНИЯ: " + e.getMessage());
            e.printStackTrace(writer);
            writer.flush();
        }
    }

    public static void logScanStart(int totalSlots) {
        if (writer == null) init();
        writer.println("\n\n");
        writer.println("╔═══════════════════════════════════════════════════════════════");
        writer.println("║ НАЧАЛО СКАНИРОВАНИЯ АУКЦИОНА");
        writer.println("║ Время: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
        writer.println("║ Всего слотов: " + totalSlots);
        writer.println("╚═══════════════════════════════════════════════════════════════\n");
        writer.flush();
    }

    public static void logScanEnd(int found, int total) {
        if (writer == null) init();
        writer.println("\n");
        writer.println("╔═══════════════════════════════════════════════════════════════");
        writer.println("║ КОНЕЦ СКАНИРОВАНИЯ");
        writer.println("║ Найдено для покупки: " + found + " из " + total);
        writer.println("╚═══════════════════════════════════════════════════════════════\n\n");
        writer.flush();
    }

    public static void close() {
        if (writer != null) {
            writer.println("\n========== КОНЕЦ ЛОГА ==========");
            writer.close();
            writer = null;
        }
    }

    public static String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : "не создан";
    }
}