package net.coderbot.iris.compat.sodium.impl.vertex_format.entity_xhfp;

import net.minecraft.client.model.geom.ModelPart;

public interface IrisModelPartData {
	static IrisModelPartData from(ModelPart child) {
		return (IrisModelPartData) (Object) child;
	}

	IrisModelCuboid[] getCuboids();

	ModelPart[] getChildren();

	boolean isVisible();

	boolean isHidden();
}
