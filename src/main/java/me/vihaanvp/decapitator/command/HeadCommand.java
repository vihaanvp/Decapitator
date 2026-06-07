package me.vihaanvp.decapitator.command;

import me.vihaanvp.decapitator.Decapitator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class HeadCommand implements CommandExecutor {

    private final Decapitator plugin;

    public HeadCommand(Decapitator plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Subcommands: reload, count, help, or main behaviour: /head [player]
        if (args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("reload")) {
                return handleReload(sender);
            } else if (sub.equals("count") || sub.equals("stats")) {
                return handleCount(sender, args);
            } else if (sub.equals("help")) {
                sender.sendMessage(ChatColor.GREEN + "Decapitator usage:");
                sender.sendMessage(ChatColor.YELLOW + "/head" + ChatColor.WHITE + " - get your own head");
                sender.sendMessage(ChatColor.YELLOW + "/head <player>" + ChatColor.WHITE + " - get another player's head (permission required)");
                sender.sendMessage(ChatColor.YELLOW + "/head count [player]" + ChatColor.WHITE + " - show how many heads ordered");
                sender.sendMessage(ChatColor.YELLOW + "/head reload" + ChatColor.WHITE + " - reload config and data (admin)");
                return true;
            }
            // otherwise fall through to normal handling (player name)
        }

        // Normal head giving logic
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player requester = (Player) sender;

        // check plugin enabled in config
        if (!plugin.getConfig().getBoolean("enabled", true)) {
            requester.sendMessage(ChatColor.RED + "This plugin is currently disabled.");
            return true;
        }

        OfflinePlayer target;
        if (args.length == 0 || (args.length == 1 && (args[0].equalsIgnoreCase(requester.getName()) || args[0].equalsIgnoreCase(requester.getUniqueId().toString())))) {
            target = requester;
        } else {
            // attempt to give another player's head
            if (!requester.hasPermission("decapitator.head.others")) {
                requester.sendMessage(ChatColor.RED + "You don't have permission to get other players' heads.");
                return true;
            }

            String targetName = args[0];
            target = Bukkit.getOfflinePlayerIfCached(targetName);
            if (target == null) {
                target = Bukkit.getOfflinePlayer(targetName);
            }
            if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
                requester.sendMessage(ChatColor.RED + "Could not find player: " + targetName);
                return true;
            }
        }

        // check limit per player (support -1 for unlimited)
        int max = plugin.getConfig().getInt("max-heads-per-player", 10);
        boolean unlimited = (max == -1);
        UUID requesterUuid = requester.getUniqueId();
        int currentCount = plugin.getDataManager().getCount(requesterUuid);
        if (!unlimited && currentCount >= max) {
            requester.sendMessage(ChatColor.RED + "You have reached the maximum number of heads (" + max + ").");
            return true;
        }

        // create the head item
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            boolean applied = false;

            // 1) If SkinsRestorer is installed & enabled, try to get texture via its API (reflection)
            try {
                if (Bukkit.getPluginManager().getPlugin("SkinsRestorer") != null && Bukkit.getPluginManager().getPlugin("SkinsRestorer").isEnabled()) {
                    // net.skinsrestorer.api.SkinsRestorerAPI.getApi().getProfile(name).getProperty()
                    Class<?> srApiClass = Class.forName("net.skinsrestorer.api.SkinsRestorerAPI");
                    Method getApi = srApiClass.getMethod("getApi");
                    Object api = getApi.invoke(null);

                    // try getProfile(String) first
                    Method getProfileMethod = null;
                    try {
                        getProfileMethod = srApiClass.getMethod("getProfile", String.class);
                    } catch (NoSuchMethodException ignore) {
                    }

                    Object srProfile = null;
                    if (getProfileMethod != null) {
                        srProfile = getProfileMethod.invoke(api, target.getName() != null ? target.getName() : "");
                    } else {
                        // fallback to getSkinProperty(String)
                        Method getSkinProp = srApiClass.getMethod("getSkinProperty", String.class);
                        srProfile = getSkinProp.invoke(api, target.getName() != null ? target.getName() : "");
                    }

                    if (srProfile != null) {
                        // srProfile either has getProperty() or is itself a SkinProperty
                        Object skinProp = null;
                        try {
                            Method getProperty = srProfile.getClass().getMethod("getProperty");
                            skinProp = getProperty.invoke(srProfile);
                        } catch (NoSuchMethodException e) {
                            // srProfile might already be a SkinProperty
                            skinProp = srProfile;
                        }

                        if (skinProp != null) {
                            Method getValue = skinProp.getClass().getMethod("getValue");
                            Method getSignature = skinProp.getClass().getMethod("getSignature");
                            String value = (String) getValue.invoke(skinProp);
                            String signature = (String) getSignature.invoke(skinProp);
                            if (value != null && !value.isEmpty()) {
                                // inject GameProfile with textures
                                try {
                                    applyTextureToSkullMeta(meta, value, signature, target.getUniqueId(), target.getName());
                                    applied = true;
                                } catch (Throwable t) {
                                    plugin.getLogger().warning("Failed to apply texture from SkinsRestorer: " + t.getMessage());
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                // Any failure in SkinsRestorer integration should not break the command; fall back below.
                plugin.getLogger().warning("SkinsRestorer integration failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }

            // 2) fallback: use server-side owning player (works for online players/server profiles)
            if (!applied) {
                try {
                    meta.setOwningPlayer(target);
                } catch (Throwable t) {
                    // If setOwningPlayer isn't available or fails, just ignore and continue
                    plugin.getLogger().warning("Failed to set owning player on skull meta: " + t.getMessage());
                }
            }

            skull.setItemMeta(meta);
        }

        // give to requester (always give the head to the player who ran the command)
        if (requester.getInventory().firstEmpty() != -1) {
            requester.getInventory().addItem(skull);
            requester.sendMessage(ChatColor.GREEN + "You received " + (target.getName() != null ? target.getName() : "the player's") + " head.");
        } else {
            requester.getWorld().dropItemNaturally(requester.getLocation(), skull);
            requester.sendMessage(ChatColor.YELLOW + "Inventory full — dropped the head at your feet.");
        }

        // increment and persist the count
        int newCount = plugin.getDataManager().incrementCount(requesterUuid);

        // display counts; if unlimited, show infinity symbol
        String maxDisplay = unlimited ? "∞" : String.valueOf(max);
        requester.sendMessage(ChatColor.GRAY + "Heads used: " + ChatColor.AQUA + newCount + ChatColor.GRAY + " / " + ChatColor.AQUA + maxDisplay);

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("decapitator.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload Decapitator.");
            return true;
        }

        // reload config
        plugin.reloadConfig();

        // reload player data
        try {
            plugin.getDataManager().load();
            sender.sendMessage(ChatColor.GREEN + "Decapitator config and player data reloaded.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to reload player data: " + e.getMessage());
            plugin.getLogger().severe("Failed to reload player data: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    private boolean handleCount(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player requester = (Player) sender;

        int max = plugin.getConfig().getInt("max-heads-per-player", 10);
        boolean unlimited = (max == -1);
        String maxDisplay = unlimited ? "∞" : String.valueOf(max);

        if (args.length == 1) {
            // /head count -> show requester's count
            int count = plugin.getDataManager().getCount(requester.getUniqueId());
            requester.sendMessage(ChatColor.GREEN + "You have used " + ChatColor.AQUA + count + ChatColor.GREEN + " / " + ChatColor.AQUA + maxDisplay + " heads.");
            return true;
        } else {
            // /head count <player> -> show another player's count if permitted
            if (!requester.hasPermission("decapitator.count.others")) {
                requester.sendMessage(ChatColor.RED + "You don't have permission to view other players' counts.");
                return true;
            }
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
            if (target == null) {
                target = Bukkit.getOfflinePlayer(targetName);
            }
            if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
                requester.sendMessage(ChatColor.RED + "Could not find player: " + targetName);
                return true;
            }
            int count = plugin.getDataManager().getCount(target.getUniqueId());
            requester.sendMessage(ChatColor.GREEN + target.getName() + " has used " + ChatColor.AQUA + count + ChatColor.GREEN + " / " + ChatColor.AQUA + maxDisplay + " heads.");
            return true;
        }
    }

    /**
     * Create a GameProfile (reflectively) and inject the textures property into the SkullMeta.
     * Uses reflection to avoid compile-time dependency on com.mojang.authlib.
     */
    private void applyTextureToSkullMeta(SkullMeta meta, String textureValue, String textureSignature, UUID uuid, String name) throws Exception {
        // create GameProfile(UUID, name)
        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        Constructor<?> gpConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
        Object gameProfile = gpConstructor.newInstance(uuid != null ? uuid : UUID.randomUUID(), name);

        // create Property("textures", value, signature)
        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Constructor<?> propertyCtor = propertyClass.getConstructor(String.class, String.class, String.class);
        Object property = propertyCtor.newInstance("textures", textureValue, textureSignature);

        // add property to GameProfile.getProperties().put("textures", property)
        Method getProperties = gameProfileClass.getMethod("getProperties");
        Object propertyMap = getProperties.invoke(gameProfile);
        // propertyMap is a com.mojang.authlib.properties.PropertyMap which supports put(Object, Object)
        Method putMethod = propertyMap.getClass().getMethod("put", Object.class, Object.class);
        putMethod.invoke(propertyMap, "textures", property);

        // Try to call meta.setProfile(GameProfile) if it exists
        try {
            Method setProfile = meta.getClass().getMethod("setProfile", gameProfileClass);
            setProfile.invoke(meta, gameProfile);
            return;
        } catch (NoSuchMethodException ignored) {
        }

        // Fallback: set the private 'profile' field in the SkullMeta implementation
        try {
            Field profileField = null;
            Class<?> clazz = meta.getClass();
            while (clazz != null) {
                try {
                    profileField = clazz.getDeclaredField("profile");
                    break;
                } catch (NoSuchFieldException ex) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (profileField != null) {
                profileField.setAccessible(true);
                profileField.set(meta, gameProfile);
                return;
            }
        } catch (Throwable t) {
            // ignore and allow exception to bubble below if nothing worked
        }

        throw new IllegalStateException("Could not set GameProfile on SkullMeta (no setProfile method or profile field found).");
    }
}