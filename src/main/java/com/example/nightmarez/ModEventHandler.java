package com.example.nightmarez;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Random;

public class ModEventHandler {
    private final Random rand = new Random();

    // ИЗМЕНЕНО: Вместо LivingSpawnEvent.CheckSpawn, мы будем использовать
    // EntityJoinLevelEvent. Это событие вызывается для КАЖДОЙ сущности перед
    // ее добавлением в мир, что позволяет нам отменить спавн. Это более надежный способ.
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent evt) {
        Entity entity = evt.getEntity();
        Level level = evt.getLevel();

        if (level.isClientSide) return;

        // --- Логика отмены спавна ---
        // Если это монстр, но не зомби, отменяем его появление
        if (entity instanceof Monster && !(entity instanceof Zombie)) {
            evt.setCanceled(true);
            return; // Важно выйти, чтобы не выполнять остальной код для отмененной сущности
        }

        // --- Логика для зомби ---
        if (entity instanceof Zombie zombie) {
            ServerLevel serverLevel = (ServerLevel) level;

            // Проверяем, обрабатывали ли мы уже этого зомби, чтобы избежать бесконечного цикла спавна
            if (zombie.getPersistentData().getBoolean("nightmarez_processed")) {
                return;
            }
            zombie.getPersistentData().putBoolean("nightmarez_processed", true);


            AttributeInstance speedAttr = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(0.10D);
            }
            AttributeInstance followAttr = zombie.getAttribute(Attributes.FOLLOW_RANGE);
            if (followAttr != null) followAttr.setBaseValue(50.0D);

            zombie.getPersistentData().putBoolean("nightmarez_no_sun_burn", true);
            zombie.goalSelector.addGoal(3, new BreakBlockGoal(zombie, 3.0f));

            int extras = 19;
            for (int i = 0; i < extras; i++) {
                // ИСПРАВЛЕНО: Добавлено явное приведение типа (Zombie)
                Zombie copy = (Zombie) EntityType.ZOMBIE.create(level);
                if (copy == null) continue;

                copy.setPos(zombie.getX() + rand.nextDouble() * 6.0 - 3.0, zombie.getY(), zombie.getZ() + rand.nextDouble() * 6.0 - 3.0);

                // Устанавливаем атрибуты для копии
                AttributeInstance s = copy.getAttribute(Attributes.MOVEMENT_SPEED);
                if (s != null) s.setBaseValue(0.10D);
                AttributeInstance f = copy.getAttribute(Attributes.FOLLOW_RANGE);
                if (f != null) f.setBaseValue(50.0D);

                copy.getPersistentData().putBoolean("nightmarez_no_sun_burn", true);

                // Отмечаем копию как обработанную, чтобы избежать рекурсивного спавна
                copy.getPersistentData().putBoolean("nightmarez_processed", true);

                serverLevel.addFreshEntity(copy);
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load evt) {
        if (evt.getLevel().isClientSide()) return;
        if (!(evt.getLevel() instanceof ServerLevel serverLevel)) return;
        try {
            serverLevel.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, serverLevel.getServer());
            serverLevel.setDayTime(18000L);
        } catch (Exception e) {
            serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(), "gamerule doDaylightCycle false");
            serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(), "time set night");
        }
    }
}
