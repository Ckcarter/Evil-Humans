package com.spaz97.humanmob.entity;

import com.spaz97.humanmob.HumanMobConfig;
import com.spaz97.humanmob.HumanMobMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * A player-like mob.
 * <ul>
 *   <li>Without a friend it hunts nearby monsters (never players unless attacked).</li>
 *   <li>Throw it a diamond and the thrower becomes its friend: it thanks you, follows you,
 *       and pockets the diamond after a couple of seconds.</li>
 *   <li>Give your friend a sword and it will fight monsters again while still following you.</li>
 *   <li>It never retaliates against its friend, and says goodbye when it dies.</li>
 * </ul>
 */
public class Human extends Monster {
    private static final ResourceLocation DIAMONDS_ADVANCEMENT =
            new ResourceLocation(HumanMobMod.MODID, "diamonds_to_you");
    private static final int DIAMOND_HOLD_TICKS = 50;

    /** Player name whose skin this Human wears ("" = default skin). Synced to clients. */
    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(Human.class, EntityDataSerializers.STRING);

    @Nullable private UUID friendId;
    @Nullable private String friendName;
    private int diamondTimer;

    public Human(EntityType<? extends Human> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SKIN, "");
    }

    public String getSkinName() {
        return this.entityData.get(DATA_SKIN);
    }

    public void setSkinName(@Nullable String name) {
        this.entityData.set(DATA_SKIN, name == null ? "" : name);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        // Pick a random skin from the config list.
        List<? extends String> skins = HumanMobConfig.SKINS.get();
        if (!skins.isEmpty()) {
            this.setSkinName(skins.get(this.random.nextInt(skins.size())));
        }
        return result;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new FollowFriendGoal(this, 1.0D, 8.0F, 3.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // Fight back when hurt -- but never against our friend.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && !Human.this.isFriend(Human.this.getLastHurtByMob());
            }
        });
        // Hunt monsters when we have no friend, or when our friend has armed us with a sword.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                target -> !(target instanceof Human) && this.isReadyToFight()));
    }

    private boolean isReadyToFight() {
        return !this.hasFriend() || this.getMainHandItem().getItem() instanceof SwordItem;
    }

    // ---------------------------------------------------------------- friend

    public boolean hasFriend() {
        return this.friendId != null || this.friendName != null;
    }

    public boolean isFriend(@Nullable Entity entity) {
        return entity instanceof Player && this.friendId != null && this.friendId.equals(entity.getUUID());
    }

    /** The friend, if they are online and in the same dimension. */
    @Nullable
    public Player getFriend() {
        return this.friendId == null ? null : this.level().getPlayerByUUID(this.friendId);
    }

    private void say(@Nullable Player player, String text) {
        if (player != null) {
            player.sendSystemMessage(Component.literal("<" + this.getName().getString() + "> " + text));
        }
    }

    // ---------------------------------------------------------------- items

    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    public void onItemPickup(ItemEntity itemEntity) {
        super.onItemPickup(itemEntity);
        if (this.level().isClientSide) {
            return;
        }
        ItemStack stack = itemEntity.getItem();

        if (stack.is(Items.DIAMOND) && !this.hasFriend()) {
            Player thrower = this.findThrower(itemEntity);
            if (thrower == null) {
                return;
            }
            this.friendId = thrower.getUUID();
            this.friendName = thrower.getGameProfile().getName();
            this.diamondTimer = DIAMOND_HOLD_TICKS;
            this.setPersistenceRequired();
            this.awardDiamondsAdvancement(thrower);
            this.say(thrower, "Wow! Thanks, " + this.friendName + "! How could I ever repay you?");
        } else if (stack.getItem() instanceof SwordItem && this.hasFriend()) {
            this.say(this.getFriend(), "I'm ready to take anyone on!");
        }
    }

    @Nullable
    private Player findThrower(ItemEntity itemEntity) {
        // ItemEntity stores who threw it under "Thrower" in its saved data.
        CompoundTag tag = itemEntity.saveWithoutId(new CompoundTag());
        if (!tag.hasUUID("Thrower")) {
            return null;
        }
        return this.level().getPlayerByUUID(tag.getUUID("Thrower"));
    }

    private void awardDiamondsAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverPlayer.getServer();
            Advancement advancement = server == null ? null
                    : server.getAdvancements().getAdvancement(DIAMONDS_ADVANCEMENT);
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "gift");
            }
        }
    }

    // ---------------------------------------------------------------- ticking

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        // Friends saved by the old version (name only): match them up once they're online.
        if (this.friendId == null && this.friendName != null && this.tickCount % 20 == 0) {
            MinecraftServer server = this.getServer();
            ServerPlayer player = server == null ? null : server.getPlayerList().getPlayerByName(this.friendName);
            if (player != null) {
                this.friendId = player.getUUID();
            }
        }

        // Hold up the gifted diamond for a moment, then pocket it.
        if (this.hasFriend() && this.diamondTimer >= 0 && this.getMainHandItem().is(Items.DIAMOND)) {
            if (this.diamondTimer > 0) {
                this.diamondTimer--;
            } else {
                this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.diamondTimer = -1;
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level().isClientSide) {
            return;
        }
        Player friend = this.getFriend();
        if (friend != null) {
            if (this.isFriend(source.getEntity())) {
                this.say(friend, "Why, " + this.friendName + "? I thought I was your friend...");
            } else {
                this.say(friend, "I'm sorry, " + this.friendName + "... I've failed you...");
            }
        }
    }

    // ---------------------------------------------------------------- save data

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.friendId != null) {
            tag.putUUID("FriendUUID", this.friendId);
        }
        if (this.friendName != null) {
            tag.putString("Friend", this.friendName);
        }
        tag.putInt("DiamondTimer", this.diamondTimer);
        if (!this.getSkinName().isEmpty()) {
            tag.putString("Skin", this.getSkinName());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.friendId = tag.hasUUID("FriendUUID") ? tag.getUUID("FriendUUID") : null;
        this.friendName = tag.contains("Friend") && !tag.getString("Friend").isEmpty() ? tag.getString("Friend") : null;
        this.diamondTimer = tag.contains("DiamondTimer") ? tag.getInt("DiamondTimer") : -1;
        this.setSkinName(tag.getString("Skin"));
    }

    // ---------------------------------------------------------------- sounds (player-like)

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.PLAYER_SWIM;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.PLAYER_SPLASH;
    }

    @Override
    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.PLAYER_SMALL_FALL, SoundEvents.PLAYER_BIG_FALL);
    }
}
