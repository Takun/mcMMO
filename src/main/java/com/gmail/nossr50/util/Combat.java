package com.gmail.nossr50.util;

import org.bukkit.Material;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.config.Config;
import com.gmail.nossr50.datatypes.AbilityType;
import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.datatypes.SkillType;
import com.gmail.nossr50.datatypes.ToolType;
import com.gmail.nossr50.events.fake.FakeEntityDamageByEntityEvent;
import com.gmail.nossr50.events.fake.FakeEntityDamageEvent;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.party.Party;
import com.gmail.nossr50.runnables.GainXp;
import com.gmail.nossr50.runnables.BleedTimer;
import com.gmail.nossr50.skills.combat.Archery;
import com.gmail.nossr50.skills.combat.Axes;
import com.gmail.nossr50.skills.combat.Swords;
import com.gmail.nossr50.skills.combat.Taming;
import com.gmail.nossr50.skills.combat.Unarmed;
import com.gmail.nossr50.skills.misc.Acrobatics;

public class Combat {
    private static Config configInstance = Config.getInstance();
    private static Permissions permInstance = Permissions.getInstance();

    /**
     * Apply combat modifiers and process and XP gain.
     *
     * @param event The event to run the combat checks on.
     * @param plugin mcMMO plugin instance
     */
    public static void combatChecks(EntityDamageByEntityEvent event, mcMMO plugin) {
        if (event.getDamage() == 0 || event.getEntity().isDead()) {
            return;
        }

        Entity damager = event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();
        EntityType damagerType = damager.getType();
        EntityType targetType = target.getType();

        switch (damagerType) {
        case PLAYER:
            Player attacker = (Player) event.getDamager();
            ItemStack itemInHand = attacker.getItemInHand();
            PlayerProfile PPa = Users.getProfile(attacker);

            combatAbilityChecks(attacker);

            if (ItemChecks.isSword(itemInHand) && permInstance.swords(attacker)) {
                if (!configInstance.getSwordsPVP()) {
                    if (targetType.equals(EntityType.PLAYER) || (targetType.equals(EntityType.WOLF) && ((Wolf) target).isTamed())) {
                        return;
                    }
                }

                if (!configInstance.getSwordsPVE()) {
                    if (!targetType.equals(EntityType.PLAYER) || !(targetType.equals(EntityType.WOLF) && ((Wolf) target).isTamed())) {
                        return;
                    }
                }

                if (permInstance.swordsBleed(attacker)) {
                    Swords.bleedCheck(attacker, target, plugin);
                }

                if (PPa.getAbilityMode(AbilityType.SERRATED_STRIKES) && permInstance.serratedStrikes(attacker)) {
                    applyAbilityAoE(attacker, target, event.getDamage() / 4, plugin, SkillType.SWORDS);
                    BleedTimer.add(target, 5);
                }

                startGainXp(attacker, PPa, target, SkillType.SWORDS, plugin);
            }
            else if (ItemChecks.isAxe(itemInHand) && permInstance.axes(attacker)) {
                if (!configInstance.getAxesPVP()) {
                    if (targetType.equals(EntityType.PLAYER) || (targetType.equals(EntityType.WOLF) && ((Wolf) target).isTamed())) {
                        return;
                    }
                }

                if (!configInstance.getAxesPVE()) {
                    if (!targetType.equals(EntityType.PLAYER) || !(targetType.equals(EntityType.WOLF) && ((Wolf) target).isTamed())) {
                        return;
                    }
                }

                if (permInstance.axeBonus(attacker)) {
                    Axes.axesBonus(attacker, event);
                }

                if (permInstance.criticalHit(attacker)) {
                    Axes.axeCriticalCheck(attacker, event);
                }

                if (permInstance.impact(attacker)) {
                    Axes.impact(attacker, target, event);
                }
 
                if (PPa.getAbilityMode(AbilityType.SKULL_SPLIITER) && permInstance.skullSplitter(attacker)) {
                    applyAbilityAoE(attacker, target, event.getDamage() / 2, plugin, SkillType.AXES);
                }

                startGainXp(attacker, PPa, target, SkillType.AXES, plugin);
            }
            else if (itemInHand.getType().equals(Material.AIR) && permInstance.unarmed(attacker)) {
                if (!configInstance.getUnarmedPVP()) {
                    if (targetType.equals(EntityType.PLAYER) || (targetType.equals(EntityType.WOLF) && ((Wolf) target).isTamed())) {
                        return;
                    }
                }

                if (!configInstance.getUnarmedPVE()) {
                    if (!targetType.equals(EntityType.PLAYER) || !(targetType.equals(EntityType.WOLF) && ((Wolf) target).isTamed())) {
                        return;
                    }
                }

                if (permInstance.unarmedBonus(attacker)) {
                    Unarmed.unarmedBonus(PPa, event);
                }

                if (PPa.getAbilityMode(AbilityType.BERSERK) && permInstance.berserk(attacker)) {
                    event.setDamage((int) (event.getDamage() * 1.5));
                }

                if (targetType.equals(EntityType.PLAYER) && permInstance.disarm(attacker)) {
                    Unarmed.disarmProcCheck(attacker, (Player) target);
                }

                startGainXp(attacker, PPa, target, SkillType.UNARMED, plugin);
            }
            else if (itemInHand.getType().equals(Material.BONE) && permInstance.beastLore(attacker)) {
                Taming.beastLore(event, target, attacker);
            }
            break;

        case WOLF:
            Wolf wolf = (Wolf) damager;

            if (wolf.isTamed() && wolf.getOwner() instanceof Player) {
                Player master = (Player) wolf.getOwner();
                PlayerProfile PPo = Users.getProfile(master);

                if (!configInstance.getTamingPVP()) {
                    if (targetType.equals(EntityType.PLAYER) || (targetType.equals(EntityType.WOLF) && ((Wolf) target).isTamed())) {
                        return;
                    }
                }

                if (!configInstance.getTamingPVE()) {
                    if (!targetType.equals(EntityType.PLAYER) || !(targetType.equals(EntityType.WOLF) && ((Wolf) target).isTamed())) {
                        return;
                    }
                }

                if (permInstance.taming(master)) {
                    if (permInstance.fastFoodService(master)) {
                        Taming.fastFoodService(PPo, wolf, event);
                    }

                    if (permInstance.sharpenedClaws(master)) {
                        Taming.sharpenedClaws(PPo, event);
                    }

                    if (permInstance.gore(master)) {
                        Taming.gore(PPo, event, master, plugin);
                    }

                    startGainXp(master, PPo, target, SkillType.TAMING, plugin);
                }
            }
            break;

        case ARROW:
            if (!configInstance.getArcheryPVP() && ((Arrow) damager).getShooter().getType().equals(EntityType.PLAYER)) {
                if (targetType.equals(EntityType.PLAYER) || (targetType.equals(EntityType.WOLF) && ((Wolf) target).isTamed())) {
                    return;
                }
            }

            if (!configInstance.getArcheryPVE() && !((Arrow) damager).getShooter().getType().equals(EntityType.PLAYER)) {
                if (!targetType.equals(EntityType.PLAYER) || !(targetType.equals(EntityType.WOLF) && ((Wolf) target).isTamed())) {
                    return;
                }
            }

            archeryCheck(event, plugin);
            break;
        }

        if (targetType.equals(EntityType.PLAYER)) {
            if (configInstance.getSwordsPVP() && damagerType.equals(EntityType.PLAYER)) {
                Swords.counterAttackChecks(event);
            }

            if (configInstance.getSwordsPVE() && !damagerType.equals(EntityType.PLAYER)) {
                Swords.counterAttackChecks(event);
            }

            if (configInstance.getAcrobaticsPVP() && damagerType.equals(EntityType.PLAYER)) {
                Acrobatics.dodgeChecks(event);
            }

            if (configInstance.getAcrobaticsPVE() && !damagerType.equals(EntityType.PLAYER)) {
                Acrobatics.dodgeChecks(event);
            }
        }
    }

