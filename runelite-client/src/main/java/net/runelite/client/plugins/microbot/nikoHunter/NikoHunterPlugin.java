package net.runelite.client.plugins.microbot.nikoHunter;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.time.Instant;

/**
 * Main plugin class for automated Kebbit hunting using a falcon.
 * Handles configuration, overlay management, event subscriptions, and script lifecycle.
 */
@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "Nikol Hunter",
        description = "Nikol Hunter",
        tags = {"hunter", "custom", "skilling"},
        enabledByDefault = false
)
public class NikoHunterPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private NikoHunterConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private NikoHunterOverlay nikoHunterOverlay;

    private NikoHunterScript script;
    private Instant scriptStartTime;

    /**
     * Provides the plugin configuration to RuneLite's config manager.
     *
     * @param configManager the config manager instance
     * @return configuration for this plugin
     */
    @Provides
    NikoHunterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(NikoHunterConfig.class);
    }

    /**
     * Called when the plugin is started. Initializes script and overlay.
     */
    @Override
    protected void startUp() {
        scriptStartTime = Instant.now();
        overlayManager.add(nikoHunterOverlay);
        script = new NikoHunterScript();
        script.run(this);
    }

    /**
     * Called when the plugin is shut down. Cleans up overlay and script.
     */
    @Override
    protected void shutDown() {
        scriptStartTime = null;
        overlayManager.remove(nikoHunterOverlay);
        if (script != null) {
            script.shutdown();
        }
    }

    /**
     * Returns the formatted runtime of the script.
     *
     * @return Duration since the script was started.
     */
    public String getTimeRunning() {
        return scriptStartTime != null
                ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now())
                : "";
    }
}
