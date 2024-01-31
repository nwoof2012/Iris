package net.coderbot.iris.mixin.fantastic;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import net.coderbot.iris.Iris;
import net.coderbot.iris.fantastic.ParticleRenderingPhase;
import net.coderbot.iris.fantastic.PhasedParticleEngine;
import net.coderbot.iris.shaderpack.ParticleRenderingSettings;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Uses the PhasedParticleManager changes to render opaque particles much earlier than other particles.
 *
 * See the comments in {@link MixinParticleEngine} for more details.
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private RenderBuffers renderBuffers;

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void iris$resetParticleManagerPhase(float pLevelRenderer0, long pLong1, boolean pBoolean2, Camera pCamera3, GameRenderer pGameRenderer4, LightTexture pLightTexture5, Matrix4f pMatrix4f6, Matrix4f pMatrix4f7, CallbackInfo ci) {
		((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
	}

	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=entities"))
	private void iris$renderOpaqueParticles(float f, long pLong1, boolean pBoolean2, Camera camera, GameRenderer pGameRenderer4, LightTexture lightTexture, Matrix4f pMatrix4f6, Matrix4f pMatrix4f7, CallbackInfo ci) {
		minecraft.getProfiler().popPush("opaque_particles");

		MultiBufferSource.BufferSource bufferSource = renderBuffers.bufferSource();

		ParticleRenderingSettings settings = getRenderingSettings();

		Matrix4fStack lvMatrix4fStack24 = RenderSystem.getModelViewStack();
		lvMatrix4fStack24.pushMatrix();
		lvMatrix4fStack24.mul(pMatrix4f6);
		RenderSystem.applyModelViewMatrix();

		if (settings == ParticleRenderingSettings.BEFORE) {
			minecraft.particleEngine.render(lightTexture, camera, f);
		} else if (settings == ParticleRenderingSettings.MIXED) {
			((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
			minecraft.particleEngine.render(lightTexture, camera, f);
		}

		lvMatrix4fStack24.popMatrix();
		RenderSystem.applyModelViewMatrix();
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
	private void iris$renderTranslucentAfterDeferred(ParticleEngine instance, LightTexture lightTexture, Camera camera, float f) {
		ParticleRenderingSettings settings = getRenderingSettings();

		if (settings == ParticleRenderingSettings.AFTER) {
			minecraft.particleEngine.render(lightTexture, camera, f);
		} else if (settings == ParticleRenderingSettings.MIXED) {
				((PhasedParticleEngine) minecraft.particleEngine).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
			minecraft.particleEngine.render(lightTexture, camera, f);
		}
	}

	private ParticleRenderingSettings getRenderingSettings() {
		return Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::getParticleRenderingSettings).orElse(ParticleRenderingSettings.MIXED);
	}
}
