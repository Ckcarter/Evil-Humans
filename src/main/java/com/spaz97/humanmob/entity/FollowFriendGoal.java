package com.spaz97.humanmob.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** Walks back to the Human's friend whenever they get too far away. */
public class FollowFriendGoal extends Goal {
    private final Human human;
    private final double speed;
    private final float startDistanceSq;
    private final float stopDistanceSq;
    private Player friend;
    private int recalcPathIn;

    public FollowFriendGoal(Human human, double speed, float startDistance, float stopDistance) {
        this.human = human;
        this.speed = speed;
        this.startDistanceSq = startDistance * startDistance;
        this.stopDistanceSq = stopDistance * stopDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.human.getTarget() != null) {
            return false;
        }
        Player player = this.human.getFriend();
        if (player == null || player.isSpectator() || !player.isAlive()
                || this.human.distanceToSqr(player) < this.startDistanceSq) {
            return false;
        }
        this.friend = player;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.friend != null && this.friend.isAlive() && !this.friend.isSpectator()
                && this.human.getTarget() == null
                && !this.human.getNavigation().isDone()
                && this.human.distanceToSqr(this.friend) > this.stopDistanceSq;
    }

    @Override
    public void start() {
        this.recalcPathIn = 0;
        this.human.getNavigation().moveTo(this.friend, this.speed);
    }

    @Override
    public void stop() {
        this.friend = null;
        this.human.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.human.getLookControl().setLookAt(this.friend, 10.0F, (float) this.human.getMaxHeadXRot());
        if (--this.recalcPathIn <= 0) {
            this.recalcPathIn = this.adjustedTickDelay(10);
            this.human.getNavigation().moveTo(this.friend, this.speed);
        }
    }
}
