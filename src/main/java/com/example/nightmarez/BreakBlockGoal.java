package com.example.nightmarez;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class BreakBlockGoal extends Goal {
    private final Zombie mob;
    private final float breakMultiplier;
    private BlockPos targetPos;
    private int progressTicks;
    private int requiredTicks;

    private static final int MAX_SCAN_DISTANCE = 50;

    public BreakBlockGoal(Zombie mob, float breakMultiplier) {
        this.mob = mob;
        this.breakMultiplier = breakMultiplier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide) return false;
        if (!mob.isAlive()) return false;

        if (mob.getTarget() instanceof Player player) {
            BlockPos found = findBlockBetween(mob.level(), mob.blockPosition(), player.blockPosition());
            if (found != null) {
                this.targetPos = found;
                this.progressTicks = 0;
                this.requiredTicks = estimateRequiredTicks(mob.level(), found);
                return this.requiredTicks > 0;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.level().isClientSide) return false;
        if (targetPos == null) return false;
        BlockState state = mob.level().getBlockState(targetPos);
        return !state.isAir() && mob.isAlive();
    }

    @Override
    public void stop() {
        this.targetPos = null;
        this.progressTicks = 0;
        this.requiredTicks = 0;
    }

    @Override
    public void tick() {
        if (mob.level().isClientSide) return;
        if (targetPos == null) return;
        BlockState state = mob.level().getBlockState(targetPos);

        if (state.isAir() || state.getBlock() == Blocks.BEDROCK) {
            targetPos = null;
            return;
        }

        if (mob.getTarget() != null) {
            Vec3 targetVec = Vec3.atCenterOf(mob.getTarget().blockPosition());
            mob.getLookControl().setLookAt(targetVec.x, targetVec.y, targetVec.z);
        }

        this.progressTicks += Math.max(1, (int) Math.ceil(1.0 * this.breakMultiplier));

        if (this.progressTicks >= this.requiredTicks) {
            boolean removed = mob.level().destroyBlock(targetPos, true, mob);
            if (removed) {
                this.targetPos = null;
                this.progressTicks = 0;
                this.requiredTicks = 0;
            } else {
                this.progressTicks = Math.max(0, this.progressTicks - 2);
            }
        }
    }

    private BlockPos findBlockBetween(Level level, BlockPos from, BlockPos to) {
        Vec3 vFrom = Vec3.atCenterOf(from);
        Vec3 vTo = Vec3.atCenterOf(to);
        Vec3 dir = vTo.subtract(vFrom);
        double dist = dir.length();
        if (dist < 1e-6) return null;
        dir = dir.normalize();

        int checks = Math.min(MAX_SCAN_DISTANCE, (int) Math.ceil(dist));
        for (int i = 1; i <= checks; i++) {
            Vec3 sample = vFrom.add(dir.scale(i));
            BlockPos p = BlockPos.containing(sample);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && s.getBlock() != Blocks.BEDROCK) {
                if (s.getFluidState().isEmpty()) return p;
            }
        }
        return null;
    }

    private int estimateRequiredTicks(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() == Blocks.BEDROCK) return 0;

        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed <= 0.0f) destroySpeed = 1.0f;

        int baseTicks = Math.max(1, (int) Math.ceil(destroySpeed * 30.0f));
        int result = (int) Math.ceil(baseTicks / this.breakMultiplier);
        return Math.max(1, result);
    }
}
