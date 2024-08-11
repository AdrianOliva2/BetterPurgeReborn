package be.betterplugins.betterpurge.model;

import be.betterplugins.betterpurge.BetterPurge;
import be.betterplugins.betterpurge.listener.ContainerListener;
import be.betterplugins.betterpurge.messenger.BPLogger;
import be.betterplugins.betterpurge.messenger.Messenger;
import be.betterplugins.betterpurge.messenger.MsgEntry;
import be.betterplugins.betterpurge.runnable.BossBarUpdateProgressRunnable;
import be.betterplugins.betterpurge.runnable.CountdownRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class PurgeHandler
{

    private final PurgeStatus purgeStatus;
    private final ContainerListener containerListener;
    private final PurgeConfiguration purgeConfig;
    private final BetterPurge plugin;
    private final Messenger messenger;
    private final BPLogger logger;

    private CountdownRunnable startCounter;
    private CountdownRunnable stopCounter;

    public PurgeHandler(PurgeStatus purgeStatus, ContainerListener containerListener, PurgeConfiguration purgeConfig, Messenger messenger, BPLogger logger, BetterPurge plugin)
    {
        this.purgeStatus = purgeStatus;
        this.containerListener = containerListener;
        this.purgeConfig = purgeConfig;
        this.plugin = plugin;
        this.messenger = messenger;
        this.logger = logger;
    }

    public PurgeStatus getPurgeStatus()
    {
        return purgeStatus;
    }

    /**
     * Start the purge and end it at the duration from the settings
     */
    public void startPurge()
    {
        this.startPurge(0);
    }

    /**
     * Start the purge and automatically end it after a given amount of minutes
     *
     * @param duration the duration in minutes
     */
    public void startPurge(int duration)
    {
        if (purgeStatus.getState() == PurgeState.ACTIVE)
        {
            logger.log(Level.FINE, "Tried enabling the purge while it was already active");
            return;
        }

        purgeStatus.setState( PurgeState.COUNTDOWN );

        logger.log(Level.FINEST,"Enabling the purge...");

        int purgeDuration = duration > 0 ? duration : purgeConfig.getDuration();

        startCounter = new CountdownRunnable(
                10,
                (int count) -> {
                    logger.log(Level.FINEST, "Countdown: " + count);
                    // SHOW 10 second countdown
                    String message = messenger.composeMessage(
                            "seconds_countdown",
                            new MsgEntry("<duration>", count)
                    );
                    if (!message.isEmpty())
                        for (Player player : Bukkit.getOnlinePlayers())
                            player.sendTitle("", message, 5, 10, 5);
                },
                (int count) -> {
                    logger.log(Level.FINEST, "Countdown done: " + count);
                    List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                    messenger.sendMessage(
                            players,
                            "purge_start",
                            new MsgEntry("<duration>", purgeDuration)
                    );
                    BossBar bossBar = plugin.getServer().createBossBar(plugin.getBossBarKey(),ChatColor.translateAlternateColorCodes('&', "&c&lLA PURGA ESTÁ ACTIVA"), BarColor.RED, BarStyle.SOLID);
                    bossBar.setProgress(1.0);
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.closeInventory();
                        if (!purgeConfig.isWorldBlacklisted(player.getWorld().getName()))
                            bossBar.addPlayer(player);
                    });
                    bossBar.setVisible(true);
                    final int[] seconds = {purgeDuration * 60};
                    BossBarUpdateProgressRunnable bossBarUpdateProgressRunnable = new BossBarUpdateProgressRunnable(plugin, bossBar, seconds, purgeDuration * 60);
                    bossBarUpdateProgressRunnable.start();
                    purgeStatus.setState( PurgeState.ACTIVE );
                    this.stopPurge( purgeDuration );
                }
        );
        startCounter.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Stop the purge at once
     */
    public boolean stopPurge()
    {
        if (purgeStatus.getState() == PurgeState.DISABLED)
        {
            logger.log(Level.FINE, "Tried disabling the purge while it was already disabled");
            return false;
        }

        logger.log(Level.FINEST,"Disabling the purge");

        cancelCounter(startCounter);
        cancelCounter(stopCounter);

        // Close all opened purge inventories
        this.containerListener.closeAll();
        // Notify all players
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        messenger.sendMessage(players, "purge_ended");
        BossBar bossBar = plugin.getServer().getBossBar(plugin.getBossBarKey());
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAll();
        }
        plugin.getServer().removeBossBar(plugin.getBossBarKey());
        // Update the purge's state
        purgeStatus.setState(PurgeState.DISABLED);

        PurgeConfiguration.refreshStartTime(plugin.getPluginConfig());

        return true;
    }


    /**
     * Stop the purge after a given delay in minutes
     *
     * @param minutes the amount of minutes until the purge should disable
     */
    public void stopPurge(int minutes)
    {
        cancelCounter(stopCounter);

        stopCounter = new CountdownRunnable(
            minutes,
            (int remainingMinutes) ->
            {
                if (remainingMinutes <= purgeConfig.getNumStopWarnings())
                {
                    logger.log(Level.FINEST,"Almost disabling the purge, but not yet");
                    List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                    messenger.sendMessage(
                            players,
                            "purge_end_countdown",
                            new MsgEntry("<duration>", remainingMinutes)
                    );
                }
                else
                {
                    logger.log(Level.FINEST,"The purge has more than " + purgeConfig.getNumStopWarnings() + " minutes remaining");
                }
            },
            (int zero) ->
            {
                logger.log(Level.FINEST,"Disabling purge because the countdown reached zero");
                this.stopPurge();
            }
        );
        stopCounter.runTaskTimer(plugin, 0L, 1200L);
    }


    private void cancelCounter(CountdownRunnable counter)
    {
        if (counter != null)
        {
            try
            {
                counter.cancel();
            }
            catch (IllegalStateException ignored) {}
        }
    }
}
