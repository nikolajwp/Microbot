package net.runelite.client.plugins.microbot.nikoHunter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.nikoHunter.State.BONES_TO_BANANAS;
import static net.runelite.client.plugins.microbot.nikoHunter.State.SETTING_TRAP;
import static net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject.convertToObjectComposition;

@Slf4j
public class NikoHunterScript extends Script {

    private static final int BOULDER_ID = 28824;
    private static final List<WorldPoint> BONES_LOCATIONS = List.of(
            new WorldPoint(2911, 9130, 0),
            new WorldPoint(2914, 9125, 0),
            new WorldPoint(2907, 9127, 0));
    private static final String BONE_NAME = "Bones";
    private static final int BONE_ID = 526;
    private static final int BANANA_ID = 1963;
    private static final int TAILS_ID = 19665;

    public static int MonkeyTailsCaught = 0;
    public static int MonkeyTailsFailed = 0;

    @Getter
    private State currentState = SETTING_TRAP;

    public void run(NikoHunterPlugin plugin) {
        Rs2Antiban.resetAntibanSettings();
        applyAntiBanSettings();
        Rs2Antiban.setActivity(Activity.GENERAL_HUNTER);
        MonkeyTailsCaught = 0;
        MonkeyTailsFailed = 0;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !this.isRunning() || Microbot.status.equals("stopping")) return;

                if (Rs2Inventory.count(BANANA_ID) < 1) {
                    return;
                }

                switch (currentState) {
                    case SETTING_TRAP:
                        handleSettingTrap();
                        break;
                    case BONES_TO_BANANAS:
                        handleBonesToBananas();
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Microbot.status = "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
    }

    private void handleBonesToBananas() {
        log.info("handleBonesToBananas");
        sleep(3600, 8000);
        Rs2Inventory.interact(8014, "Break");
        currentState = SETTING_TRAP;
    }

    private void handleDropTails() {
        log.info("handleDropTails");
        Rs2Inventory.dropAll(TAILS_ID);
        currentState = SETTING_TRAP;
    }

    private void handleSettingTrap() {
        WorldPoint trapLocation = new WorldPoint(2910, 9127, 0);
        String actionSetTrap = "Set-trap";
        String actionDismantle = "Dismantle";
        String actionCheck = "Check";

        log.info("Waiting to set up trap");
        sleepUntil(() -> Rs2GameObject.hasAction(convertToObjectComposition(Rs2GameObject.getGameObject(trapLocation)), actionSetTrap), 50000);
        sleep(100, 1300);
        log.info("Setting up trap");
        Rs2GameObject.interact(trapLocation, actionSetTrap);

        sleepUntil(() -> Rs2GameObject.hasAction(convertToObjectComposition(Rs2GameObject.getGameObject(trapLocation)), actionDismantle), 5000);
        sleep(100, 1300);
        log.info("Done setting up trap");

        checkIfPlayersNearby();

        if (Rs2Inventory.count(TAILS_ID) > 1 + new Random().nextInt(6)) {
            handleDropTails();
            sleep(500, 900);
        } else if (Rs2Inventory.emptySlotCount() > 4 + new Random().nextInt(3)) {
            handlePickingBones();
        }

        sleep(1500, 4000);

        sleepUntil(() -> {
            var gameObject = convertToObjectComposition(Rs2GameObject.getGameObject(trapLocation));
            return Rs2GameObject.hasAction(gameObject, actionSetTrap) || Rs2GameObject.hasAction(gameObject, actionCheck);
        }, 70000);

        sleep(100, 7500);
        simulateBreaks();

        if (Rs2GameObject.hasAction(convertToObjectComposition(Rs2GameObject.getGameObject(trapLocation)), actionCheck)) {
            log.info("Checking trap");
            Rs2GameObject.interact(trapLocation, actionCheck);
            MonkeyTailsCaught++;
        } else {
            log.info("Skipping trap because it failed");
            MonkeyTailsFailed++;
        }

        if (Rs2Inventory.count(BONE_ID) > 9 + new Random().nextInt(10)
                || Rs2Inventory.count(BANANA_ID) < 2) {
            currentState = BONES_TO_BANANAS;
        }
    }

    private void handlePickingBones() {
        for (WorldPoint location : BONES_LOCATIONS) {
            Rs2GroundItem.lootItemsBasedOnLocation(location, BONE_ID);

            sleepUntil(() -> !Rs2GroundItem.getGroundItems()
                    .cellSet()
                    .stream()
                    .anyMatch(cell ->
                            cell.getColumnKey() == BONE_ID &&
                                    cell.getRowKey().equals(location)), 5000);
            sleep(0, 250);
            if (new Random().nextInt(4) == 0) {
                sleep(0, 1500);
            }
        }
        currentState = SETTING_TRAP;
    }

    private void checkIfPlayersNearby() {
        if (Rs2Player.getPlayers(player ->
                player.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= 2
        ).count() > 0) {
            log.info("Players are nearby -> Waiting");
            sleep(80000, 160000);
            if (Rs2Player.getPlayers(player ->
                    player.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= 2
            ).count() > 0) {
                log.info("Players are still nearby -> Exiting");
                Rs2Player.logoutIfPlayerDetected(1, 10000, 3);
                Microbot.status = "stopping";
            }
        }
    }

    private void simulateBreaks() {
        int oneSec = 1000;
        if (new Random().nextInt(9) == 0) {
            log.info("Going to sleep (1)");
            sleep(oneSec, 10 * oneSec);
            if (new Random().nextInt(2) == 0) {
                log.info("Going to sleep (2)");
                sleep(oneSec, 15 * oneSec);
            }
            if (new Random().nextInt(5) == 0) {
                log.info("Going to sleep (3)");
                sleep(2 * oneSec, 25 * oneSec);
            }
            if (new Random().nextInt(7) == 0) {
                log.info("Going to sleep (4)");
                sleep(2 * oneSec, 25 * oneSec);
            }
            if (new Random().nextInt(10) == 0) {
                log.info("Going to sleep (5)");
                sleep(2 * oneSec, 150 * oneSec);
            }
        } else if (new Random().nextInt(55) == 0) {
            log.info("Going to sleep LONG (1)");
            sleep(30 * oneSec, 190 * oneSec);
            if (new Random().nextInt(3) == 0) {
                log.info("Going to sleep LONG (2)");
                sleep(200 * oneSec, 450 * oneSec);
                if (new Random().nextInt(4) == 0) {
                    log.info("Going to sleep LONG (3)");
                    sleep(100 * oneSec, 850 * oneSec);
                }
            }
        }
    }

    private void applyAntiBanSettings() {
        Rs2AntibanSettings.antibanEnabled = true;
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = true;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.contextualVariability = true;
        Rs2AntibanSettings.devDebug = false;
        Rs2AntibanSettings.playSchedule = true;
        Rs2AntibanSettings.actionCooldownChance = 0.1;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        MonkeyTailsCaught = 0;
        Microbot.status = "Script stopped.";
    }
}
