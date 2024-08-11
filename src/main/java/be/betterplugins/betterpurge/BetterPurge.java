package be.betterplugins.betterpurge;

import be.betterplugins.betterpurge.listener.ContainerListener;
import be.betterplugins.betterpurge.listener.PVPListener;
import be.betterplugins.betterpurge.listener.PlayerListener;
import be.betterplugins.betterpurge.messenger.BPLogger;
import be.betterplugins.betterpurge.messenger.Messenger;
import be.betterplugins.betterpurge.model.PurgeConfiguration;
import be.betterplugins.betterpurge.model.PurgeHandler;
import be.betterplugins.betterpurge.model.PurgeStatus;
import be.betterplugins.betterpurge.runnable.PurgeStartScheduler;
import be.dezijwegel.betteryaml.BetterLang;
import be.dezijwegel.betteryaml.OptionalBetterYaml;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.logging.Level;

/**
 *
 * BetterPurge Plugin
 * @author Thomas Verschoor
 *
 **/
public class BetterPurge extends JavaPlugin
{

    public static BetterPurge getInstance() {
        return getPlugin(BetterPurge.class);
    }

    private PurgeStartScheduler purgeStartScheduler;
    YamlConfiguration config;
    private NamespacedKey bossBarKey;
    private PurgeHandler purgeHandler;
    private Boolean initiated;
    private BukkitTask startScheduler;

    @Override
    public void onEnable()
    {
        initiated = false;
        bossBarKey = new NamespacedKey(this, "betterpurge_bossbar");
        BPLogger logger = new BPLogger(Level.WARNING);

        // Initialise configuration

        OptionalBetterYaml betterYaml = new OptionalBetterYaml("config.yml", this, true);
        Optional<YamlConfiguration> optionalConfig = betterYaml.getYamlConfiguration();
        config = optionalConfig.orElse(null);

        // Disable the plugin & prevent further code execution if a config error happens (this should never happen)
        if (!optionalConfig.isPresent())
        {
            logger.log(Level.SEVERE, ChatColor.RED + "BetterPurge cannot enable due to a configuration error, please contact the developer");
            this.getPluginLoader().disablePlugin(this);
            return;
        }


        PurgeConfiguration purgeConfig = new PurgeConfiguration(config, logger);

        // Initialising localisation

        String localised = config.getString("lang") != null ? config.getString("lang") : "en-US";
        assert localised != null;
        BetterLang betterLang = new BetterLang("lang.yml", localised.toLowerCase() + ".yml", this, true);

        if (!betterLang.getYamlConfiguration().isPresent())
        {
            logger.log(Level.WARNING, "Language '" + localised + "' not found. Reverting to default: 'en-US'");
            betterLang = new BetterLang("lang.yml", "en-us.yml", this);
        }

        // Initialise util objects

        PurgeStatus purgeStatus = new PurgeStatus(purgeConfig);
        Messenger messenger = new Messenger(betterLang.getMessages(), logger, true);

        // Initialise listeners

        ContainerListener containerListener = new ContainerListener(purgeStatus, purgeConfig, messenger, logger);
        Bukkit.getServer().getPluginManager().registerEvents(containerListener, this );

        PVPListener pvpListener = new PVPListener(purgeStatus, purgeConfig, logger);
        Bukkit.getServer().getPluginManager().registerEvents(pvpListener, this);

        PlayerListener playerListener = new PlayerListener(purgeConfig);
        Bukkit.getServer().getPluginManager().registerEvents(playerListener, this);

        // Initialise purge handler
        purgeHandler = new PurgeHandler(purgeStatus, containerListener, purgeConfig, messenger, logger, this);

        // Initialise runnables

        purgeStartScheduler = new PurgeStartScheduler(purgeHandler, purgeConfig, messenger, logger);

        // run every mochnute
        startScheduler = purgeStartScheduler.runTaskTimer(this, 0L, 1200L);

        // Initialise command handler
        CommandHandler commandHandler = new CommandHandler(messenger, logger, purgeHandler, this);
        this.getCommand("betterpurge").setExecutor( commandHandler );

        MetricsHandler metricsHandler = new MetricsHandler(this, config);
    }

    public YamlConfiguration getPluginConfig()
    {
        return config;
    }

    public NamespacedKey getBossBarKey()
    {
        return bossBarKey;
    }

    public PurgeHandler getPurgeHandler()
    {
        return purgeHandler;
    }

    public BukkitTask getStartScheduler()
    {
        return startScheduler;
    }

    public Boolean hasInitiated()
    {
        return initiated;
    }

    public void initiate()
    {
        this.initiated = true;
    }

    @Override
    public void onDisable()
    {
        if (this.purgeStartScheduler != null && !this.purgeStartScheduler.isCancelled())
            this.purgeStartScheduler.cancel();
        HandlerList.unregisterAll(this);
    }


    public void reload()
    {
        this.onDisable();
        this.onEnable();
    }
}
