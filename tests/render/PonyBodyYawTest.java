package top.csituka.magicaland.client.render;

import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class PonyBodyYawTest {
    private static int checks;
    public static void main(String[] args) {
        near(PonyBodyYaw.mounted(45,45),45,"mount turn immediately rotates body even while rider bodyYaw is stale at zero");
        near(PonyBodyYaw.mounted(45,60),45,"small independent head turn does not turn torso away from mount");
        near(PonyBodyYaw.mounted(0,50),0,"vanilla follow threshold is strict");
        near(PonyBodyYaw.mounted(0,60),12,"vanilla soft torso follow after fifty degrees");
        near(PonyBodyYaw.mounted(0,170),102,"vanilla eighty-five-degree clamp plus torso follow");
        near(PonyBodyYaw.mounted(0,-170),-102,"opposite side symmetry");
        for(int mount=-1080;mount<=1080;mount+=15) for(int head=-1080;head<=1080;head+=17) {
            float actual=PonyBodyYaw.mounted(mount,head);
            near(actual,vanilla(mount,head),"matches verified 1.20.1 renderer branch");
            float relative=MathHelper.wrapDegrees(head-actual);
            check(Math.abs(relative)<=68.001f,"riding head remains within existing seventy-degree safety limit");
            var look=PonyHeadLookMath.sample(actual,actual,head,head,0,0,.5f,PonyHeadLookMath.Pose.NORMAL);
            Matrix4f eyeFrame=new Matrix4f().rotateY((float)Math.toRadians(180-actual)).rotateY(look.yaw());
            Vector3f world=eyeFrame.transformDirection(new Vector3f(0,0,-1));
            Vector3f expected=new Vector3f(-(float)Math.sin(Math.toRadians(head)),0,(float)Math.cos(Math.toRadians(head)));
            check(world.distance(expected)<.00001,"head still points in player view direction without double yaw");
            var eye=PonyGazeMath.project(eyeFrame,new Vector3f(expected).mul(5),new Vector3f());
            near(eye.x(),0,"existing inverse eye tracking uses corrected mounted body naturally");
            near(eye.y(),0,"eye pitch stays untouched");
        }
        for(float delta:new float[]{0,.01f,.25f,.5f,.75f,1}) {
            float mount=MathHelper.lerpAngleDegrees(delta,179,-179);
            float head=MathHelper.lerpAngleDegrees(delta,179,-179);
            near(MathHelper.wrapDegrees(PonyBodyYaw.mounted(mount,head)-179),2*delta,"boundary-crossing mount and rider interpolate together");
            float normalBody=MathHelper.lerpAngleDegrees(delta,0,90);
            near(PonyBodyYaw.mounted(normalBody,normalBody),normalBody,"entire turn follows mount at every partial tick");
        }
        System.out.println("PASS PonyBodyYawTest: "+checks+" vanilla mounted yaw, limits, interpolation, head and eye direction checks");
    }
    private static float vanilla(float body,float head) {
        float offset=MathHelper.wrapDegrees(head-body);
        if(offset < -85) offset=-85;
        if(offset >= 85) offset=85;
        body=head-offset;
        if(offset*offset>2500)body+=offset*.2f;
        return body;
    }
    private static void near(float actual,float expected,String label){check(Math.abs(actual-expected)<.00003f,label+": "+actual+" vs "+expected);}
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
