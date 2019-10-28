package com.ruinscraft.botboi.plugin;

import com.ruinscraft.botboi.storage.MySqlStorage;
import com.ruinscraft.botboi.storage.Storage;
import org.bukkit.plugin.java.JavaPlugin;

public class BotBoiPlugin extends JavaPlugin {

    private static BotBoiPlugin instance;
    private Storage storage;

    public static BotBoiPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.storage = new MySqlStorage(
                getConfig().getString("storage.mysql.host"),
                Integer.parseInt(getConfig().getString("storage.mysql.port")),
                getConfig().getString("storage.mysql.database"),
                getConfig().getString("storage.mysql.username"),
                getConfig().getString("storage.mysql.password"),
                getConfig().getString("storage.mysql.table"),
                getConfig().getString("storage.mysql.luckperms.database"),
                getConfig().getString("storage.mysql.luckperms.playertable"),
                getConfig().getString("storage.mysql.luckperms.permtable"),
                getConfig().getString("storage.mysql.luckperms.grouppermtable"));

        String discordLink = getConfig().getString("discord_link");

        getCommand("discord").setExecutor(new DiscordCommand(discordLink));
    }

    @Override
    public void onDisable() {
        storage.close();
    }

    public Storage getStorage() {
        return storage;
    }

}
