
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class LaptopAnimations {
    public static final AnimationDefinition open = AnimationDefinition.Builder.withLength(0.6f)
        .addAnimation("lid", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.15f, KeyframeAnimations.degreeVec(17.578125f, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.45f, KeyframeAnimations.degreeVec(94.921875f, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.6f, KeyframeAnimations.degreeVec(112.5f, 0, 0), AnimationChannel.Interpolations.CATMULLROM))).build();
    public static final AnimationDefinition close = AnimationDefinition.Builder.withLength(0.6f)
        .addAnimation("lid", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0, KeyframeAnimations.degreeVec(112.5f, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.15f, KeyframeAnimations.degreeVec(94.921875f, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.45f, KeyframeAnimations.degreeVec(17.578125f, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.6f, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM))).build();
}
