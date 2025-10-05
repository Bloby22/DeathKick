package org.bloby.insanekick.listeners;

import org.bloby.insanekick.InsaneKick;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathListener implements Listener {

    private final InsaneKick plugin;
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    private final Map<UUID, Long> lastDeathTimestamp = new HashMap<>();

    public DeathListener(InsaneKick plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.isPluginEnabled()) {
            plugin.debug("Plugin je deaktivován, přeskakuji událost smrti");
            return;
        }

        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        
        updateDeathStatistics(player);
        plugin.recordDeath(player);
        
        logDeathInformation(player, event);
        
        schedulePlayerKick(player);
    }

    private void updateDeathStatistics(Player player) {
        UUID playerId = player.getUniqueId();
        int deaths = deathCount.getOrDefault(playerId, 0) + 1;
        deathCount.put(playerId, deaths);
        lastDeathTimestamp.put(playerId, System.currentTimeMillis());
        
        plugin.debug("Hráč " + player.getName() + " zemřel (celkem: " + deaths + "x)");
    }

    private void logDeathInformation(Player player, PlayerDeathEvent event) {
        String deathMessage = event.getDeathMessage() != null ? event.getDeathMessage() : "Neznámá příčina";
        String location = String.format("Svět: %s, X: %d, Y: %d, Z: %d",
            player.getWorld().getName(),
            player.getLocation().getBlockX(),
            player.getLocation().getBlockY(),
            player.getLocation().getBlockZ()
        );
        
        plugin.getLogger().info("=== UDÁLOST SMRTI ===");
        plugin.getLogger().info("Hráč: " + player.getName());
        plugin.getLogger().info("UUID: " + player.getUniqueId());
        plugin.getLogger().info("Příčina: " + deathMessage);
        plugin.getLogger().info("Lokace: " + location);
        plugin.getLogger().info("====================");
        
        if (plugin.isDebugMode()) {
            plugin.debug("Zdraví před smrtí: " + player.getHealth());
            plugin.debug("Level: " + player.getLevel());
            plugin.debug("Exp: " + player.getTotalExperience());
        }
    }

    private void schedulePlayerKick(Player player) {
        String kickMessage = plugin.getKickMessage();
        int kickDelay = plugin.getKickDelay();
        long delayTicks = kickDelay > 0 ? kickDelay * 20L : 1L;
        
        new BukkitRunnable() {
            int countdown = kickDelay;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    plugin.debug("Hráč " + player.getName() + " již není online, rušíme kick");
                    cancel();
                    return;
                }
                
                if (kickDelay > 0 && countdown > 0) {
                    sendCountdownMessage(player, countdown);
                    countdown--;
                } else {
                    executeKick(player, kickMessage);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, delayTicks, 20L);
    }

    private void sendCountdownMessage(Player player, int seconds) {
        String title = "§c§lUMŘEL JSI!";
        String subtitle = "§7Vyhodím tě za §e" + seconds + "§7 sekund...";
        
        player.sendTitle(title, subtitle, 10, 40, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
        
        plugin.debug("Odpočítávání pro " + player.getName() + ": " + seconds + "s");
    }

    private void executeKick(Player player, String kickMessage) {
        if (!player.isOnline()) {
            return;
        }
        
        try {
            player.kickPlayer(kickMessage);
            
            String logMessage = String.format(
                "Hráč %s byl vyhozen po smrti (Úmrtí celkem: %d)",
                player.getName(),
                deathCount.getOrDefault(player.getUniqueId(), 0)
            );
            
            plugin.getLogger().info(logMessage);
            plugin.debug("Kick byl úspěšně proveden");
            
            broadcastKickNotification(player);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Chyba při vyhazování hráče " + player.getName() + ": " + e.getMessage());
            
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastKickNotification(Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("insanekick.notify")) {
                online.sendMessage("§c§lInsaneKick §8| §7Hráč §f" + player.getName() + " §7byl vyhozen po smrti");
            }
        }
    }

    public int getDeathCount(UUID playerId) {
        return deathCount.getOrDefault(playerId, 0);
    }

    public long getLastDeathTimestamp(UUID playerId) {
        return lastDeathTimestamp.getOrDefault(playerId, 0L);
    }

    public void resetDeathCount(UUID playerId) {
        deathCount.remove(playerId);
        lastDeathTimestamp.remove(playerId);
        plugin.debug("Reset statistik pro hráče: " + playerId);
    }

    public void clearAllStatistics() {
        deathCount.clear();
        lastDeathTimestamp.clear();
        plugin.getLogger().info("Všechny statistiky úmrtí byly vymazány");
    }

    public Map<UUID, Integer> getDeathStatistics() {
        return new HashMap<>(deathCount);
    }
}
