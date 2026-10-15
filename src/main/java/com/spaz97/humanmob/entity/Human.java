package com.spaz97.humanmob.entity;

import com.spaz97.humanmob.HumanMobConfig;
import com.spaz97.humanmob.HumanMobMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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
 *   <li>Evil (default config): without a friend it attacks players on sight and ignores monsters.
 *       With {@code evil = false} it hunts monsters instead and only fights players who attack it.</li>
 *   <li>It never picks items up. Right-click it with a diamond and you become its friend: it stops
 *       attacking players, follows you, and pockets the diamond after a couple of seconds.</li>
 *   <li>Right-click your friend with a sword to arm it: it fights monsters again while still following you.</li>
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
    /** Spawned naturally at night: vanishes at sunrise unless befriended or named. */
    private boolean nightSpawn;

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

    /** True when this Human goes by the name of the skin it was given (no name tag on it). */
    public boolean isNamedAfterSkin() {
        return !this.hasCustomName() && !this.getSkinName().isEmpty() && HumanMobConfig.NAME_AFTER_SKIN.get();
    }

    /** Named after its skin (e.g. "Notch"). Used for the name above its head, chat and death messages. */
    @Override
    public Component getName() {
        return this.isNamedAfterSkin() ? Component.literal(this.getSkinName()) : super.getName();
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        this.nightSpawn = reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION;
        // Pick a random skin from the config list.
        List<? extends String> skins = HumanMobConfig.SKINS.get();
        if (!skins.isEmpty()) {
            this.setSkinName(skins.get(this.random.nextInt(skins.size())));
        }
        return result;
    }

    /** Natural spawns: only at night, only on the surface (open sky), only in the dark. */
    public static boolean checkHumanSpawnRules(EntityType<Human> type, ServerLevelAccessor level, MobSpawnType reason,
                                               BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION) {
            if (!HumanMobConfig.SPAWN_AT_NIGHT.get() || !level.getLevel().isNight() || !level.canSeeSky(pos)) {
                return false;
            }
        }
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D) // slow walk; overridden by walkSpeed in the config
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
        // Evil Humans hunt players -- unless they've been bribed with a diamond.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> isEvil() && !this.hasFriend()));
        // Hunt monsters when armed with a sword by our friend (or, if not evil, whenever we have no friend).
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                target -> !(target instanceof Human) && this.wantsToHuntMonsters()));
    }

    private static boolean isEvil() {
        return HumanMobConfig.EVIL.get();
    }

    private boolean wantsToHuntMonsters() {
        if (this.hasFriend()) {
            return this.getMainHandItem().getItem() instanceof SwordItem;
        }
        return !isEvil();
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

    /** Humans never pick items up off the ground. Hand them items with right-click instead. */
    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // Right-click with a diamond: bribe / befriend.
        if (held.is(Items.DIAMOND) && !this.hasFriend()) {
            if (!this.level().isClientSide) {
                this.befriend(player);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Friend right-clicks with a sword: hand it over.
        if (held.getItem() instanceof SwordItem && this.isFriend(player) && !this.level().isClientSide) {
            ItemStack sword = held.copy();
            sword.setCount(1);
            ItemStack previous = this.getMainHandItem();
            this.setItemSlot(EquipmentSlot.MAINHAND, sword);
            this.setDropChance(EquipmentSlot.MAINHAND, 2.0F); // always drops your sword back if it dies
            this.diamondTimer = -1;
            if (!previous.isEmpty() && !previous.is(Items.DIAMOND)) {
                this.spawnAtLocation(previous);
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            this.say(player, "I'm ready to take anyone on!");
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private void befriend(Player player) {
        this.friendId = player.getUUID();
        this.friendName = player.getGameProfile().getName();
        this.setPersistenceRequired();
        this.awardDiamondsAdvancement(player);

        // Hold up the diamond for a moment (then pocket it), unless already holding something.
        if (this.getMainHandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            this.diamondTimer = DIAMOND_HOLD_TICKS;
        }

        if (isEvil()) {
            // Stop chasing the player who just bought us off.
            if (this.getTarget() instanceof Player) {
                this.setTarget(null);
            }
            this.say(player, "Hmph... a diamond? Fine, " + this.friendName + ". I'll fight for you instead.");
        } else {
            this.say(player, "Wow! Thanks, " + this.friendName + "! How could I ever repay you?");
        }
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
        this.applyWalkSpeed();

        // Wild night-time Humans vanish once the sun is up (not all at once).
        if (this.nightSpawn && !this.hasFriend() && !this.hasCustomName() && !this.isPersistenceRequired()
                && this.level().isDay() && this.tickCount % 20 == 0 && this.random.nextInt(8) == 0) {
            this.level().broadcastEntityEvent(this, (byte) 60); // puff of smoke
            this.discard();
            return;
        }

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

    /** Keep the walking speed in line with the config (so edits apply without a restart). */
    private void applyWalkSpeed() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        double wanted = HumanMobConfig.WALK_SPEED.get();
        if (speed != null && speed.getBaseValue() != wanted) {
            speed.setBaseValue(wanted);
        }
        if (this.isSprinting()) {
            this.setSprinting(false);
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
        tag.putBoolean("NightSpawn", this.nightSpawn);
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
        this.nightSpawn = tag.getBoolean("NightSpawn");
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
