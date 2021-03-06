package com.gmail.nossr50.skills.gathering;

import java.util.Random;

import org.bukkit.CropState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.datatypes.AbilityType;
import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.datatypes.SkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.runnables.GreenThumbTimer;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.Skills;
import com.gmail.nossr50.util.Users;

public class Herbalism {

    private static Random random = new Random();

    /**
     * Activate the Green Terra ability.
     *
     * @param player The player activating the ability
     * @param block The block to be changed by Green Terra
     */
    public static void greenTerra(Player player, Block block) {
        PlayerInventory inventory = player.getInventory();
        boolean hasSeeds = inventory.contains(Material.SEEDS);

        if (!hasSeeds) {
            player.sendMessage("You need more seeds to spread Green Terra");  //TODO: Needs more locale.
        }
        else if (hasSeeds && !block.getType().equals(Material.WHEAT)) {
            inventory.removeItem(new ItemStack(Material.SEEDS));
            player.updateInventory();
            greenTerraConvert(player, block);
        }
    }
    
    public static void greenTerraConvert(Player player, Block block) {
        Material type = block.getType();
        
        if (Misc.blockBreakSimulate(block, player, false)) {
            if (Config.getInstance().getHerbalismGreenThumbSmoothbrickToMossy() && type.equals(Material.SMOOTH_BRICK)) {
                block.setData((byte) 0x1); //Set type of the brick to mossy
            }
            else if (Config.getInstance().getHerbalismGreenThumbDirtToGrass() && type.equals(Material.DIRT)) {
                block.setType(Material.GRASS);
            }
            else if (Config.getInstance().getHerbalismGreenThumbCobbleToMossy() && type.equals(Material.COBBLESTONE)) {
                block.setType(Material.MOSSY_COBBLESTONE);
            }
        }
    }

    /**
     * Check for extra Herbalism drops.
     *
     * @param block The block to check for extra drops
     * @param player The player getting extra drops
     * @param event The event to use for Green Thumb
     * @param plugin mcMMO plugin instance
     */
    public static void herbalismProcCheck(final Block block, Player player, BlockBreakEvent event, mcMMO plugin) {
        final PlayerProfile PP = Users.getProfile(player);
        final int MAX_BONUS_LEVEL = 1000;

        int herbLevel = PP.getSkillLevel(SkillType.HERBALISM);
        int id = block.getTypeId();
        Material type = block.getType();

        Byte data = block.getData();
        Location loc = block.getLocation();
        Material mat = null;
        int xp = 0;
        int catciDrops = 0;
        int caneDrops = 0;

        switch (type) {
        case BROWN_MUSHROOM:
        case RED_MUSHROOM:
            if (!block.hasMetadata("mcmmoPlacedBlock")) {
                mat = Material.getMaterial(id);
                xp = Config.getInstance().getHerbalismXPMushrooms();
            }
            break;

        case CACTUS:
            for (int y = 0;  y <= 2; y++) {
                Block b = block.getRelative(0, y, 0);
                if (b.getType().equals(Material.CACTUS)) {
                    mat = Material.CACTUS;
                    if (!b.hasMetadata("mcmmoPlacedBlock")) {
                        if (herbLevel > MAX_BONUS_LEVEL || random.nextInt(1000) <= herbLevel) {
                            catciDrops++;
                        }
                        xp += Config.getInstance().getHerbalismXPCactus();
                    }
                }
            }
            break;

        case CROPS:
            if (data == CropState.RIPE.getData()) {
                mat = Material.WHEAT;
                xp = Config.getInstance().getHerbalismXPWheat();

                if (Permissions.getInstance().greenThumbWheat(player)) {
                    greenThumbWheat(block, player, event, plugin);
                }
            }
            break;

        case MELON_BLOCK:
            if (!block.hasMetadata("mcmmoPlacedBlock")) {
                mat = Material.MELON;
                xp = Config.getInstance().getHerbalismXPMelon();
            }
            break;

        case NETHER_WARTS:
            if (data == (byte) 0x3) {
                mat = Material.NETHER_STALK;
                xp = Config.getInstance().getHerbalismXPNetherWart();
            }
            break;

        case PUMPKIN:
        case JACK_O_LANTERN:
            if (!block.hasMetadata("mcmmoPlacedBlock")) {
                mat = Material.getMaterial(id);
                xp = Config.getInstance().getHerbalismXPPumpkin();
            }
            break;

        case RED_ROSE:
        case YELLOW_FLOWER:
            if (!block.hasMetadata("mcmmoPlacedBlock")) {
                mat = Material.getMaterial(id);
                xp = Config.getInstance().getHerbalismXPFlowers();
            }
            break;

        case SUGAR_CANE_BLOCK:
            for (int y = 0;  y <= 2; y++) {
                Block b = block.getRelative(0, y, 0);
                if (b.getType().equals(Material.SUGAR_CANE_BLOCK)) {
                    mat = Material.SUGAR_CANE;
                    if (!b.hasMetadata("mcmmoPlacedBlock")) {
                        if (herbLevel > MAX_BONUS_LEVEL || random.nextInt(1000) <= herbLevel) {
                            caneDrops++;
                        }
                        xp += Config.getInstance().getHerbalismXPSugarCane();
                    }
                }
            }
            break;

        case VINE:
            if (!block.hasMetadata("mcmmoPlacedBlock")) {
                mat = type;
                xp = Config.getInstance().getHerbalismXPVines();
            }
            break;

        case WATER_LILY:
            if (!block.hasMetadata("mcmmoPlacedBlock")) {
                mat = type;
                xp = Config.getInstance().getHerbalismXPLilyPads();
            }
            break;

        default:
            break;
        }

        if (mat == null) {
            return;
        }

        if (Permissions.getInstance().herbalismDoubleDrops(player)) {
            ItemStack is = new ItemStack(mat);

            if (herbLevel > MAX_BONUS_LEVEL || random.nextInt(1000) <= herbLevel) {
                Config configInstance = Config.getInstance();

                switch (type) {
                case BROWN_MUSHROOM:
                    if (configInstance.getBrownMushroomsDoubleDropsEnabled()) {
                        Misc.mcDropItem(loc, is);
                    }
                    break;

                case CACTUS:
                    if (configInstance.getCactiDoubleDropsEnabled()) {
                        Misc.mcDropItems(loc, is, catciDrops);
                    }
                    break;

                case CROPS:
                    if (configInstance.getWheatDoubleDropsEnabled()) {
                        Misc.mcDropItem(loc, is);
                    }
                    break;

                case MELON_BLOCK:
                    if (configInstance.getMelonsDoubleDropsEnabled()) {
                        Misc.mcDropItems(loc, is, 3);
                        Misc.mcRandomDropItems(loc, is, 50, 4);
                    }
                    break;

                case NETHER_WARTS:
                    if (configInstance.getNetherWartsDoubleDropsEnabled()) {
                        Misc.mcDropItems(loc, is, 2);
                        Misc.mcRandomDropItems(loc, is, 50, 3);
                    }
                    break;

                case PUMPKIN:
                    if (configInstance.getPumpkinsDoubleDropsEnabled()) {
                        Misc.mcDropItem(loc, is);
                    }
                    break;

                case RED_MUSHROOM:
                    if (configInstance.getRedMushroomsDoubleDropsEnabled()) {
                        Misc.mcDropItem(loc, is);
                    }
                    break;

                case SUGAR_CANE_BLOCK:
                    if (configInstance.getSugarCaneDoubleDropsEnabled()) {
                        Misc.mcDropItems(loc, is, caneDrops);
                    }
                    break;

                case VINE:
                    if (configInstance.getVinesDoubleDropsEnabled()) {
                        Misc.mcDropItem(loc, is);
                    }
                    break;

                case WATER_LILY:
                    if (configInstance.getWaterLiliesDoubleDropsEnabled()) {
                        Misc.mcDropItem(loc, is);
                    }
                    break;

                case YELLOW_FLOWER:
                    if (configInstance.getYellowFlowersDoubleDropsEnabled()) {
                        Misc.mcDropItem(loc, is);
                    }
                    break;

                default:
                    break;
                }
            }
        }

        PP.addXP(SkillType.HERBALISM, xp);
        Skills.XpCheckSkill(SkillType.HERBALISM, player);
    }

