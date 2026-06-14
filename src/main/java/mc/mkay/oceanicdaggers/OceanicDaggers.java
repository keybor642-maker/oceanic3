package mc.mkay.oceanicdaggers;

import org.bukkit.plugin.java.JavaPlugin;

public class OceanicDaggers extends JavaPlugin {

    private static OceanicDaggers instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new DaggerListener(this), this);
        getCommand("givedaggers").setExecutor(new DaggerCommand(this));
        getCommand("givedaggeroff").setExecutor(new DaggerCommand(this));
        getLogger().info("[OceanicDaggers] Enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[OceanicDaggers] Disabled.");
    }

    public static OceanicDaggers getInstance() { return instance; }
}
