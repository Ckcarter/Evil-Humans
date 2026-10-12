package com.spaz97.humanmob;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.regex.Pattern;

/**
 * config/humanmobmod-common.toml
 * <p>
 * On a server, the server's copy of this file decides which skins Humans get
 * (the chosen skin is sent to players automatically).
 */
public final class HumanMobConfig {
    /** Valid Minecraft usernames: 3-16 letters, digits or underscores. */
    public static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SKINS;
    public static final ForgeConfigSpec.BooleanValue EVIL;
    public static final ForgeConfigSpec.DoubleValue WALK_SPEED;
    public static final ForgeConfigSpec.BooleanValue SPAWN_AT_NIGHT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        EVIL = builder
                .comment(
                        "If true, Humans are evil: they attack players on sight and leave monsters alone.",
                        "Right-clicking one with a diamond still bribes it into becoming your friend.",
                        "If false, Humans behave like the original mod: they hunt monsters and only fight players who hit them.")
                .define("evil", true);

        WALK_SPEED = builder
                .comment(
                        "How fast Humans walk (chasing, following and wandering).",
                        "0.2 = slow walk (default), 0.23 = zombie, 0.3 = about as fast as a walking player, 0.4+ = running.",
                        "Takes effect right after you save the file, for all Humans.")
                .defineInRange("walkSpeed", 0.2D, 0.05D, 0.5D);

        SPAWN_AT_NIGHT = builder
                .comment(
                        "If true, Humans spawn naturally on the surface at night (overworld only),",
                        "and wild ones vanish in a puff of smoke when the sun comes up.",
                        "Humans from spawn eggs, named Humans and befriended Humans never vanish.",
                        "If false, Humans only come from spawn eggs.")
                .define("spawnAtNight", true);

        SKINS = builder
                .comment(
                        "Player names whose skins Humans can wear.",
                        "Every newly spawned Human picks one of these at random and keeps it.",
                        "Leave the list empty to use the default Steve skin.",
                        "A name tag with a player's name always overrides this.",
                        "Changes apply to Humans spawned after you save the file (no restart needed).",
                        "Example: skins = [\"Notch\", \"jeb_\", \"Dinnerbone\", \"deadmau5\"]")
                .defineListAllowEmpty(List.of("skins"), () -> List.<String>of(), HumanMobConfig::isValidPlayerName);

        SPEC = builder.build();
    }

    private HumanMobConfig() {}

    public static boolean isValidPlayerName(Object value) {
        return value instanceof String name && PLAYER_NAME.matcher(name).matches();
    }
}
