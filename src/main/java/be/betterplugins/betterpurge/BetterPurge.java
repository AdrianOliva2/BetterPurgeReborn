package be.betterplugins.betterpurge;

import be.betterplugins.betterpurge.listener.ContainerListener;
import be.betterplugins.betterpurge.listener.PVPListener;
import be.betterplugins.betterpurge.messenger.BPLogger;
import be.betterplugins.betterpurge.messenger.Messenger;
import be.betterplugins.betterpurge.model.PurgeConfiguration;
import be.betterplugins.betterpurge.model.PurgeStatus;
import be.betterplugins.betterpurge.runnable.PurgeScheduler;
import be.dezijwegel.betteryaml.BetterLang;
import be.dezijwegel.betteryaml.OptionalBetterYaml;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
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

    // run this code when plugin should is enabled
    @Override
    public void onEnable()
    {

        BPLogger logger = new BPLogger(Level.ALL);

        // Initialise configuration

        OptionalBetterYaml betterYaml = new OptionalBetterYaml("config.yml", this, true);
        Optional<YamlConfiguration> optionalConfig = betterYaml.getYamlConfiguration();

        // Disable the plugin & prevent further code execution if a config error happens (this should never happen)
        if (!optionalConfig.isPresent())
        {
            logger.log(Level.SEVERE, ChatColor.RED + "BetterPurge cannot enable due to a configuration error, please contact the developer");
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        YamlConfiguration config = optionalConfig.get();
        PurgeConfiguration purgeConfig = new PurgeConfiguration(config);

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

        // Initialise runnables

        PurgeScheduler purgeScheduler = new PurgeScheduler(purgeStatus, purgeConfig, containerListener, messenger, logger, this);

        // run every mochnute
        purgeScheduler.runTaskTimer(this, 0L, 1200L);
    }

    // run this code when plugin should be disabled
    @Override
    public void onDisable()
    {

    }



}
