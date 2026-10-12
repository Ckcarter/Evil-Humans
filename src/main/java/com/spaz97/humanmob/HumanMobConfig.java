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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

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
