package net.mcreator.commandcookieselectronicmod.client.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.Minecraft;

import net.mcreator.commandcookieselectronicmod.client.model.animations.LaptopAnimations;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

import com.google.gson.Gson;

/** Vanilla keyframe model. Per-face mesh preserves the Blockbench UV mapping. */
public class ModelLaptop<T extends Entity> extends HierarchicalModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("commandcookies_electronic_mod", "model_laptop"), "main");
	public final ModelPart root;
	public final ModelPart lid;
	private final float[][] baseMesh, lidMesh;

	public ModelLaptop(ModelPart root) {
		this.root = root;
		this.lid = root.getChild("lid");
		try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(ResourceLocation.parse("commandcookies_electronic_mod:laptop/laptop_animated_mesh.json"))) {
			var mesh = new Gson().fromJson(reader, float[][][].class);
			baseMesh = mesh[0];
			lidMesh = mesh[1];
		} catch (java.io.IOException e) {
			throw new IllegalStateException("Laptop mesh could not load", e);
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("lid", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 22.1f, -3.5f, -(float) Math.PI / 2, 0, 0));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(T entity, float swing, float amount, float age, float yaw, float pitch) {
	}

	public void animateLid(AnimationState opening, AnimationState closing, float age) {
		root.getAllParts().forEach(ModelPart::resetPose);
		animate(opening, LaptopAnimations.open, age, 1);
		animate(closing, LaptopAnimations.close, age, 1);
	}

	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay, int color) {
		pose.pushPose();
		root.translateAndRotate(pose);
		draw(baseMesh, pose, vertices, light, overlay, color);
		pose.pushPose();
		lid.translateAndRotate(pose);
		draw(lidMesh, pose, vertices, light, overlay, color);
		pose.popPose();
		pose.popPose();
	}

	private static void draw(float[][] faces, PoseStack poses, VertexConsumer out, int light, int overlay, int color) {
		var pose = poses.last();
		for (float[] face : faces)
			for (int v = 0; v < 4; v++) {
				int i = v * 5;
				out.addVertex(pose.pose(), face[i] / 16, face[i + 1] / 16, face[i + 2] / 16).setColor(color).setUv(face[i + 3], face[i + 4]).setOverlay(overlay).setLight(light).setNormal(pose, face[20], face[21], face[22]);
			}
	}
}