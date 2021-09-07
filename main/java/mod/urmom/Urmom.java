package mod.urmom;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Base64;

public final class Urmom extends JavaPlugin {
    public SQLStuff SQL;
    private File customConfigFile;
    private FileConfiguration customConfig;
    @Override
    public void onEnable() {
        // Plugin startup logic
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
        if (label.equalsIgnoreCase("onduty")) {
            if (!(sender instanceof Player)) return true;

            Player player = (Player) sender;
            PreparedStatement ps;
            try {
                ps = SQL.getConnection().prepareStatement("SELECT * FROM urmom WHERE UUID=?");
                ps.setString(1, player.getUniqueId().toString());
                ResultSet results = ps.executeQuery();
                if (results.next()) {
                    player.sendMessage(ChatColor.RED + "You are already on duty!");
                    return true;
                }
                ps = SQL.getConnection().prepareStatement("INSERT INTO urmom (UUID, INVFO, ARMOR) VALUES (?,?,?)");
                ps.setString(1, player.getUniqueId().toString());
                ps.setString(2, mod.urmom.SavingStuff.itemStackArrayToBase64(player.getInventory().getContents()));
                ps.setString(3, mod.urmom.SavingStuff.itemStackArrayToBase64(player.getInventory().getArmorContents()));
                ps.executeUpdate();
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
                player.setGameMode(GameMode.SPECTATOR);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,6010 * 20, 1));
                player.sendMessage(ChatColor.GREEN + "You are now on duty soldier!");
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        if (label.equalsIgnoreCase("offduty")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
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
                player.getInventory().setContents(mod.urmom.SavingStuff.itemStackArrayFromBase64(results.getString("INVFO")));
                player.getInventory().setArmorContents(mod.urmom.SavingStuff.itemStackArrayFromBase64(results.getString("ARMOR")));
                for (PotionEffect e : player.getActivePotionEffects()) {
                    player.removePotionEffect(e.getType());
                }
                player.sendMessage(ChatColor.GREEN + "You are now off duty!");
            } else player.sendMessage(ChatColor.RED + "You are not even on duty.");

            } catch (SQLException | IOException e) {
                Bukkit.getLogger().severe("Some error in the offduty cmd");
            }
        }
        return false;
    }
}