    /**
     * Process combat abilities based on weapon preparation modes.
     *
     * @param attacker The player attacking
     */
    public static void combatAbilityChecks(Player attacker) {
        PlayerProfile PPa = Users.getProfile(attacker);

        if (PPa.getToolPreparationMode(ToolType.AXE)) {
            Skills.abilityCheck(attacker, SkillType.AXES);
        }
        else if (PPa.getToolPreparationMode(ToolType.SWORD)) {
            Skills.abilityCheck(attacker, SkillType.SWORDS);
        }
        else if (PPa.getToolPreparationMode(ToolType.FISTS)) {
            Skills.abilityCheck(attacker, SkillType.UNARMED);
        }
    }

    /**
     * Process archery abilities.
     *
     * @param event The event to run the archery checks on.
     * @param pluginx mcMMO plugin instance
     */
    public static void archeryCheck(EntityDamageByEntityEvent event, mcMMO pluginx) {
        Arrow arrow = (Arrow) event.getDamager();
        LivingEntity shooter = arrow.getShooter();
        LivingEntity target = (LivingEntity) event.getEntity();

        if (target instanceof Player) {
            Player defender = (Player) target;

            if (defender.getItemInHand().getType().equals(Material.AIR)) {
                if (configInstance.getUnarmedPVP()) {
                    Unarmed.deflectCheck(defender, event);
                }
            }
        }

        if (shooter instanceof Player) {
            Player attacker = (Player) shooter;
            PlayerProfile PPa = Users.getProfile(attacker);
            int damage = event.getDamage();

            if (permInstance.archery(attacker) && damage > 0) {

                if (permInstance.archeryBonus(attacker)) {

                    /*Archery needs a damage bonus to be viable in PVP*/
                    int skillLvl = Users.getProfile(attacker).getSkillLevel(SkillType.ARCHERY);
                    double dmgBonusPercent = ((skillLvl / 50) * 0.1D);

                    /* Cap maximum bonus at 200% */
                    if (dmgBonusPercent > 2) {
                        dmgBonusPercent = 2;
                    }

                    /* Every 50 skill levels Archery gains 10% damage bonus, set that here */
                    //TODO: Work in progress for balancing out Archery, will work on it more later...
                    int archeryBonus = (int)(event.getDamage() * dmgBonusPercent);
                    event.setDamage(event.getDamage() + archeryBonus);
                }

                if (permInstance.trackArrows(attacker)) {
                    Archery.trackArrows(pluginx, target, PPa);
                }

                startGainXp(attacker, PPa, target, SkillType.ARCHERY, pluginx);

                if (target instanceof Player) {
                    Player defender = (Player) target;
                    PlayerProfile PPd = Users.getProfile(defender);

                    if (PPa.inParty() && PPd.inParty() && Party.getInstance().inSameParty(defender, attacker)) {
                        event.setCancelled(true);
                        return;
                    }

                    if (permInstance.daze(attacker)) {
                        Archery.dazeCheck(defender, attacker);
                    }
                }
            }
        }
    }

