package be.betterplugins.betterpurge.model;

import be.betterplugins.betterpurge.BetterPurge;
import be.betterplugins.betterpurge.messenger.BPLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.DayOfWeek;
import java.util.*;
import java.util.logging.Level;

public class PurgeConfiguration {

    private final Set<DayOfWeek> enabledDays;
    private static PurgeTime startTime;
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
        /*List<String> startTimeList = config.getStringList("start");
        Random rand = new Random();
        String startTime = startTimeList.get(rand.nextInt(0, startTimeList.size()-1));

        BetterPurge plugin = BetterPurge.getInstance();

        plugin.getLogger().log(Level.INFO, "Start time: " + startTime);*/

        int duration = config.getInt("duration");
        this.numStartWarnings = Math.max(0, config.getInt("num_start_warnings"));
        this.numStopWarnings = Math.max(0, config.getInt("num_stop_warnings"));
        this.handleContainers = config.getBoolean("enable_chests");
        this.overwriteSafezonePvp = config.getBoolean("overwrite_safezone");
        this.handlePVP = config.getBoolean("handle_pvp");

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
        //logger.log(Level.CONFIG, "Enabled days: " + enabledDays.size());

        this.blacklistedWorlds = new HashSet<>();
        List<String> worldsList = config.getStringList("blacklisted_worlds");
        if (!worldsList.isEmpty())
        {
            this.blacklistedWorlds.addAll(worldsList);
        }
        //logger.log(Level.CONFIG, "Blacklisted worlds: " + blacklistedWorlds.size());

        // Handle input & perform validation/correction
        //this.startTime = startTime != null ? new PurgeTime( startTime ) : new PurgeTime(21, 0);
        this.refreshStartTime(config);
        this.duration = Math.max( Math.min( duration, 1400), 2);

        logger.log(Level.CONFIG, "Purge start? " + startTime);
        logger.log(Level.CONFIG, "Purge duration? " + duration);
    }

    public static void refreshStartTime(YamlConfiguration config) {
        //List<String> startTimeList = config.getStringList("start");
        Random rand = new Random();
        //String startTime = startTimeList.get(rand.nextInt(0, startTimeList.size()-1));
        String fromStartTime = config.getString("start.from");
        String toStartTime = config.getString("start.to");

        if (fromStartTime == null || fromStartTime.isEmpty()) {
            fromStartTime = "21:00";
        }
        if (toStartTime == null || toStartTime.isEmpty()) {
            toStartTime = "22:00";
        }

        int fromHoursTime = Integer.parseInt(fromStartTime.split(":")[0]);
        int fromMinutesTime = Integer.parseInt(fromStartTime.split(":")[1]);

        int toHoursTime = Integer.parseInt(toStartTime.split(":")[0]);
        int toMinutesTime = Integer.parseInt(toStartTime.split(":")[1]);

        if (toMinutesTime == 0) {
            toMinutesTime = 59;
            toHoursTime -= 1;
        }

        String startTimeHour = String.valueOf(rand.nextInt(fromHoursTime, toHoursTime+1));
        String startTimeMinute = String.valueOf(rand.nextInt(fromMinutesTime, toMinutesTime+1));

        if (startTimeHour.length() == 1) {
            startTimeHour = "0" + startTimeHour;
        }

        if (startTimeMinute.length() == 1) {
            startTimeMinute = "0" + startTimeMinute;
        }

        String startTime = startTimeHour + ":" + startTimeMinute;

        //BetterPurge plugin = BetterPurge.getInstance();

        //plugin.getLogger().log(Level.INFO, "Start time: " + startTime);
        PurgeConfiguration.startTime = new PurgeTime( startTime );
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

    public boolean isWorldBlacklisted(String worldName) {
        //blacklistedWorlds.forEach(blackListedWorld -> Bukkit.getConsoleSender().sendMessage(blackListedWorld));
        return blacklistedWorlds.contains(worldName);
    }
}
