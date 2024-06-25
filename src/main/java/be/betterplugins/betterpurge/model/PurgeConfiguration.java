package be.betterplugins.betterpurge.model;

import be.betterplugins.betterpurge.messenger.BPLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.DayOfWeek;
import java.util.*;
import java.util.logging.Level;

public class PurgeConfiguration {

    private final Set<DayOfWeek> enabledDays;
    private final PurgeTime startTime;
    private final int duration;

    private final int numStartWarnings;
    private final int numStopWarnings;

    private final boolean handleContainers;
    private final boolean overwriteSafezonePvp;
    private final boolean handlePVP;
    private final Set<String> blacklistedWorlds;

    /**
     * Wrapper around our config file. This allows easy input validation in one place
     *
     * @param config config.yml, which contains the settings
     */
    public PurgeConfiguration(YamlConfiguration config, BPLogger logger)
    {
        // Read all settings from the config file
        String startTime    = config.getString("start");
        int duration        = config.getInt("duration");
        this.numStartWarnings = Math.max(0, config.getInt("num_start_warnings"));
        this.numStopWarnings = Math.max(0, config.getInt("num_stop_warnings"));
        this.handleContainers       = config.getBoolean("enable_chests");
        this.overwriteSafezonePvp   = config.getBoolean("overwrite_safezone");
        this.handlePVP              = config.getBoolean("handle_pvp");

        logger.log(Level.CONFIG, "Start time: " + startTime);
        logger.log(Level.CONFIG, "Handle containers? " + handleContainers);
        logger.log(Level.CONFIG, "Handle pvp? " + handlePVP);
        logger.log(Level.CONFIG, "Overwrite pvp zones? " + overwriteSafezonePvp);

        this.enabledDays = new HashSet<>();
        ConfigurationSection daysSection = config.getConfigurationSection("enabled_days");
        if (daysSection != null)
        {
            for (String path : daysSection.getKeys(false))
                if (daysSection.getBoolean(path))
                    this.enabledDays.add(DayOfWeek.valueOf(path.toUpperCase()));
        }
        else
        {
            this.enabledDays.addAll(Arrays.asList(DayOfWeek.values()));
        }
        logger.log(Level.CONFIG, "Enabled days: " + enabledDays.size());

        this.blacklistedWorlds = new HashSet<>();
        ConfigurationSection worldsSection = config.getConfigurationSection("blacklisted_worlds");
        if (worldsSection != null)
        {
            this.blacklistedWorlds.addAll(worldsSection.getKeys(false));
        }
        logger.log(Level.CONFIG, "Blacklisted worlds: " + blacklistedWorlds.size());

        // Handle input & perform validation/correction
        this.startTime = startTime != null ? new PurgeTime( startTime ) : new PurgeTime(21, 0);
        this.duration = Math.max( Math.min( duration, 1400), 2);

        logger.log(Level.CONFIG, "Purge start? " + startTime);
        logger.log(Level.CONFIG, "Purge duration? " + duration);
    }

    public PurgeTime getConfiguredStartTime()
    {
        return startTime;
    }

    public boolean isDayEnabled(DayOfWeek day)
    {
        return this.enabledDays.contains( day );
    }

    public int getDuration()
    {
        return duration;
    }

    public boolean shouldOverwriteSafezonePvp()
    {
        return overwriteSafezonePvp;
    }

    public boolean shouldHandleContainers()
    {
        return handleContainers;
    }

    public boolean shouldHandlePVP()
    {
        return handlePVP;
    }

    public int getNumStartWarnings()
    {
        return numStartWarnings;
    }

    public int getNumStopWarnings()
    {
        return numStopWarnings;
    }

    public boolean isWorldBlacklisted(String worldName) { return blacklistedWorlds.contains(worldName); }
}
