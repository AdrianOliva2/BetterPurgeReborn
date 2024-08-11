package be.betterplugins.betterpurge.runnable;

import be.betterplugins.betterpurge.BetterPurge;
import be.betterplugins.betterpurge.messenger.BPLogger;
import be.betterplugins.betterpurge.messenger.Messenger;
import be.betterplugins.betterpurge.messenger.MsgEntry;
import be.betterplugins.betterpurge.model.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


/**
 * PurgeTimer class
 *
 * Reads in the purge timing settings (out of config.yaml)
 * decides to start or end the purge
 *
 */
public class PurgeStartScheduler extends BukkitRunnable
{

    private final PurgeHandler purgeHandler;
    private final PurgeConfiguration purgeConfig;
    private final Messenger messenger;
    private final BPLogger logger;
    private final BetterPurge plugin;

    /**
     * @param config gives the configuration file as parameter
     **/
    public PurgeStartScheduler(PurgeHandler purgeHandler, PurgeConfiguration config, Messenger messenger, BPLogger logger)
    {
        this.purgeConfig = config;
        this.messenger = messenger;
        this.purgeHandler = purgeHandler;
        this.logger = logger;
        this.plugin = BetterPurge.getInstance();
    }

    /**
     * Handles the purge state based on time
     */
    @Override
    public void run()
    {
        if (plugin.hasInitiated()) {
            BukkitTask startScheduler = plugin.getStartScheduler();
            if (startScheduler != null) {
                startScheduler.cancel();
                plugin.getServer().getScheduler().cancelTask(startScheduler.getTaskId());
            }
            return;
        }
        // Get the current time
        LocalDateTime localDateTime = LocalDateTime.now();
        PurgeTime timeNow = new PurgeTime( localDateTime.toLocalTime() );
        DayOfWeek day = localDateTime.getDayOfWeek();

        // Get the purge starting time
        PurgeStatus purgeStatus = purgeHandler.getPurgeStatus();
        PurgeTime purgeStart = purgeStatus.getPurgeConfiguration().getConfiguredStartTime();
        PurgeTime purgeEnd = purgeStart.addMinutes( purgeConfig.getDuration() );

        // get the time of the config file
        logger.log(Level.FINER,"TIME NOW IS: "          + timeNow);
        logger.log(Level.FINER,"PURGE START IS: "       + purgeStart );
        logger.log(Level.FINER,"PURGE END IS: "         + purgeEnd );
        logger.log(Level.FINER,"Current purge state: "  + purgeStatus.getState() );
        logger.log(Level.FINER,"Is here a purge today: "+ purgeConfig.isDayEnabled( day ) );


        // Enable the purge when in the time slot
        PurgeState state = purgeStatus.getState();
        switch (state)
        {
            case DISABLED:
                // Check if a countdown should start
                PurgeTime startCountdown = purgeStart.subtractMinutes( purgeConfig.getNumStartWarnings() );
                PurgeTime stopCountdown = purgeStart.subtractMinutes(1);
                if (purgeConfig.isDayEnabled( day ) && timeNow.isInRange(startCountdown, stopCountdown ))
                {
                    logger.log(Level.FINEST,"Minute purge start countdown");
                    List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                    messenger.sendMessage(
                            players,
                            "purge_start_countdown",
                            new MsgEntry("<duration>", purgeStart.compareTo( timeNow ))
                    );

                    if (timeNow.compareTo(stopCountdown) >= 0)
                    {
                        logger.log(Level.FINEST,"Starting countdown");
                        purgeStatus.setState( PurgeState.COUNTDOWN );
                    }
                }
                else
                {
                    logger.log(Level.FINEST,"Purge start countdown: No need to count down. Past start time? " + (timeNow.compareTo(startCountdown) >= 0) + ". Before end time? " + (timeNow.compareTo(stopCountdown) <= 0));
                }
                break;
            case COUNTDOWN:
                // Check if the purge should enable
                if (purgeConfig.isDayEnabled( day ) && timeNow.isInRange(purgeStart, purgeEnd))
                {
                    logger.log(Level.FINEST,"Enabling purge by time");
                    purgeHandler.startPurge( this.purgeConfig.getDuration() );
                    plugin.initiate();
                }
                else
                {
                    logger.log(Level.FINE,"The purge should not enable? This should never happen");
                }
                break;
            case ACTIVE:
                // Handled automatically by PurgeHandler
                break;
        }
    }

}


