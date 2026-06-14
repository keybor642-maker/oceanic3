package mc.mkay.oceanicdaggers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class DaggerListener implements Listener {

    private final OceanicDaggers plugin;

    private static final long DASH_COOLDOWN = 40000;
    private static final long TIDE_COOLDOWN = 60000;

    private final Map<UUID, Long>   dashCooldowns = new HashMap<>();
    private final Map<UUID, Long>   tideCooldowns = new HashMap<>();
    private final Set<UUID>         inTide        = new HashSet<>();
    private final Map<UUID, Double> tideLaunchY   = new HashMap<>();
    private final Map<UUID, Double> tidePeakY     = new HashMap<>();

    public DaggerListener(OceanicDaggers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!DaggerItems.isDualWielding(player)) return;
        event.setCancelled(true);
        if (player.isSneaking()) activateTide(player);
        else activateDash(player);
    }

    // ─── ABILITY 1: DAGGER DASH (cross + lunge through) ───────────────────────

    private void activateDash(Player player) {
        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (dashCooldowns.containsKey(uid) && now - dashCooldowns.get(uid) < DASH_COOLDOWN) {
            long left = (DASH_COOLDOWN - (now - dashCooldowns.get(uid))) / 1000;
            player.sendActionBar(Component.text("⚔ Dash on cooldown: " + left + "s", NamedTextColor.RED));
            return;
        }

        LivingEntity target = getNearestTarget(player, 10);
        if (target == null) {
            player.sendActionBar(Component.text("No target in range!", NamedTextColor.GRAY));
            return;
        }

        dashCooldowns.put(uid, now);

        World world = player.getWorld();
        Location eyeLoc = player.getEyeLocation();
        Vector facing = eyeLoc.getDirection();

        // Sounds
        world.playSound(eyeLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
        world.playSound(eyeLoc, Sound.ITEM_TRIDENT_THROW, 1f, 1.5f);

        // Instant crossed-daggers flash — pure particles, no display entities left behind
        Location crossPoint = eyeLoc.clone().add(facing.clone().multiply(1.2));
        spawnCrossBurst(world, crossPoint, facing);

        // Spear-style lunge toward the target before dashing through
        Vector lungeDir = target.getLocation().toVector()
                .subtract(player.getLocation().toVector())
                .setY(0)
                .normalize();
        player.setVelocity(lungeDir.clone().multiply(1.1).setY(0.25));

        new BukkitRunnable() {
            @Override
            public void run() {
                performDash(player, target, world);
            }
        }.runTaskLater(plugin, 3);
    }

    /**
     * Draws an instant X-shaped particle cross at the given point, facing the player.
     * Replaces the old item-display "crossed daggers" animation.
     */
    private void spawnCrossBurst(World world, Location center, Vector facing) {
        Vector up = new Vector(0, 1, 0);
        Vector right = facing.clone().crossProduct(up);
        if (right.lengthSquared() < 0.0001) {
            right = new Vector(1, 0, 0);
        }
        right.normalize();

        for (double t = -0.5; t <= 0.5; t += 0.08) {
            Location diag1 = center.clone().add(right.clone().multiply(t)).add(0, t, 0);
            Location diag2 = center.clone().add(right.clone().multiply(t)).add(0, -t, 0);

            world.spawnParticle(Particle.CRIT, diag1, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.DRIPPING_WATER, diag1, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.CRIT, diag2, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.DRIPPING_WATER, diag2, 1, 0, 0, 0, 0);
        }

        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 1.3f);
    }

    private void performDash(Player player, LivingEntity target, World world) {
        if (!target.isValid() || target.isDead()) {
            player.sendActionBar(Component.text("Target lost.", NamedTextColor.GRAY));
            return;
        }

        Location targetLoc = target.getLocation();
        Vector dir = targetLoc.toVector().subtract(player.getLocation().toVector()).normalize();
        Location dashEnd = targetLoc.clone().add(dir.multiply(1.5)).add(0, 0.1, 0);
        dashEnd.setYaw(player.getLocation().getYaw());
        dashEnd.setPitch(player.getLocation().getPitch());

        // Trail as we dash through
        Location start = player.getLocation().clone();
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 5) { cancel(); return; }
                double p = t / 5.0;
                Location trail = start.clone().add(dir.clone().multiply(p * targetLoc.distance(start)));
                world.spawnParticle(Particle.DRIPPING_WATER, trail, 5, 0.1, 0.1, 0.1, 0);
                world.spawnParticle(Particle.CRIT, trail, 3, 0.1, 0.1, 0.1, 0.2);
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(dashEnd);
                target.damage(4.0, player); // 2 hearts
                spawnCrossBurst(world, target.getLocation().add(0, 1, 0), dir);
                world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 1.2f);
                player.sendActionBar(Component.text("⚔ Dagger Dash!", NamedTextColor.AQUA));
            }
        }.runTaskLater(plugin, 3);
    }

    // ─── ABILITY 2: OCEANIC TIDE ──────────────────────────────────────────────

    private void activateTide(Player player) {
        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (tideCooldowns.containsKey(uid) && now - tideCooldowns.get(uid) < TIDE_COOLDOWN) {
            long left = (TIDE_COOLDOWN - (now - tideCooldowns.get(uid))) / 1000;
            player.sendActionBar(Component.text("≋ Tide on cooldown: " + left + "s", NamedTextColor.BLUE));
            return;
        }

        tideCooldowns.put(uid, now);
        inTide.add(uid);
        tideLaunchY.put(uid, player.getLocation().getY());
        tidePeakY.put(uid, player.getLocation().getY());

        World world = player.getWorld();
        Location loc = player.getLocation();

        for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2 / 16) * i;
            double x = Math.cos(angle) * 2;
            double z = Math.sin(angle) * 2;
            world.spawnParticle(Particle.SPLASH, loc.clone().add(x, 0.1, z), 8, 0.1, 0.1, 0.1, 0.2);
            world.spawnParticle(Particle.DRIPPING_WATER, loc.clone().add(x, 0.5, z), 3, 0, 0, 0, 0);
        }
        world.spawnParticle(Particle.SPLASH, loc.clone().add(0, 0.5, 0), 30, 1, 0.5, 1, 0.3);
        world.playSound(loc, Sound.ENTITY_DOLPHIN_SPLASH, 1.5f, 0.6f);
        world.playSound(loc, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 0.8f);

        player.setVelocity(new Vector(0, 1.4, 0));
        player.sendActionBar(Component.text("≋ Oceanic Tide! Crash down on enemies!", NamedTextColor.AQUA));

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!inTide.contains(uid) || ticks > 40) { cancel(); return; }
                world.spawnParticle(Particle.DRIPPING_WATER, player.getLocation(), 6, 0.2, 0.2, 0.2, 0);
                world.spawnParticle(Particle.SPLASH, player.getLocation(), 3, 0.2, 0.2, 0.2, 0.1);
                double y = player.getLocation().getY();
                if (y > tidePeakY.getOrDefault(uid, y)) {
                    tidePeakY.put(uid, y);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 2);

        new BukkitRunnable() {
            @Override
            public void run() {
                inTide.remove(uid);
                tideLaunchY.remove(uid);
                tidePeakY.remove(uid);
            }
        }.runTaskLater(plugin, 60);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        if (!inTide.contains(uid)) return;
        if (player.isOnGround() && player.getLocation().getY() <= (tideLaunchY.getOrDefault(uid, 0.0) + 2)) {
            inTide.remove(uid);
            tideLaunchY.remove(uid);
            performTideCrash(player, uid);
            tidePeakY.remove(uid);
        }
    }

    private void performTideCrash(Player player, UUID uid) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        double peakY = tidePeakY.getOrDefault(uid, loc.getY());
        double fallDistance = Math.max(0.0, peakY - loc.getY());

        // Mace-style smash: extra damage the further you fell from
        double damage = 6.0 + Math.min(fallDistance, 10.0) * 0.6;
        boolean heavy = fallDistance >= 3.0;

        world.spawnParticle(Particle.SPLASH, loc.clone().add(0, 0.5, 0), 60, 2, 0.5, 2, 0.3);
        world.spawnParticle(Particle.DRIPPING_WATER, loc.clone().add(0, 1, 0), 40, 1.5, 0.5, 1.5, 0.2);
        world.spawnParticle(Particle.CRIT, loc.clone().add(0, 0.5, 0), 20, 1, 0.3, 1, 0.1);
        world.spawnParticle(Particle.GUST_EMITTER_LARGE, loc.clone().add(0, 0.1, 0), 1, 0, 0, 0, 0);

        world.playSound(loc, heavy ? Sound.ITEM_MACE_SMASH_GROUND_HEAVY : Sound.ITEM_MACE_SMASH_GROUND, 2f, 1f);
        world.playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 1.5f, 0.5f);
        world.playSound(loc, Sound.ENTITY_DOLPHIN_SPLASH, 1.5f, 0.4f);

        for (Entity entity : world.getNearbyEntities(loc, 3, 3, 3)) {
            if (entity instanceof LivingEntity target && entity != player) {
                if (target instanceof Monster || target instanceof Player) {
                    target.damage(damage, player);

                    // Slam them down and outward, like a mace ground smash
                    Vector horizontal = target.getLocation().toVector().subtract(loc.toVector());
                    horizontal.setY(0);
                    if (horizontal.lengthSquared() < 0.0001) {
                        horizontal = new Vector(0.1, 0, 0.1);
                    }
                    horizontal.normalize().multiply(0.7);

                    Vector kb = new Vector(horizontal.getX(), -0.6, horizontal.getZ());
                    target.setVelocity(kb);

                    world.spawnParticle(Particle.GUST, target.getLocation().add(0, 1, 0), 10, 0.3, 0.1, 0.3, 0.05);
                }
            }
        }
        player.sendActionBar(Component.text("≋ Oceanic Tide Crash! ≋", NamedTextColor.BLUE));
    }

    // ─── UTILS ────────────────────────────────────────────────────────────────

    private LivingEntity getNearestTarget(Player player, double range) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection();
        LivingEntity nearest = null;
        double nearestDist = range;

        for (Entity entity : player.getWorld().getNearbyEntities(eyeLoc, range, range, range)) {
            if (entity == player) continue;
            if (!(entity instanceof LivingEntity living)) continue;
            Vector toEntity = entity.getLocation().add(0, 1, 0).toVector().subtract(eyeLoc.toVector());
            double dist = toEntity.length();
            double dot = toEntity.normalize().dot(dir);
            if (dot > 0.7 && dist < nearestDist) {
                nearest = living;
                nearestDist = dist;
            }
        }
        return nearest;
    }
}
