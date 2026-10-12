package com.spaz97.humanmob.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.spaz97.humanmob.HumanMobConfig;
import com.spaz97.humanmob.entity.Human;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up player skins for Humans (asynchronously, once per name) and falls back to
 * the default Steve skin while loading or if no such player exists.
 */
public final class HumanSkins {
    private static final Map<String, ResourceLocation> SKINS = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();

    private HumanSkins() {}

    /**
     * Which player's skin this Human wears: a name tag with a valid player name wins,
     * otherwise the skin picked from the config when it spawned. Null = default skin.
     */
    @Nullable
    public static String skinNameFor(Human human) {
        Component customName = human.getCustomName();
        if (customName != null && HumanMobConfig.isValidPlayerName(customName.getString())) {
            return customName.getString();
        }
        String configured = human.getSkinName();
        return HumanMobConfig.isValidPlayerName(configured) ? configured : null;
    }

    public static ResourceLocation get(Human human) {
        String name = skinNameFor(human);
        if (name == null) {
            return DefaultPlayerSkin.getDefaultSkin();
        }
        String key = name.toLowerCase(Locale.ROOT);
        ResourceLocation skin = SKINS.get(key);
        if (skin != null) {
            return skin;
        }
        if (REQUESTED.add(key)) {
            request(name, key);
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    private static void request(String name, String key) {
        SkullBlockEntity.updateGameprofile(new GameProfile(null, name), profile -> {
            Minecraft minecraft = Minecraft.getInstance();
            // Texture registration must happen on the render thread.
            minecraft.execute(() -> {
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures =
                        minecraft.getSkinManager().getInsecureSkinInformation(profile);
                MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
                if (skin != null) {
                    SKINS.put(key, minecraft.getSkinManager().registerTexture(skin, MinecraftProfileTexture.Type.SKIN));
                }
            });
        });
    }
}
