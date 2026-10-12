package com.spaz97.humanmob.client;

import com.spaz97.humanmob.entity.Human;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** Renders a Human with the player model; name tag it with a player's name to wear their skin. */
public class HumanRenderer extends HumanoidMobRenderer<Human, PlayerModel<Human>> {

    public HumanRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new Deadmau5EarsLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Human human) {
        return HumanSkins.get(human);
    }
}