    /**
     * Attempt to damage target for value dmg with reason CUSTOM
     *
     * @param target LivingEntity which to attempt to damage
     * @param dmg Amount of damage to attempt to do
     */
    public static void dealDamage(LivingEntity target, int dmg) {
        dealDamage(target, dmg, EntityDamageEvent.DamageCause.CUSTOM);
    }

    /**
     * Attempt to damage target for value dmg with reason cause
     *
     * @param target LivingEntity which to attempt to damage
     * @param dmg Amount of damage to attempt to do
     * @param cause DamageCause to pass to damage event
     */
    private static void dealDamage(LivingEntity target, int dmg, DamageCause cause) {
        if (configInstance.getEventCallbackEnabled()) {
            EntityDamageEvent ede = (EntityDamageEvent) new FakeEntityDamageEvent(target, cause, dmg);
            mcMMO.p.getServer().getPluginManager().callEvent(ede);

            if (ede.isCancelled()) {
                return;
            }

            target.damage(ede.getDamage());
        }
        else {
            target.damage(dmg);
        }
    }

    /**
     * Attempt to damage target for value dmg with reason ENTITY_ATTACK with damager attacker
     *
     * @param target LivingEntity which to attempt to damage
     * @param dmg Amount of damage to attempt to do
     * @param attacker Player to pass to event as damager
     */
    private static void dealDamage(LivingEntity target, int dmg, Player attacker) {
        if (configInstance.getEventCallbackEnabled()) {
            EntityDamageEvent ede = (EntityDamageByEntityEvent) new FakeEntityDamageByEntityEvent(attacker, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, dmg);
            mcMMO.p.getServer().getPluginManager().callEvent(ede);

            if (ede.isCancelled()) {
                return;
            }

            target.damage(ede.getDamage());
        }
        else {
            target.damage(dmg);
        }
    }

