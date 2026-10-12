package com.spaz97.humanmob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.spaz97.humanmob.entity.Human;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Easter egg carried over from the original: a Human named "deadmau5" gets the ears. */
public class Deadmau5EarsLayer extends RenderLayer<Human, PlayerModel<Human>> {

    public Deadmau5EarsLayer(RenderLayerParent<Human, PlayerModel<Human>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, Human human,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Component name = human.getCustomName();
        if (name == null || !"deadmau5".equals(name.getString()) || human.isInvisible()) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entitySolid(this.getTextureLocation(human)));
        int overlay = LivingEntityRenderer.getOverlayCoords(human, 0.0F);

        for (int i = 0; i < 2; ++i) {
            float yRot = Mth.lerp(partialTick, human.yRotO, human.getYRot())
                    - Mth.lerp(partialTick, human.yBodyRotO, human.yBodyRot);
            float xRot = Mth.lerp(partialTick, human.xRotO, human.getXRot());
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
            poseStack.translate(0.375F * (float) (i * 2 - 1), 0.0F, 0.0F);
            poseStack.translate(0.0F, -0.375F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-xRot));
            poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
            float scale = 1.3333334F;
            poseStack.scale(scale, scale, scale);
            this.getParentModel().renderEars(poseStack, consumer, packedLight, overlay);
            poseStack.popPose();
        }
    }
}