    /**
     * Apply the Green Thumb ability to crops.
     *
     * @param block The block to apply the ability to
     * @param player The player using the ability
     * @param event The event triggering the ability
     * @param plugin mcMMO plugin instance
     */
    private static void greenThumbWheat(Block block, Player player, BlockBreakEvent event, mcMMO plugin) {
        final int MAX_BONUS_LEVEL = 1500;

        PlayerProfile PP = Users.getProfile(player);
        int herbLevel = PP.getSkillLevel(SkillType.HERBALISM);
        PlayerInventory inventory = player.getInventory();
        boolean hasSeeds = inventory.contains(Material.SEEDS);
        Location loc = block.getLocation();

        if (hasSeeds && PP.getAbilityMode(AbilityType.GREEN_TERRA) || hasSeeds && (herbLevel > MAX_BONUS_LEVEL || random.nextInt(1500) <= herbLevel)) {
            event.setCancelled(true);

            Misc.mcDropItem(loc, new ItemStack(Material.WHEAT));
            Misc.mcRandomDropItems(loc, new ItemStack(Material.SEEDS), 50, 3);

            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new GreenThumbTimer(block, PP), 1);

            inventory.removeItem(new ItemStack(Material.SEEDS));
            player.updateInventory();
        }
    }

    /**
     * Apply the Green Thumb ability to blocks.
     *
     * @param is The item in the player's hand
     * @param player The player activating the ability
     * @param block The block being used in the ability
     */
    public static void greenThumbBlocks(ItemStack is, Player player, Block block) {
        final int MAX_BONUS_LEVEL = 1500;

        PlayerProfile PP = Users.getProfile(player);
        int skillLevel = PP.getSkillLevel(SkillType.HERBALISM);
        int seeds = is.getAmount();

        player.setItemInHand(new ItemStack(Material.SEEDS, seeds - 1));

        if (skillLevel > MAX_BONUS_LEVEL || random.nextInt(1500) <= skillLevel) {
            greenTerraConvert(player, block);
        }
        else {
            player.sendMessage(LocaleLoader.getString("mcPlayerListener.GreenThumbFail"));
        }
    }
}
