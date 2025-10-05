package org.bloby.insanekick;

import org.bloby.insanekick.listeners.DeathListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class InsaneKick extends JavaPlugin {

    private static InsaneKick instance;
    private DeathListener deathListener;
    private final Map<UUID, Long> lastDeathTime = new HashMap<>();
    private boolean debugMode = false;
    private long serverStartTime;

    @Override
    public void onEnable() {
        instance = this;
        serverStartTime = System.currentTimeMillis();

        long startTime = System.currentTimeMillis();

        initializeConfig();
        registerListeners();
        registerCommands();
        loadSettings();

        long loadTime = System.currentTimeMillis() - startTime;

        getLogger().info("================================");
        getLogger().info("InsaneKick v" + getDescription().getVersion());
        getLogger().info("Autor: " + getDescription().getAuthors());
        getLogger().info("Status: AKTIVNÍ");
        getLogger().info("Načteno za: " + loadTime + "ms");
        getLogger().info("================================");

        if (debugMode) {
            getLogger().warning("Debug mód je ZAPNUTÝ!");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("================================");
        getLogger().info("InsaneKick se vypína...");

        cleanupData();
        unregisterListeners();

        getLogger().info("Plugin byl úspěšně vypnut!");
        getLogger().info("================================");

        instance = null;
    }

    private void initializeConfig() {
        saveDefaultConfig();
        getLogger().info("Konfigurace byla načtena");
    }

    private void registerListeners() {
        deathListener = new DeathListener(this); // oprava case-sensitivity
        getServer().getPluginManager().registerEvents(deathListener, this);
        getLogger().info("Death listener byl registrován");
    }

    private void registerCommands() {
        getLogger().info("Příkazy byly registrovány");
    }

    private void loadSettings() {
        debugMode = getConfig().getBoolean("debug-mode", false);

        if (debugMode) {
            getLogger().setLevel(Level.ALL);
        }
    }

    private void cleanupData() {
        lastDeathTime.clear();
        getLogger().info("Data byla vyčištěna");
    }

    private void unregisterListeners() {
        if (deathListener != null) {
            getLogger().info("Listenery byly odregistrovány");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("insanekick")) {
            return false;
        }

        if (!sender.hasPermission("insanekick.admin")) {
            sender.sendMessage(formatMessage("&c&lInsaneKick &8| &cNemáš oprávnění použít tento příkaz!"));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "info":
                handleInfo(sender);
                break;
            case "toggle":
                handleToggle(sender);
                break;
            case "debug":
                handleDebug(sender);
                break;
            case "stats":
                handleStats(sender);
                break;
            default:
                sender.sendMessage(formatMessage("&c&lInsaneKick &8| &cNeznámý příkaz! Použij: &f/insanekick help"));
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(formatMessage("&6&m                                    "));
        sender.sendMessage(formatMessage("&c&lInsaneKick &7v" + getDescription().getVersion()));
        sender.sendMessage(formatMessage("&7Autor: &fBloby"));
        sender.sendMessage("");
        sender.sendMessage(formatMessage("&e/insanekick reload &7- Znovu načte konfiguraci"));
        sender.sendMessage(formatMessage("&e/insanekick info &7- Zobrazí informace o pluginu"));
        sender.sendMessage(formatMessage("&e/insanekick toggle &7- Zapne/vypne plugin"));
        sender.sendMessage(formatMessage("&e/insanekick debug &7- Zapne/vypne debug mód"));
        sender.sendMessage(formatMessage("&e/insanekick stats &7- Zobrazí statistiky"));
        sender.sendMessage(formatMessage("&6&m                                    "));
    }

    private void handleReload(CommandSender sender) {
        long startTime = System.currentTimeMillis();

        reloadConfig();
        loadSettings();

        long reloadTime = System.currentTimeMillis() - startTime;

        sender.sendMessage(formatMessage("&c&lInsaneKick &8| &aKonfigurace byla úspěšně načtena!"));
        sender.sendMessage(formatMessage("&c&lInsaneKick &8| &7Čas načítání: &f" + reloadTime + "ms"));

        getLogger().info(sender.getName() + " znovu načetl konfiguraci");
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage(formatMessage("&6&m                                    "));
        sender.sendMessage(formatMessage("&c&lInsaneKick &7Informace"));
        sender.sendMessage("");
        sender.sendMessage(formatMessage("&7Verze: &f" + getDescription().getVersion()));
        sender.sendMessage(formatMessage("&7Autor: &f" + getDescription().getAuthors()));
        sender.sendMessage(formatMessage("&7Status: " + (isPluginEnabled() ? "&aAKTIVNÍ" : "&cNEAKTIVNÍ")));
        sender.sendMessage(formatMessage("&7Debug: " + (debugMode ? "&eZAPNUTÝ" : "&cVYPNUTÝ")));
        sender.sendMessage(formatMessage("&7Kick delay: &f" + getKickDelay() + "s"));
        sender.sendMessage(formatMessage("&7Server: &f" + getServer().getName() + " " + getServer().getVersion()));
        sender.sendMessage(formatMessage("&6&m                                    "));
    }

    private void handleToggle(CommandSender sender) {
        boolean currentState = isPluginEnabled();
        getConfig().set("enabled", !currentState);
        saveConfig();

        String status = !currentState ? "&aZAPNUT" : "&cVYPNUT";
        sender.sendMessage(formatMessage("&c&lInsaneKick &8| &7Plugin byl " + status + "&7!"));

        getLogger().info(sender.getName() + " změnil stav pluginu na: " + !currentState);
    }

    private void handleDebug(CommandSender sender) {
        debugMode = !debugMode;
        getConfig().set("debug-mode", debugMode);
        saveConfig();

        String status = debugMode ? "&eZAPNUT" : "&cVYPNUT";
        sender.sendMessage(formatMessage("&c&lInsaneKick &8| &7Debug mód byl " + status + "&7!"));

        if (debugMode) {
            getLogger().setLevel(Level.ALL);
        } else {
            getLogger().setLevel(Level.INFO);
        }
    }

    private void handleStats(CommandSender sender) {
        int onlinePlayers = getServer().getOnlinePlayers().size();
        int maxPlayers = getServer().getMaxPlayers();
        long uptime = getServerUptime(); // oprava getStartTime()

        sender.sendMessage(formatMessage("&6&m                                    "));
        sender.sendMessage(formatMessage("&c&lInsaneKick &7Statistiky"));
        sender.sendMessage("");
        sender.sendMessage(formatMessage("&7Hráči online: &f" + onlinePlayers + "/" + maxPlayers));
        sender.sendMessage(formatMessage("&7Zaznamenaných úmrtí: &f" + lastDeathTime.size()));
        sender.sendMessage(formatMessage("&7Uptime serveru: &f" + formatUptime(uptime)));
        sender.sendMessage(formatMessage("&6&m                                    "));
    }

    private long getServerUptime() {
        return System.currentTimeMillis() - serverStartTime;
    }

    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
    }

    public void recordDeath(Player player) {
        lastDeathTime.put(player.getUniqueId(), System.currentTimeMillis());

        if (debugMode) {
            getLogger().info("Zaznamenána smrt hráče: " + player.getName());
        }
    }

    public long getLastDeathTime(UUID uuid) {
        return lastDeathTime.getOrDefault(uuid, 0L);
    }

    public static InsaneKick getInstance() {
        return instance;
    }

    public String getKickMessage() {
        String message = getConfig().getString("kick-message",
                "&c&lInsaneKick\n&7\n&fOmlouvám se, ale bohužel jsi umřel,\n&ftak tě to vyhodilo.\n&7\n&ePřipoj se prosím znovu!");
        return formatMessage(message);
    }

    public int getKickDelay() {
        return getConfig().getInt("kick-delay", 0);
    }

    public boolean isPluginEnabled() {
        return getConfig().getBoolean("enabled", true);
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    private String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
