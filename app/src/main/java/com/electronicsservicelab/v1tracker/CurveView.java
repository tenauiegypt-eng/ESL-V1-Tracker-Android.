package com.electronicsservicelab.v1tracker;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;

public class CurveView extends View {
    private final Paint p=new Paint(3);
    private double[] volts=new double[0], milliamps=new double[0];
    private final float[] vx={0.68f,-5.6f}, iy={0.5f,-0.5f};
    private final boolean[] enabled={true,false,true,false};
    private int dragging=-1;
    private float xmin=-8,xmax=8,ymin=-2,ymax=2;
    private float left,top,right,bottom;
    public CurveView(Context c){super(c);setBackgroundColor(Color.rgb(6,24,42));}
    public void setData(double[] v,double[] i){volts=v.clone();milliamps=i.clone();invalidate();}
    public void toggleCursor(int idx){enabled[idx]=!enabled[idx];invalidate();}
    public void clearCursors(){for(int j=0;j<4;j++)enabled[j]=false;invalidate();}
    private float px(float v){return left+(v-xmin)/(xmax-xmin)*(right-left);}
    private float py(float i){return bottom-(i-ymin)/(ymax-ymin)*(bottom-top);}
    private float xval(float x){return xmin+(x-left)/(right-left)*(xmax-xmin);}
    private float yval(float y){return ymin+(bottom-y)/(bottom-top)*(ymax-ymin);}
    private void line(Canvas c,int color,float stroke,float x1,float y1,float x2,float y2){p.setColor(color);p.setStrokeWidth(stroke);p.setStyle(Paint.Style.STROKE);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);
        float d=getResources().getDisplayMetrics().density;
        left=47*d;right=getWidth()-13*d;top=27*d;bottom=getHeight()-40*d;
        if(right<=left||bottom<=top)return;
        p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        for(int j=0;j<=8;j++) {float x=left+(right-left)*j/8;line(c,0xff23425c,d*.7f,x,top,x,bottom);}
        for(int j=0;j<=8;j++) {float y=top+(bottom-top)*j/8;line(c,0xff23425c,d*.7f,left,y,right,y);}
        line(c,0xff5683a0,d,px(0),top,px(0),bottom);line(c,0xff5683a0,d,left,py(0),right,py(0));
        p.setTextSize(11*d);p.setColor(0xff9ac6db);
        for(int j=0;j<=4;j++){float v=xmin+(xmax-xmin)*j/4;c.drawText(String.format(Locale.US,"%.1f",v),px(v)-12*d,bottom+16*d,p);float i=ymin+(ymax-ymin)*j/4;c.drawText(String.format(Locale.US,"%.1f",i),5*d,py(i)+4*d,p);}
        c.drawText("Voltage (V)",right-66*d,getHeight()-5*d,p);c.drawText("Current (mA)",left,17*d,p);
        if(volts.length>1){p.setColor(0xff6ef6c0);p.setStrokeWidth(2.3f*d);p.setStyle(Paint.Style.STROKE);Path path=new Path();boolean first=true;
            for(int j=0;j<volts.length;j++){float x=px((float)volts[j]),y=py((float)milliamps[j]);if(first){path.moveTo(x,y);first=false;}else path.lineTo(x,y);}c.drawPath(path,p);p.setStyle(Paint.Style.FILL);}
        p.setTextSize(12*d);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        for(int j=0;j<4;j++){if(!enabled[j])continue;boolean vertical=j<2;float at=vertical?px(vx[j]):py(iy[j-2]);int color=vertical?0xffffcf72:0xffefafff;
            if(vertical)line(c,color,1.5f*d,at,top,at,bottom);else line(c,color,1.5f*d,left,at,right,at);
            String label=vertical?String.format(Locale.US,"V%d: %+.3f V",j+1,vx[j]):String.format(Locale.US,"I%d: %+.3f mA",j-1,iy[j-2]);
            p.setColor(color);float tw=p.measureText(label);float tx=vertical?Math.max(left,Math.min(right-tw,at+5*d)):left+6*d;
            float ty=vertical?top+15*d+(j*17*d):Math.max(top+14*d,Math.min(bottom-4*d,at-5*d));c.drawText(label,tx,ty,p);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){float best=35*getResources().getDisplayMetrics().density;dragging=-1;
            for(int j=0;j<4;j++){if(!enabled[j])continue;float dist=Math.abs((j<2?px(vx[j])-e.getX():py(iy[j-2])-e.getY()));if(dist<best){best=dist;dragging=j;}}
            return true;
        }if(e.getAction()==MotionEvent.ACTION_MOVE&&dragging>=0){if(dragging<2)vx[dragging]=Math.max(xmin,Math.min(xmax,xval(e.getX())));else iy[dragging-2]=Math.max(ymin,Math.min(ymax,yval(e.getY())));invalidate();return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){dragging=-1;performClick();return true;}return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