    /**
     * Apply Area-of-Effect ability actions.
     *
     * @param attacker The attacking player
     * @param target The defending entity
     * @param damage The initial damage amount
     * @param plugin mcMMO plugin instance
     * @param type The type of skill being used
     */
    private static void applyAbilityAoE(Player attacker, LivingEntity target, int damage, mcMMO plugin, SkillType type) {
        int numberOfTargets = Misc.getTier(attacker.getItemInHand()); //The higher the weapon tier, the more targets you hit
        int damageAmount = damage;

        if (damageAmount < 1) {
            damageAmount = 1;
        }

        for (Entity entity : target.getNearbyEntities(2.5, 2.5, 2.5)) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            if (numberOfTargets <= 0) {
                break;
            }

            switch (entity.getType()) {
            case WOLF:
                AnimalTamer tamer = ((Wolf) entity).getOwner();

                if (tamer instanceof Player) {
                    if (tamer.equals(attacker) || Party.getInstance().inSameParty(attacker, (Player) tamer)) {
                        continue;
                    }
                }

                break;

            case PLAYER:
                Player defender = (Player) entity;

                if (!target.getWorld().getPVP()) {
                    continue;
                }

                if (defender.getName().equals(attacker.getName())) {
                    continue;
                }

                if (Party.getInstance().inSameParty(attacker, defender)) {
                    continue;
                }

                PlayerProfile playerProfile = Users.getProfile((Player) entity);

                if (playerProfile.getGodMode()) {
                    continue;
                }

                break;

            default:
                break;
            }

            switch (type) {
            case SWORDS:
                if (entity instanceof Player) {
                    ((Player) entity).sendMessage(LocaleLoader.getString("Swords.Combat.SS.Struck"));
                }

                BleedTimer.add((LivingEntity) entity, 5);

                break;

            case AXES:
                if (entity instanceof Player) {
                    ((Player) entity).sendMessage(LocaleLoader.getString("Axes.Combat.Cleave.Struck"));
                }

                break;

            default:
                break;
            }

            dealDamage((LivingEntity) entity, damageAmount, attacker);
            numberOfTargets--;
        }
    }

    /**
     * Start the task that gives combat XP.
     *
     * @param attacker The attacking player
     * @param PP The player's PlayerProfile
     * @param target The defending entity
     * @param skillType The skill being used
     * @param plugin mcMMO plugin instance
     */
    public static void startGainXp(Player attacker, PlayerProfile PP, LivingEntity target, SkillType skillType, mcMMO pluginx) {
        double baseXP = 0;

        if (target instanceof Player) {
            if (!configInstance.getExperienceGainsPlayerVersusPlayerEnabled()) {
                return;
            }

            Player defender = (Player) target;
            PlayerProfile PPd = Users.getProfile(defender);

            if (System.currentTimeMillis() >= (PPd.getRespawnATS() * 1000) + 5000 && ((PPd.getLastLogin() + 5) * 1000) < System.currentTimeMillis() && defender.getHealth() >= 1) {
                baseXP = 20 * configInstance.getPlayerVersusPlayerXP();
            }
        }
        else if (!target.hasMetadata("mcmmoFromMobSpawner")) {
            if (target instanceof Animals && !target.hasMetadata("mcmmoSummoned")) {
                baseXP = configInstance.getAnimalsXP();
            }
            else {
                EntityType type = target.getType();

                switch (type) {
                case BLAZE:
                    baseXP = configInstance.getBlazeXP();
                    break;

                case CAVE_SPIDER:
                    baseXP = configInstance.getCaveSpiderXP();
                    break;

                case CREEPER:
                    baseXP = configInstance.getCreeperXP();
                    break;

                case ENDER_DRAGON:
                    baseXP = configInstance.getEnderDragonXP();
                    break;

                case ENDERMAN:
                    baseXP = configInstance.getEndermanXP();
                    break;

                case GHAST:
                    baseXP = configInstance.getGhastXP();
                    break;

                case MAGMA_CUBE:
                    baseXP = configInstance.getMagmaCubeXP();
                    break;

                case IRON_GOLEM:
                    if (!((IronGolem) target).isPlayerCreated())
                        baseXP = configInstance.getIronGolemXP();
                    break;

                case PIG_ZOMBIE:
                    baseXP = configInstance.getPigZombieXP();
                    break;

                case SILVERFISH:
                    baseXP = configInstance.getSilverfishXP();
                    break;

                case SKELETON:
                    baseXP = configInstance.getSkeletonXP();
                    break;

                case SLIME:
                    baseXP = configInstance.getSlimeXP();
                    break;

                case SPIDER:
                    baseXP = configInstance.getSpiderXP();
                    break;

                case ZOMBIE:
                    baseXP = configInstance.getZombieXP();
                    break;

                default:
                    break;
                }
            }

            baseXP *= 10;
        }

        if (baseXP != 0) {
            mcMMO.p.getServer().getScheduler().scheduleSyncDelayedTask(pluginx, new GainXp(attacker, PP, skillType, baseXP, target), 0);
        }
    }
}
