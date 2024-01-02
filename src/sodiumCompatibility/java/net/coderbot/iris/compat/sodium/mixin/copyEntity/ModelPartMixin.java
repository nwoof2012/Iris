package net.coderbot.iris.compat.sodium.mixin.copyEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.coderbot.iris.compat.sodium.impl.vertex_format.entity_xhfp.IrisEntityRenderer;
import net.coderbot.iris.compat.sodium.impl.vertex_format.entity_xhfp.IrisModelCuboid;
import net.coderbot.iris.compat.sodium.impl.vertex_format.entity_xhfp.IrisModelCuboidAccessor;
import net.coderbot.iris.compat.sodium.impl.vertex_format.entity_xhfp.IrisModelPartData;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public class ModelPartMixin implements IrisModelPartData {
	@Shadow
	public float x;
	@Shadow
	public float y;
	@Shadow
	public float z;

	@Shadow
	public float xScale;
	@Shadow
	public float yScale;
	@Shadow
	public float zScale;

	@Shadow
	public float yRot;
	@Shadow
	public float xRot;
	@Shadow
	public float zRot;

	@Shadow
	public boolean visible;
	@Shadow
	public boolean skipDraw;

	@Mutable
	@Shadow
	@Final
	private List<ModelPart.Cube> cubes;

	@Mutable
	@Shadow
	@Final
	private Map<String, ModelPart> children;

	@Unique
	private ModelPart[] iris$children;

	@Unique
	private IrisModelCuboid[] iris$cuboids;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(List<ModelPart.Cube> cuboids, Map<String, ModelPart> children, CallbackInfo ci) {
		var copies = new IrisModelCuboid[cuboids.size()];

		for (int i = 0; i < cuboids.size(); i++) {
			var accessor = (IrisModelCuboidAccessor) cuboids.get(i);
			copies[i] = accessor.iris$copy();
		}

		this.iris$cuboids = copies;
		this.iris$children = children.values()
			.toArray(ModelPart[]::new);

		// Try to catch errors caused by mods touching the collections after we've copied everything.
		this.cubes = Collections.unmodifiableList(this.cubes);
		this.children = Collections.unmodifiableMap(this.children);
	}

	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V", at = @At("HEAD"), cancellable = true)
	private void onRender(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
		VertexBufferWriter writer = VertexConsumerUtils.convertOrLog(vertices);

		if (writer == null) {
			return;
		}

		ci.cancel();

		IrisEntityRenderer.render(matrices, writer, (ModelPart) (Object) this, light, overlay, ColorABGR.pack(red, green, blue, alpha));
	}

	/**
	 * @author JellySquid
	 * @reason Apply transform more quickly
	 */
	@Overwrite
	public void translateAndRotate(PoseStack matrixStack) {
		if (this.x != 0.0F || this.y != 0.0F || this.z != 0.0F) {
			matrixStack.translate(this.x * (1.0f / 16.0f), this.y * (1.0f / 16.0f), this.z * (1.0f / 16.0f));
		}

		if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
			MatrixHelper.rotateZYX(matrixStack.last(), this.zRot, this.yRot, this.xRot);
		}

		if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
			matrixStack.scale(this.xScale, this.yScale, this.zScale);
		}
	}

	@Override
	public IrisModelCuboid[] getCuboids() {
		return this.iris$cuboids;
	}

	@Override
	public boolean isVisible() {
		return this.visible;
	}

	@Override
	public boolean isHidden() {
		return this.skipDraw;
	}

	@Override
	public ModelPart[] getChildren() {
		return this.iris$children;
	}
}
