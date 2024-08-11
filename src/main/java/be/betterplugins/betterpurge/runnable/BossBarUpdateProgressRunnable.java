package be.betterplugins.betterpurge.runnable;

import be.betterplugins.betterpurge.BetterPurge;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class BossBarUpdateProgressRunnable {

    private BukkitTask task;
    private final BetterPurge plugin;
    private final BossBar bossBar;
    private final int[] seconds;
    private final int purgeDuration;

    public BossBarUpdateProgressRunnable(BetterPurge plugin, BossBar bossBar, int[] seconds, int purgeDuration)
    {
        this.plugin = plugin;
        this.bossBar = bossBar;
        this.seconds = seconds;
        this.purgeDuration = purgeDuration;
    }

    public void start()
    {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        task = scheduler.runTaskTimerAsynchronously(plugin, () -> {
            if (seconds[0] > 0){
                bossBar.setProgress((double) seconds[0] / purgeDuration);
                seconds[0]--;
            }
            else{
                bossBar.setVisible(false);
                task.cancel();
            }
        }, 0L, 20L);
    }

}
