package ru.mirtime.wipe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MirTimeWipe extends JavaPlugin implements CommandExecutor, Listener {

    private File lockFile;
    private final Map<UUID, Integer> unlockSeconds = new HashMap<>();
    private final Map<UUID, BukkitTask> timerTasks = new HashMap<>();

    @Override
    public void onLoad() {
        saveDefaultConfig();
        lockFile = new File(getDataFolder(), "wipe.lock");
        if (lockFile.exists()) {
            Bukkit.getConsoleSender().sendMessage(color("&#0055ff&l⚡ [MirTimeWipe] &eЗапуск вайпа с созданием резервной копии..."));
            List<String> targets = getConfig().getStringList("delete-targets");
            File rootDir = Bukkit.getWorldContainer();
            File backupDir = new File(rootDir, "backup_wipe_" + System.currentTimeMillis());
            backupDir.mkdirs();

            for (String relPath : targets) {
                File target = new File(rootDir, relPath);
                if (target.exists()) {
                    try {
                        File dest = new File(backupDir, relPath);
                        dest.getParentFile().mkdirs();
                        Files.move(target.toPath(), dest.toPath());
                        Bukkit.getConsoleSender().sendMessage(color(" &#00FF66✔ Перенесено в бэкап: &f" + relPath));
                    } catch (IOException e) {
                        Bukkit.getConsoleSender().sendMessage(color(" &c✖ Ошибка переноса: &f" + relPath));
                    }
                }
            }
            lockFile.delete();
            Bukkit.getConsoleSender().sendMessage(color("&#00FF66&l⚡ [MirTimeWipe] Вайп завершён! Резервная копия: &f" + backupDir.getName()));
        }
    }

    @Override
    public void onEnable() {
        if (getCommand("wipe") != null) getCommand("wipe").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getConsoleSender().sendMessage(color("&#00E5FF  MirTimeWipe v1.0.0 успешно загружен!"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(color("&cЭта команда доступна строго операторам сервера (OP)!"));
            return true;
        }
        if (sender instanceof Player p) {
            openStep1Gui(p);
        } else {
            executeWipe();
        }
        return true;
    }

    private void openStep1Gui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, color("&e&l⚠ ВАЙП [1/2] &8| &fВы точно уверены?"));
        ItemStack orange = item(Material.ORANGE_STAINED_GLASS_PANE, "&6▪", null);
        for (int i = 0; i < 27; i++) inv.setItem(i, orange);

        inv.setItem(11, item(Material.LIME_WOOL, "&#00FF66&l[✔ ДА, Я ТОЧНО ХОЧУ СДЕЛАТЬ ВАЙП]", List.of(
                "&7Нажмите, чтобы перейти ко второму",
                "&7финальному подтверждению с таймером."
        )));

        inv.setItem(13, item(Material.BOOK, "&#00E5FF&lИНФОРМАЦИЯ О ВАЙПЕ", List.of(
                "&c✖ &fСбросятся: миры, монеты, иксы, кланы,",
                "   &fаукцион, приваты и награды за время.",
                "",
                "&#00FF66✔ &fСохранятся: привилегии (LuckPerms),",
                "   &fтокены (PlayerPoints) и ключи кейсов."
        )));

        inv.setItem(15, item(Material.RED_WOOL, "&c&l[✖ НЕТ, ОТМЕНА]", List.of("&7Закрыть меню и ничего не менять.")));
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.0f);
    }
    private void openStep2Gui(Player p) {
        stopTimer(p.getUniqueId());
        unlockSeconds.put(p.getUniqueId(), 20);

        Inventory inv = Bukkit.createInventory(null, 27, color("&c&l⚠ ВАЙП [2/2] &8| &4ТОЧНО-ТОЧНО?!"));
        ItemStack red = item(Material.RED_STAINED_GLASS_PANE, "&c▪", null);
        for (int i = 0; i < 27; i++) inv.setItem(i, red);

        inv.setItem(13, item(Material.TNT, "&4&lПОСЛЕДНЕЕ ПРЕДУПРЕЖДЕНИЕ!", List.of(
                "&7После запуска сервер кикнет всех игроков",
                "&7и перезапустится в новом сезоне.",
                "&eСтарые файлы попадут в папку backup_wipe_..."
        )));

        inv.setItem(15, item(Material.RED_WOOL, "&c&l[✖ ОТМЕНИТЬ ВСЁ]", List.of("&7Нажмите, чтобы отменить вайп.")));
        updateStep2Button(p, inv);
        p.openInventory(inv);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || !ChatColor.stripColor(p.getOpenInventory().getTitle()).contains("ВАЙП [2/2]")) {
                    stopTimer(p.getUniqueId());
                    return;
                }
                int left = unlockSeconds.getOrDefault(p.getUniqueId(), 0) - 1;
                if (left <= 0) {
                    unlockSeconds.put(p.getUniqueId(), 0);
                    updateStep2Button(p, p.getOpenInventory().getTopInventory());
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                    cancel();
                    timerTasks.remove(p.getUniqueId());
                } else {
                    unlockSeconds.put(p.getUniqueId(), left);
                    updateStep2Button(p, p.getOpenInventory().getTopInventory());
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.0f);
                }
            }
        }.runTaskTimer(this, 20L, 20L);
        timerTasks.put(p.getUniqueId(), task);
    }

    private void updateStep2Button(Player p, Inventory inv) {
        int left = unlockSeconds.getOrDefault(p.getUniqueId(), 0);
        if (left > 0) {
            ItemStack clock = item(Material.CLOCK, "&e&l[⏳ ПОДОЖДИТЕ " + left + " СЕК.]", List.of(
                    "&7Кнопка запуска разблокируется",
                    "&7через &e" + left + " сек.&7 для защиты от мисклика."
            ));
            clock.setAmount(Math.min(64, left));
            inv.setItem(11, clock);
        } else {
            inv.setItem(11, item(Material.EMERALD_BLOCK, "&#00FF66&l[✔ ДА, ТОЧНО-ТОЧНО ЗАПУСТИТЬ ВАЙП!]", List.of(
                    "&aТаймер завершён!",
                    "&7Нажмите, чтобы начать вайп сервера."
            )));
        }
    }

    private void stopTimer(UUID id) {
        BukkitTask t = timerTasks.remove(id);
        if (t != null) t.cancel();
        unlockSeconds.remove(id);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String clean = ChatColor.stripColor(e.getView().getTitle());
        if (!clean.contains("ВАЙП [1/2]") && !clean.contains("ВАЙП [2/2]")) return;
        e.setCancelled(true);

        if (!p.isOp()) { p.closeInventory(); return; }
        int slot = e.getRawSlot();

        if (clean.contains("ВАЙП [1/2]")) {
            if (slot == 15) {
                p.closeInventory();
                p.sendMessage(color(getConfig().getString("prefix") + "&7Вайп отменён."));
            } else if (slot == 11) {
                openStep2Gui(p);
            }
            return;
        }

        if (clean.contains("ВАЙП [2/2]")) {
            if (slot == 15) {
                stopTimer(p.getUniqueId());
                p.closeInventory();
                p.sendMessage(color(getConfig().getString("prefix") + "&7Вайп отменён."));
            } else if (slot == 11) {
                int left = unlockSeconds.getOrDefault(p.getUniqueId(), 20);
                if (left > 0) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                stopTimer(p.getUniqueId());
                p.closeInventory();
                executeWipe();
            }
        }
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            stopTimer(p.getUniqueId());
        }
    }

    private void executeWipe() {
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            lockFile.createNewFile();
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage(color("&c[MirTimeWipe] Ошибка создания wipe.lock!"));
            return;
        }

        String kickMsg = color("&#0055ff&l⚡ &#00E5FF&lСЕРВЕР MIR-TIME &#0055ff&l⚡\n\n" +
                "&e&lНАЧИНАЕТСЯ ГЛОБАЛЬНЫЙ ВАЙП И НОВЫЙ СЕЗОН!\n" +
                "&fКарта, монеты и кланы обнуляются.\n" +
                "&#00FF66Ваши привилегии, токены и кейсы сохранены!\n\n" +
                "&7Перезаходите на сервер через 1–2 минуты.");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer(kickMsg);
        }

        Bukkit.getScheduler().runTaskLater(this, Bukkit::shutdown, 20L);
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null) {
                List<String> list = new ArrayList<>();
                for (String s : lore) list.add(color(s));
                meta.setLore(list);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            is.setItemMeta(meta);
        }
        return is;
    }

    public static String color(String text) {
        if (text == null) return "";
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder rep = new StringBuilder("§x");
            for (char c : hex.toCharArray()) rep.append('§').append(c);
            matcher.appendReplacement(buffer, rep.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
