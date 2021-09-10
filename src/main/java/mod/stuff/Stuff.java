package mod.stuff;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Base64;
public final class Stuff extends JavaPlugin implements Listener{
    public SQLStuff SQL;
    private File customConfigFile;
    private FileConfiguration customConfig;
    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getServer().getPluginManager().registerEvents(this, this);
        this.SQL = new SQLStuff();
        try {
            SQL.connect();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        if (SQL.isConnected()) {
            Bukkit.getLogger().info("Database Connected!");
            SQL.initializedbs();
        }
        createCustomConfig();
    }
    public FileConfiguration getCustomConfig() {
        return this.customConfig;
    }

    private void createCustomConfig() {
        customConfigFile = new File(getDataFolder(), "custom.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("custom.yml", false);
        }

        customConfig= new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            SQL.disconnect();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (label.equalsIgnoreCase("onduty")) {
            try {
                PreparedStatement ps;
                PreparedStatement ps2;
                if(!(sender instanceof Player)) return true;
                player.sendMessage(player.getName());

                    ps = SQL.getConnection().prepareStatement("SELECT * FROM mods WHERE mods=?");
                    ps.setString(1, player.getName());
                    ResultSet resultSet = ps.executeQuery();

                if (!resultSet.next() && !player.isOp()) {
                    player.sendMessage(ChatColor.RED + "You dont have permission!");
                    return true;
                }



                ps = SQL.getConnection().prepareStatement("SELECT * FROM urmom WHERE UUID=?");
                ps.setString(1, player.getUniqueId().toString());
                ResultSet results = ps.executeQuery();
                if (results.next()) {
                    player.sendMessage(ChatColor.RED + "You are already on duty!");
                    return true;
                }
                ps2 = SQL.getConnection().prepareStatement("INSERT INTO urmom (UUID, INVFO, ARMOR) VALUES (?,?,?)");
                ps2.setString(1, player.getUniqueId().toString());
                ps2.setString(2, mod.stuff.SavingStuff.itemStackArrayToBase64(player.getInventory().getContents()));
                ps2.setString(3, mod.stuff.SavingStuff.itemStackArrayToBase64(player.getInventory().getArmorContents()));
                ps2.executeUpdate();
                if (getCustomConfig().contains("players." + player.getUniqueId().toString() + ".coords"))
                    getCustomConfig().set("players." + player.getUniqueId().toString(), null);
                getCustomConfig().set("players." + player.getUniqueId().toString() + ".coords.world", player.getWorld().getName());
                getCustomConfig().set("players." + player.getUniqueId().toString() + ".coords.x", player.getLocation().getX());
                getCustomConfig().set("players." + player.getUniqueId().toString() + ".coords.y", player.getLocation().getY());
                getCustomConfig().set("players." + player.getUniqueId().toString() + ".coords.z", player.getLocation().getZ());
                getCustomConfig().set("players." + player.getUniqueId().toString() + ".coords.pitch", player.getLocation().getPitch());
                getCustomConfig().set("players." + player.getUniqueId().toString() + ".coords.yaw", player.getLocation().getYaw());
                getCustomConfig().save(customConfigFile);
                player.getInventory().clear();
                player.setGameMode(GameMode.CREATIVE);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,6010 * 20, 1));
                ItemStack s = new ItemStack(Material.STICK);
                ItemStack se = new ItemStack(Material.WOODEN_HOE);
                ItemStack ses = new ItemStack(Material.WOODEN_AXE);
                ItemStack sea = new ItemStack(Material.WOODEN_SHOVEL);
                ItemStack sed = new ItemStack(Material.GOLDEN_SHOVEL);
                ItemStack[] e = new ItemStack[] {s, se, ses, sea, sed};
                player.getInventory().addItem(e);
                player.sendMessage(ChatColor.GREEN + "You are now on duty soldier!");
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        if (label.equalsIgnoreCase("offduty")) {
            if (!(sender instanceof Player)) return true;

            PreparedStatement ps;
            PreparedStatement ps2;
            try {
                ps2 = SQL.getConnection().prepareStatement("SELECT * FROM urmom WHERE UUID=?");
                ps2.setString(1, player.getUniqueId().toString());
                ResultSet results = ps2.executeQuery();
                if (results.next()) {
                    ps = SQL.getConnection().prepareStatement("DELETE FROM urmom WHERE UUID=?");
                    ps.setString(1, player.getUniqueId().toString());
                    ps.executeUpdate();
                    World w = Bukkit.getWorld(getCustomConfig().getString("players." + player.getUniqueId().toString() + ".coords.world"));
                    double x = getCustomConfig().getDouble("players." + player.getUniqueId().toString() + ".coords.x");
                    double y = getCustomConfig().getDouble("players." + player.getUniqueId().toString() + ".coords.y");
                    double z = getCustomConfig().getDouble("players." + player.getUniqueId().toString() + ".coords.z");
                    String yaw = getCustomConfig().getString("players." + player.getUniqueId().toString() + ".coords.yaw");
                    String pitch = getCustomConfig().getString("players." + player.getUniqueId().toString() + ".coords.pitch");
                    Location loc = new Location(w, x, y, z, Float.parseFloat(yaw), Float.parseFloat(pitch));
                    player.teleport(loc);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().setContents(mod.stuff.SavingStuff.itemStackArrayFromBase64(results.getString("INVFO")));
                    player.getInventory().setArmorContents(mod.stuff.SavingStuff.itemStackArrayFromBase64(results.getString("ARMOR")));
                    for (PotionEffect e : player.getActivePotionEffects()) {
                        player.removePotionEffect(e.getType());
                    }
                    player.sendMessage(ChatColor.GREEN + "You are now off duty!");
                } else player.sendMessage(ChatColor.RED + "You are not even on duty.");

            } catch (SQLException | IOException e) {
                Bukkit.getLogger().severe("Some error in the offduty cmd");
            }
        }
        if (label.equalsIgnoreCase("addmod")) {
            if (args.length >= 0 && player.isOp()) {
                PreparedStatement ps;
                try {
                    ps = SQL.getConnection().prepareStatement("SELECT * FROM mods where mods = ?");
                    ps.setString(1, player.getName());
                    ResultSet resultSet = ps.executeQuery();
                    if (resultSet.next()) {
                        player.sendMessage(ChatColor.RED + "Player is already mod");
                        return true;
                    }
                    ps = SQL.getConnection().prepareStatement("INSERT INTO mods (mods) VALUES(?)");
                    ps.setString(1, player.getName());
                    ps.executeUpdate();
                    player.sendMessage(ChatColor.GREEN + "Player is now moderator!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                player.sendMessage(ChatColor.RED + "You don't have permission or missing arguments");
                net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent("Read the docs(Click me) for more information.");
                message.setColor(net.md_5.bungee.api.ChatColor.RED);
                message.setBold(true);
                message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://empathyyt.github.io"));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click Me!")));
                player.spigot().sendMessage(message);
            }
        }
        if (label.equalsIgnoreCase("remmod")) {
            if (args.length >= 0 && player.isOp()) {
                PreparedStatement ps;
                try {
                    ps = SQL.getConnection().prepareStatement("SELECT * FROM mods where mods = ?");
                    ps.setString(1, player.getName());
                    ResultSet resultSet = ps.executeQuery();
                    if (!resultSet.next()) {
                        player.sendMessage(ChatColor.RED + "Player is not mod");
                        return true;
                    }
                    ps = SQL.getConnection().prepareStatement("DELETE FROM mods WHERE mods=?");
                    ps.setString(1, player.getName());
                    ps.executeUpdate();
                    player.sendMessage(ChatColor.GREEN + "Player is no longer a moderator!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                player.sendMessage(ChatColor.RED + "You don't have permission or missing arguments");
                net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent("Read the docs(Click me) for more information.");
                message.setColor(net.md_5.bungee.api.ChatColor.RED);
                message.setBold(true);
                message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://empathyyt.github.io"));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click Me!")));
                player.spigot().sendMessage(message);
            }
        }
        return false;
    }

    @EventHandler
    public void InventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        PreparedStatement ps2;

        try {
            ps2 = SQL.getConnection().prepareStatement("SELECT * FROM urmom WHERE UUID=?");
            ps2.setString(1, player.getUniqueId().toString());
            ResultSet results = ps2.executeQuery();
            if (player.getGameMode().equals(GameMode.CREATIVE) && results.next()) {
                event.setCancelled(true);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
    @EventHandler()
    public void PlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String[] ress = message.split(" ");
        PreparedStatement ps2;
        try {
            ps2 = SQL.getConnection().prepareStatement("SELECT * FROM urmom WHERE UUID=?");
            ps2.setString(1, player.getUniqueId().toString());
            ResultSet results = ps2.executeQuery();
            if (results.next() && ress[0].equalsIgnoreCase("/gamemode")) {
                if (ress[1].equalsIgnoreCase("survival")) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                if (ress[1].equalsIgnoreCase("creative")) {
                    player.setGameMode(GameMode.CREATIVE);
                }
                if (ress[1].equalsIgnoreCase("spectator")) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
