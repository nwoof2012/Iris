package net.coderbot.iris.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import net.coderbot.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.coderbot.iris.mixin.GameRendererAccessor;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;

public class HandRenderer {
	public static final HandRenderer INSTANCE = new HandRenderer();

	private boolean ACTIVE;
	private boolean renderingSolid;
	private final FullyBufferedMultiBufferSource bufferSource = new FullyBufferedMultiBufferSource();

	public static final float DEPTH = 0.125F;

	private PoseStack setupGlState(Matrix4f modelMatrix, GameRenderer gameRenderer, Camera camera, float tickDelta) {
        final PoseStack poseStack = new PoseStack();

		Matrix4f scaleMatrix = new Matrix4f().scale(1F, 1F, DEPTH);
		scaleMatrix.mul(gameRenderer.getProjectionMatrix(((GameRendererAccessor) gameRenderer).invokeGetFov(camera, tickDelta, false)));
		gameRenderer.resetProjectionMatrix(scaleMatrix);

		poseStack.pushPose();

		((GameRendererAccessor) gameRenderer).invokeBobHurt(poseStack, tickDelta);

		if (Minecraft.getInstance().options.bobView().get()) {
			((GameRendererAccessor) gameRenderer).invokeBobView(poseStack, tickDelta);
		}

		return poseStack;
	}

	private boolean canRender(Camera camera, GameRenderer gameRenderer) {
		return !(!((GameRendererAccessor) gameRenderer).getRenderHand()
				|| camera.isDetached()
					|| !(camera.getEntity() instanceof Player)
						|| ((GameRendererAccessor)gameRenderer).getPanoramicMode()
							|| Minecraft.getInstance().options.hideGui
								|| (camera.getEntity() instanceof LivingEntity && ((LivingEntity)camera.getEntity()).isSleeping())
									|| Minecraft.getInstance().gameMode.getPlayerMode() == GameType.SPECTATOR);
	}

	public boolean isHandTranslucent(InteractionHand hand) {
		Item item = Minecraft.getInstance().player.getItemBySlot(hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND).getItem();

		if (item instanceof BlockItem) {
			return ItemBlockRenderTypes.getChunkRenderType(((BlockItem) item).getBlock().defaultBlockState()) == RenderType.translucent();
		}

		return false;
	}

	public boolean isAnyHandTranslucent() {
		return isHandTranslucent(InteractionHand.MAIN_HAND) || isHandTranslucent(InteractionHand.OFF_HAND);
	}

	public void renderSolid(Matrix4f modelMatrix, float tickDelta, Camera camera, GameRenderer gameRenderer, WorldRenderingPipeline pipeline) {
		if (!canRender(camera, gameRenderer) || !IrisApi.getInstance().isShaderPackInUse()) {
			return;
		}

		ACTIVE = true;

		pipeline.setPhase(WorldRenderingPhase.HAND_SOLID);


		Minecraft.getInstance().getProfiler().push("iris_hand");

		PoseStack poseStack = setupGlState(modelMatrix, gameRenderer, camera, tickDelta);

		RenderSystem.getModelViewStack().pushMatrix();
		RenderSystem.getModelViewStack().set(poseStack.last().pose());
		RenderSystem.applyModelViewMatrix();
		renderingSolid = true;

		gameRenderer.itemInHandRenderer.renderHandsWithItems(tickDelta, new PoseStack(), bufferSource.getUnflushableWrapper(), Minecraft.getInstance().player, Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(camera.getEntity(), tickDelta));

		Minecraft.getInstance().getProfiler().pop();

		bufferSource.readyUp();
		bufferSource.endBatch();

		gameRenderer.resetProjectionMatrix(CapturedRenderingState.INSTANCE.getGbufferProjection());
		RenderSystem.getModelViewStack().popMatrix();
		RenderSystem.applyModelViewMatrix();
		poseStack.popPose();

		renderingSolid = false;

		pipeline.setPhase(WorldRenderingPhase.NONE);

		ACTIVE = false;
	}

	public void renderTranslucent(Matrix4f modelMatrix, float tickDelta, Camera camera, GameRenderer gameRenderer, WorldRenderingPipeline pipeline) {
		if (!canRender(camera, gameRenderer) || !isAnyHandTranslucent() || !IrisApi.getInstance().isShaderPackInUse()) {
			return;
		}

		ACTIVE = true;

		pipeline.setPhase(WorldRenderingPhase.HAND_TRANSLUCENT);

		Minecraft.getInstance().getProfiler().push("iris_hand_translucent");

		PoseStack poseStack = setupGlState(modelMatrix, gameRenderer, camera, tickDelta);
		RenderSystem.getModelViewStack().pushMatrix();
		RenderSystem.getModelViewStack().set(poseStack.last().pose());
		RenderSystem.applyModelViewMatrix();
		gameRenderer.itemInHandRenderer.renderHandsWithItems(tickDelta, new PoseStack(), bufferSource, Minecraft.getInstance().player, Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(camera.getEntity(), tickDelta));

		poseStack.popPose();

		Minecraft.getInstance().getProfiler().pop();

		gameRenderer.resetProjectionMatrix(CapturedRenderingState.INSTANCE.getGbufferProjection());

		bufferSource.endBatch();
		RenderSystem.getModelViewStack().popMatrix();
		RenderSystem.applyModelViewMatrix();

		pipeline.setPhase(WorldRenderingPhase.NONE);

		ACTIVE = false;
	}

	public boolean isActive() {
		return ACTIVE;
	}

	public boolean isRenderingSolid() {
		return renderingSolid;
	}

	public FullyBufferedMultiBufferSource getBufferSource() {
		return bufferSource;
	}
}
