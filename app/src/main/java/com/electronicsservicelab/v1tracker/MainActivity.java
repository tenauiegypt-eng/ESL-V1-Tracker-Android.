package com.electronicsservicelab.v1tracker;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final FrameParser parser=new FrameParser();
    private BluetoothSocket socket;
    private volatile boolean connected=false, demo=true;
    private CurveView curve;
    private TextView status,readout;
    private Spinner parts;
    private double[] lastV=new double[0],lastI=new double[0];
    private double phase=0;
    private final String[] names={"Resistor 1 kΩ","Capacitor 100 nF","Inductor 10 H","Silicon diode","Zener 5.6 V","Mixed network"};
    private int blue=0xff143653;
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(10,9,10,9);return t;}
    private Button button(String s,LinearLayout parent,Runnable run){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff116b9e));parent.addView(b);b.setOnClickListener(v->run.run());return b;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff07182b);getWindow().setNavigationBarColor(0xff07182b);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(10,8,10,12);root.setFitsSystemWindows(true);root.setBackgroundColor(0xff0b2035);scroll.addView(root);setContentView(scroll);
        TextView title=text("ELECTRONICS SERVICE LAB  |  V1 TRACKER",18);title.setTextColor(0xff72e7ff);root.addView(title);
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.HORIZONTAL);brand.setGravity(Gravity.CENTER_VERTICAL);root.addView(brand);
        TextView subtitle=text("V–I SIGNATURE ANALYZER",14);subtitle.setTextColor(0xff72e7ff);brand.addView(subtitle);
        ImageView logo=new ImageView(this);try{logo.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("fingerprint_magnifier.png")));}catch(Exception ignored){}
        int iconSize=(int)(44*getResources().getDisplayMetrics().density);brand.addView(logo,new LinearLayout.LayoutParams(iconSize,iconSize));
        brand.setContentDescription("V-I Signature Analyzer with fingerprint magnifier icon");
        status=text("DEMO MODE — simulated data",13);status.setTextColor(0xff80f4ba);root.addView(status);
        LinearLayout connectRow=new LinearLayout(this);connectRow.setOrientation(LinearLayout.HORIZONTAL);root.addView(connectRow);
        button("Connect HC-05",connectRow,()->connect());button("Disconnect",connectRow,()->disconnect());
        parts=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names);parts.setAdapter(a);root.addView(parts);
        LinearLayout modeRow=new LinearLayout(this);modeRow.setOrientation(LinearLayout.HORIZONTAL);root.addView(modeRow);
        button("Demo",modeRow,()->{disconnect();demo=true;status.setText("DEMO MODE — simulated data");});
        button("V1",modeRow,()->curve.toggleCursor(0));button("V2",modeRow,()->curve.toggleCursor(1));
        LinearLayout cursorRow=new LinearLayout(this);cursorRow.setOrientation(LinearLayout.HORIZONTAL);root.addView(cursorRow);
        button("I1",cursorRow,()->curve.toggleCursor(2));button("I2",cursorRow,()->curve.toggleCursor(3));button("Clear cursors",cursorRow,()->curve.clearCursors());
        curve=new CurveView(this);root.addView(curve,new LinearLayout.LayoutParams(-1,(int)(360*getResources().getDisplayMetrics().density)));
        readout=text("V RMS: —     I RMS: —",14);root.addView(readout);
        root.addView(text("Drag a cursor line to read V or mA directly on the graph.\nFor Vf/Vz, read voltage at the selected conduction current.",12));
        root.addView(text("ESL V1 • 50 Hz • Single physical channel • DEMO curves are simulated",11));
        ui.post(new Runnable(){public void run(){if(demo){updateDemo();}ui.postDelayed(this,35);}});
    }
    private void updateDemo(){int n=256,kind=parts.getSelectedItemPosition();double[] v=new double[n],i=new double[n];phase+=.027;
        for(int j=0;j<n;j++){double th=2*Math.PI*j/n+phase,source=7*Math.sin(th);
            if(kind==0){v[j]=source/6;i[j]=v[j];}
            else if(kind==1){v[j]=source;i[j]=.22*Math.cos(th);}
            else if(kind==2){v[j]=source;i[j]=-.22*Math.cos(th);}
            else {double vz=kind==4?5.6:100, vf=.68;double lo=-12,hi=12;
                for(int k=0;k<38;k++){double mid=(lo+hi)/2;double current=diodeI(mid,vf,vz,kind==5);double balance=(source-mid)/5000-current;if(balance>0)lo=mid;else hi=mid;}
                v[j]=(lo+hi)/2;i[j]=1000*(source-v[j])/5000;
            }
        }setSamples(v,i);
    }
    private double softplus(double x){return Math.max(x,0)+Math.log1p(Math.exp(-Math.abs(x)));}
    private double diodeI(double v,double vf,double vz,boolean mixed){double k=.055,rd=35;return k/rd*(softplus((v-vf)/k)-softplus((-v-vz)/k))+(mixed?v/1000:0);}
    private void setSamples(double[] v,double[] i){lastV=v;lastI=i;curve.setData(v,i);double vv=0,ii=0;for(int j=0;j<v.length;j++){vv+=v[j]*v[j];ii+=i[j]*i[j];}
        if(v.length>0)readout.setText(String.format(Locale.US,"V RMS: %.3f V    I RMS: %.3f mA",Math.sqrt(vv/v.length),Math.sqrt(ii/v.length)));
    }
    private void connect(){if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},17);return;}
        BluetoothManager mgr=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);BluetoothAdapter adapter=mgr==null?null:mgr.getAdapter();if(adapter==null){alert("Bluetooth unavailable");return;}
        if(!adapter.isEnabled()){alert("Enable Bluetooth and pair HC-05 in Android Settings, then retry.");return;}
        Set<BluetoothDevice> paired;try{paired=adapter.getBondedDevices();}catch(SecurityException e){alert("Bluetooth permission required");return;}
        ArrayList<BluetoothDevice> devices=new ArrayList<>(paired);if(devices.isEmpty()){alert("No paired devices. Pair HC-05 in Android Bluetooth Settings first.");return;}
        String[] labels=new String[devices.size()];for(int j=0;j<devices.size();j++)labels[j]=devices.get(j).getName()+"  ("+devices.get(j).getAddress()+")";
        new AlertDialog.Builder(this).setTitle("Select paired HC-05").setItems(labels,(dialog,which)->startConnection(devices.get(which))).show();
    }
    private void startConnection(BluetoothDevice device){disconnect();demo=false;status.setText("Connecting to "+device.getName()+"…");
        new Thread(()->{try{BluetoothSocket s=device.createRfcommSocketToServiceRecord(SPP);socket=s;s.connect();connected=true;
                ui.post(()->status.setText("CONNECTED — "+device.getName()+" | 38400 baud UART"));
                byte[] data=new byte[512];InputStream in=s.getInputStream();while(connected){int len=in.read(data);if(len<0)break;
                    for(int[][] samples:parser.feed(data,len)){double[] v=new double[32],i=new double[32];for(int j=0;j<32;j++){
                        // ADC X,Y: 0..1023 -> 0..5V, midpoint 2.5V, analog attenuation 0.25.
                        v[j]=((samples[0][j]*5.0/1023.0)-2.5)/.25;
                        double sense=((samples[1][j]*5.0/1023.0)-2.5)/.25;
                        i[j]=sense/5000.0*1000.0;
                    }ui.post(()->{if(connected)setSamples(v,i);});}
                }
            }catch(Exception e){ui.post(()->status.setText("Bluetooth error: "+e.getMessage()));}
            finally{connected=false;try{if(socket!=null)socket.close();}catch(Exception ignored){}ui.post(()->{if(!demo)status.setText("Disconnected — select Demo or reconnect");});}
        },"HC05-SPP").start();
    }
    private void disconnect(){connected=false;try{if(socket!=null)socket.close();}catch(Exception ignored){}socket=null;}
    private void alert(String msg){new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("OK",null).show();}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==17&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)connect();}
    @Override protected void onDestroy(){disconnect();super.onDestroy();}
}
