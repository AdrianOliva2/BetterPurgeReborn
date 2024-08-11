package be.betterplugins.betterpurge.listener;

import be.betterplugins.betterpurge.BetterPurge;
import be.betterplugins.betterpurge.model.PurgeConfiguration;
import be.betterplugins.betterpurge.model.PurgeHandler;
import be.betterplugins.betterpurge.model.PurgeState;
import be.betterplugins.betterpurge.model.PurgeStatus;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    BetterPurge plugin;
    PurgeConfiguration purgeConfig;

    public PlayerListener(PurgeConfiguration purgeConfig) {
        plugin = BetterPurge.getInstance();
        this.purgeConfig = purgeConfig;
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        BossBar bossBar = plugin.getServer().getBossBar(plugin.getBossBarKey());
        if (purgeConfig.isWorldBlacklisted(event.getPlayer().getWorld().getName())){
            if (bossBar != null)
                bossBar.removePlayer(event.getPlayer());
            return;
        }

        PurgeHandler purgeHandler = plugin.getPurgeHandler();
        PurgeStatus purgeStatus = purgeHandler.getPurgeStatus();
        PurgeState state = purgeStatus.getState();

        if (state == PurgeState.ACTIVE && bossBar != null) {
            Player player = event.getPlayer();
            bossBar.addPlayer(player);
            bossBar.setVisible(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (purgeConfig.isWorldBlacklisted(event.getPlayer().getWorld().getName()))
            return;

        PurgeHandler purgeHandler = plugin.getPurgeHandler();
        PurgeStatus purgeStatus = purgeHandler.getPurgeStatus();
        PurgeState state = purgeStatus.getState();
        BossBar bossBar = plugin.getServer().getBossBar(plugin.getBossBarKey());

        if (state == PurgeState.ACTIVE && bossBar != null) {
            Player player = event.getPlayer();
            bossBar.addPlayer(player);
            bossBar.setVisible(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (purgeConfig.isWorldBlacklisted(event.getPlayer().getWorld().getName()))
            return;

        PurgeHandler purgeHandler = plugin.getPurgeHandler();
        PurgeStatus purgeStatus = purgeHandler.getPurgeStatus();
        PurgeState state = purgeStatus.getState();
        BossBar bossBar = plugin.getServer().getBossBar(plugin.getBossBarKey());

        if (state == PurgeState.ACTIVE && bossBar != null) {
            Player player = event.getPlayer();
            bossBar.removePlayer(player);
        }
    }

}
