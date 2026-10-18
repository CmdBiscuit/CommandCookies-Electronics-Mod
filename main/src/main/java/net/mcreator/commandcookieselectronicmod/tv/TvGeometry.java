package net.mcreator.commandcookieselectronicmod.tv;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Shared geometry keeps image, case, hitboxes and occupied cells aligned. */
public final class TvGeometry {
    public static final int MAX_ANGLE=45,MAX_ROLL=90;
    public static final double[] PIVOT={8,9.5,14.5};
    public static int turns(Direction facing){return switch(facing){case EAST->1;case SOUTH->2;case WEST->3;default->0;};}
    public static Direction widthDirection(Direction facing){return facing.getClockWise();}
    private static double[] angled(double x,double y,double z,int yaw,int pitch,int roll){
        x-=8;y-=9.5;z-=14.5;
        double a=Math.toRadians(roll),c=Math.cos(a),s=Math.sin(a),xx=x*c-y*s;y=x*s+y*c;x=xx;
        a=Math.toRadians(pitch);c=Math.cos(a);s=Math.sin(a);double yy=y*c-z*s,zz=y*s+z*c;
        a=Math.toRadians(yaw);c=Math.cos(a);s=Math.sin(a);
        return new double[]{x*c+zz*s+8,yy+9.5,-x*s+zz*c+14.5};
    }
    public static double extension(int yaw,int pitch){
        return extension(yaw,pitch,0);
    }
    public static double extension(int yaw,int pitch,int roll){
        double back=15.6;
        for(double x:new double[]{-7,23})for(double y:new double[]{.77,18.75})for(double z:new double[]{12.275,15.52})back=Math.max(back,angled(x,y,z,yaw,pitch,roll)[2]);
        return back-15.6+2.4; // Clearance for a mains plug behind a flat wall-mounted screen.
    }
    public static Vec3 point(double x,double y,double z,Direction facing,boolean angled,int yaw,int pitch){
        return point(x,y,z,facing,angled,yaw,pitch,0);
    }
    public static Vec3 point(double x,double y,double z,Direction facing,boolean angled,int yaw,int pitch,int roll){
        if(angled){var p=angled(x,y,z,yaw,pitch,roll);x=p[0];y=p[1];z=p[2]-extension(yaw,pitch,roll);}
        x+=8;
        for(int i=0;i<turns(facing);i++){double xx=16-z;z=x;x=xx;}
        return new Vec3(x/16,y/16,z/16);
    }
    private static AABB box(double[] b,Direction facing,boolean angled,int yaw,int pitch,int roll){
        double x=Double.POSITIVE_INFINITY,y=x,z=x,X=-x,Y=-x,Z=-x;
        for(double xx:new double[]{b[0],b[3]})for(double yy:new double[]{b[1],b[4]})for(double zz:new double[]{b[2],b[5]}){
            var p=point(xx,yy,zz,facing,angled,yaw,pitch,roll);x=Math.min(x,p.x);y=Math.min(y,p.y);z=Math.min(z,p.z);X=Math.max(X,p.x);Y=Math.max(Y,p.y);Z=Math.max(Z,p.z);
        }
        return new AABB(x,y,z,X,Y,Z);
    }
    public static List<AABB> boxes(boolean wall,Direction facing,int yaw,int pitch){
        return boxes(wall,facing,yaw,pitch,0);
    }
    public static List<AABB> boxes(boolean wall,Direction facing,int yaw,int pitch,int roll){
        var out=new ArrayList<AABB>();
        if(wall){
            for(int x=0;x<8;x++)for(int y=0;y<4;y++)out.add(box(new double[]{-7+x*3.75,.77+y*4.495,12.275,-7+(x+1)*3.75,.77+(y+1)*4.495,15.52},facing,true,yaw,pitch,roll));
            out.add(box(new double[]{.5,6,15.4,15.5,12.05,16},facing,false,0,0,0));
            out.add(box(new double[]{7.1,8.6,14.0-extension(yaw,pitch,roll),8.9,10.4,15.65},facing,false,0,0,0));
        }else{
            out.add(box(new double[]{-7,3.27,5.775,23,21.25,8.48},facing,false,0,0,0));
            out.add(box(new double[]{-2,0,3.48,-.4,4.5,9.1},facing,false,0,0,0));
            out.add(box(new double[]{16.4,0,3.48,18,4.5,9.1},facing,false,0,0,0));
        }
        return out;
    }
    public static Map<BlockPos,VoxelShape> cells(boolean wall,Direction facing,int yaw,int pitch){
        return cells(wall,facing,yaw,pitch,0);
    }
    public static Map<BlockPos,VoxelShape> cells(boolean wall,Direction facing,int yaw,int pitch,int roll){
        var result=new LinkedHashMap<BlockPos,VoxelShape>();
        for(var b:boxes(wall,facing,yaw,pitch,roll)){
            for(int x=(int)Math.floor(b.minX+1e-7);x<(int)Math.ceil(b.maxX-1e-7);x++)
                for(int y=(int)Math.floor(b.minY+1e-7);y<(int)Math.ceil(b.maxY-1e-7);y++)
                    for(int z=(int)Math.floor(b.minZ+1e-7);z<(int)Math.ceil(b.maxZ-1e-7);z++){
                        var p=new BlockPos(x,y,z);
                        var clipped=new AABB(Math.max(0,b.minX-x),Math.max(0,b.minY-y),Math.max(0,b.minZ-z),Math.min(1,b.maxX-x),Math.min(1,b.maxY-y),Math.min(1,b.maxZ-z));
                        if(clipped.getXsize()>1e-7&&clipped.getYsize()>1e-7&&clipped.getZsize()>1e-7)result.merge(p,Shapes.create(clipped),Shapes::or);
                    }
        }
        result.putIfAbsent(BlockPos.ZERO,Shapes.empty());return result;
    }
    public static AABB bounds(boolean wall,Direction facing,int yaw,int pitch){
        return bounds(wall,facing,yaw,pitch,0);
    }
    public static AABB bounds(boolean wall,Direction facing,int yaw,int pitch,int roll){
        AABB result=null;for(var b:boxes(wall,facing,yaw,pitch,roll))result=result==null?b:result.minmax(b);return result;
    }
    public static boolean portrait(boolean wall,int roll){return wall&&Math.abs(roll)>45;}
    /** Display fractions are left-to-right/top-to-bottom in the image's upright orientation. */
    public static Vec3 imagePoint(double s,double t,boolean wall,int roll){
        if(portrait(wall,roll)){double next=roll>0?t:1-t;t=roll>0?1-s:s;s=next;}
        return new Vec3((-6.4+28.8*(1-s))/16,((wall?1.7:4.2)+16.2*(1-t))/16,((wall?12.405:5.905)-.012)/16);
    }
    /** x/y are display fractions; u/v are source-image fractions. */
    public record Fit(double x0,double y0,double x1,double y1,float u0,float v0,float u1,float v1){}
    public static Fit fit(int width,int height,boolean fill){
        return fit(width,height,fill,false);
    }
    public static Fit fit(int width,int height,boolean fill,boolean portrait){
        double a=width/(double)Math.max(1,height),target=portrait?9d/16:16d/9;
        if(fill){
            if(a>target){float pad=(float)((1-target/a)/2);return new Fit(0,0,1,1,pad,0,1-pad,1);}
            float pad=(float)((1-a/target)/2);return new Fit(0,0,1,1,0,pad,1,1-pad);
        }
        if(a>target){double h=target/a;return new Fit(0,(1-h)/2,1,(1+h)/2,0,0,1,1);}
        double w=a/target;return new Fit((1-w)/2,0,(1+w)/2,1,0,0,1,1);
    }
    private TvGeometry(){}
}
