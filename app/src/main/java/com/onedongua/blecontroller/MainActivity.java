package com.onedongua.blecontroller;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.androlua.LuaContext;
import com.androlua.LuaEditor;
import com.androlua.LuaGcable;
import com.androlua.LuaThread;
import com.luajava.JavaFunction;
import com.luajava.LuaException;
import com.luajava.LuaState;
import com.luajava.LuaStateFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("MissingPermission")

public class MainActivity extends AppCompatActivity implements BLESPPUtils.OnBluetoothAction, LuaContext {
    private String filesDir;
    private LuaDexLoader mLuaDexLoader;
    // 蓝牙工具
    private BLESPPUtils mBLESPPUtils;
    // 保存搜索到的设备，避免重复
    private final ArrayList<BluetoothDevice> mDevicesList = new ArrayList<>();
    // 对话框
    private DeviceDialogCtrl mDeviceDialogCtrl;
    private LuaEditor editor;
    private TextView subtitle;
    private boolean bluetoothEnabled;
    private boolean locationEnabled;

    public BLESPPUtils getBLE() {
        return mBLESPPUtils;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar supportActionBar = getSupportActionBar();
        LinearLayout bg = findViewById(R.id.bg);
        View customActionBar = getLayoutInflater().inflate(R.layout.action_bar, null);
        // 设置自定义布局
        if (supportActionBar != null) {
            supportActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.MATCH_PARENT,
                    ActionBar.LayoutParams.MATCH_PARENT);
            supportActionBar.setCustomView(customActionBar, layoutParams);
        } else {
            bg.addView(customActionBar);
        }

        String[] ps = {"(", ")", "[", "]", "{", "}", "\"", "=", ":", ".", ",", "_", "+", "-", "*", "/", "\\", "%", "#", "^", "$", "?", "&", "|", "<", ">", "~", ";", "'"};
        LinearLayout bottom_bar = findViewById(R.id.bottom_bar);
        for (String v : ps) {
            bottom_bar.addView(newButton(v));
        }

        filesDir = getFilesDir().getAbsolutePath();
        mLuaDexLoader = new LuaDexLoader(filesDir, this);
        releaseAssets();
        checkAndRequestPermissions();
        //蓝牙开启后再检测位置服务
        checkBluetooth();

        // 初始化
        mBLESPPUtils = new BLESPPUtils(this, this);
        // 启用蓝牙
        mBLESPPUtils.enableBluetooth();
        // 设置接收停止标志位字符串
        mBLESPPUtils.setStopString("\r");
        // 用户没有开启蓝牙的话打开蓝牙
        if (!mBLESPPUtils.isBluetoothEnable()) mBLESPPUtils.enableBluetooth();
        // 启动工具类
        mBLESPPUtils.onCreate();

        mDeviceDialogCtrl = new DeviceDialogCtrl(this);
        mDeviceDialogCtrl.show();
        if (bluetoothEnabled && locationEnabled) {
            mBLESPPUtils.startDiscovery();
        }

        try {
            initLua();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        editor = new LuaEditor(this);
        bg.addView(editor);
        editor.setText(readLuaFile("main.lua"));

        ImageView play = customActionBar.findViewById(R.id.play);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeLuaFile(editor.getText().toString());

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("输入密码");

                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                input.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                builder.setView(input);

                builder.setPositiveButton("确定", (dialog, which) -> {
                    String password = input.getText().toString();
                    if (password.equals("101011")) {
                        //RunStr task = new RunStr();
                        //task.execute(editor.getText().toString());
                        doString(editor.getText().toString());
                        //mBLESPPUtils.send(("w" + "\n").getBytes());
                    } else {
                        makeToast("密码错误");
                    }
                });
                builder.setNegativeButton("取消", null);
                builder.show();
            }

        });
        play.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                makeToast("运行");
                return true;
            }
        });

        ImageView format = customActionBar.findViewById(R.id.format);
        format.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.format();
            }
        });
        format.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                makeToast("格式化");
                return true;
            }
        });

        ImageView debug = customActionBar.findViewById(R.id.debug);
        debug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doFile("debug.lua", new Object[]{editor.getText().toString()});
            }
        });
        debug.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                makeToast("检查错误");
                return true;
            }
        });

        TextView title = customActionBar.findViewById(R.id.title);
        title.setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("是否重置");
            builder.setPositiveButton("确定", (dialog, which) -> editor.setText(readLuaFile("reset.lua")));
            builder.setNegativeButton("取消", null);
            builder.show();
            return true;
        });

        subtitle = customActionBar.findViewById(R.id.subtitle);

        /*JoystickView joystick = findViewById(R.id.joystick);
        TextView tv_info = findViewById(R.id.tv_info);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                tv_info.setText(angle + "/" + strength);
            }
        });*/
    }

    public TextView newButton(String text) {
        StateListDrawable sd = new StateListDrawable();
        sd.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(0x66000000));
        sd.addState(new int[]{}, new ColorDrawable(0xffffffff));

        TextView btn = new TextView(this); // Assuming context is available
        btn.setTextSize(20f);
        float pd = btn.getTextSize() / 2f;
        btn.setPadding((int) pd, (int) (pd / 2), (int) pd, (int) (pd / 4));
        btn.setText(text);
        btn.setTag(text);
        btn.setTextColor(0xff000000);
        btn.setBackgroundDrawable(sd);

        btn.setOnClickListener(v -> {
            editor.paste((String) v.getTag());
        });

        return btn;
    }

    private void writeLuaFile(String string) {
        try {
            FileOutputStream fos = openFileOutput("main.lua", MODE_PRIVATE);
            fos.write(string.getBytes());
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String readLuaFile(String name) {
        try {
            File file = new File(filesDir, name);
            if (!file.exists()) {
                return "";
            }
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            return new String(buffer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*public class RunStr extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            doString(editor.getText().toString());
            return null;
        }
    }*/

    //当发现新设备
    @Override
    public void onFoundDevice(BluetoothDevice device) {
        //Log.d("BLE", "发现设备 " + device.getName() + device.getAddress());
        // 判断是不是重复的
        for (int i = 0; i < mDevicesList.size(); i++) {
            if (mDevicesList.get(i).getAddress().equals(device.getAddress())) return;
        }
        // 添加，下次有就不显示了
        mDevicesList.add(device);
        // 添加条目到 UI 并设置点击事件
        mDeviceDialogCtrl.addDevice(device, new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                BluetoothDevice clickDevice = (BluetoothDevice) v.getTag();
                makeToast("开始连接:" + clickDevice.getName());
                mBLESPPUtils.connect(clickDevice);
            }
        });
    }


    //当连接成功
    @SuppressLint("SetTextI18n")
    @Override
    public void onConnectSuccess(final BluetoothDevice device) {
        makeToast("连接成功");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                subtitle.setText("已连接" + device.getName());
            }
        });
        mDeviceDialogCtrl.dismiss();
    }

    //当连接失败
    @Override
    public void onConnectFailed(final String msg) {
        makeToast("连接失败:" + msg);
    }

    //当接收到 byte 数组
    @Override
    public void onReceiveBytes(final byte[] bytes) {
        runFunc("onCarReceive", new String(bytes));
        //makeToast("收到数据:" + new String(bytes));
    }

    //当调用接口发送 byte 数组
    @Override
    public void onSendBytes(final byte[] bytes) {
    }

    //当结束搜索设备
    @Override
    public void onFinishFoundDevice() {
    }

    //设备选择对话框控制
    private class DeviceDialogCtrl {
        private final LinearLayout mDialogRootView;
        private final AlertDialog mConnectDeviceDialog;

        DeviceDialogCtrl(Context context) {

            // 根布局
            mDialogRootView = new LinearLayout(context);
            mDialogRootView.setOrientation(LinearLayout.VERTICAL);
            mDialogRootView.setMinimumHeight(700);

            // 容器布局
            ScrollView scrollView = new ScrollView(context);
            scrollView.addView(mDialogRootView,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            700
                    )
            );

            // 构建对话框
            mConnectDeviceDialog = new AlertDialog
                    .Builder(context)
                    .setNegativeButton("刷新", null)
                    .setPositiveButton("退出", null)
                    .create();
            mConnectDeviceDialog.setTitle("选择连接的蓝牙设备");
            mConnectDeviceDialog.setView(scrollView);
            mConnectDeviceDialog.setCancelable(false);
        }

        //显示
        void show() {
            mConnectDeviceDialog.show();
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnLongClickListener(v -> {
                mConnectDeviceDialog.dismiss();
                return false;
            });
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                mConnectDeviceDialog.dismiss();
                finish();
            });
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                mDialogRootView.removeAllViews();
                mDevicesList.clear();
                mBLESPPUtils.startDiscovery();
            });
        }

        //取消对话框
        void dismiss() {
            mConnectDeviceDialog.dismiss();
        }

        //添加一个设备到列表
        private void addDevice(final BluetoothDevice device, final View.OnClickListener onClickListener) {
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    TextView textView = new TextView(MainActivity.this);
                    textView.setClickable(true);
                    textView.setText(device.getName() + "\nMAC:" + device.getAddress());
                    textView.setTextColor(Color.BLACK);
                    textView.setOnClickListener(onClickListener);
                    textView.setTag(device);
                    textView.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                    );
                    ((LinearLayout.LayoutParams) textView.getLayoutParams()).setMargins(
                            64, 32, 64, 32);
                    mDialogRootView.addView(textView);
                }
            });
        }
    }

    public void makeToast(Object str) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, str.toString(), Toast.LENGTH_SHORT).show());
    }

    private final int REQUEST_PERMISSION_CODE = 1001;

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();
            // Android 版本大于等于 12 时，申请新的蓝牙权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions.add(android.Manifest.permission.BLUETOOTH_SCAN);
                permissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE);
                permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);
                //根据实际需要申请定位权限
                permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            }

            List<String> notGrantedPermissions = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    notGrantedPermissions.add(permission);
                }
            }
            if (!notGrantedPermissions.isEmpty()) {
                ActivityCompat.requestPermissions(this, notGrantedPermissions.toArray(new String[0]), REQUEST_PERMISSION_CODE);
            }
        }
    }

    // 处理权限请求的结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    makeToast("未授予权限");
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void checkBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) return;
        if (bluetoothAdapter.isEnabled()) {
            bluetoothEnabled = true;
            checkLocation();
        } else {
            registerBluetoothStateReceiver();
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBluetoothIntent);
        }
    }

    private void checkLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationEnabled = true;
        } else {
            Intent enableLocationIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(enableLocationIntent);
        }
    }

    private BluetoothStateReceiver bluetoothStateReceiver;

    // 注册蓝牙状态改变的广播接收器
    private void registerBluetoothStateReceiver() {
        bluetoothStateReceiver = new BluetoothStateReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);
    }

    // 解除注册蓝牙状态改变的广播接收器
    private void unregisterBluetoothStateReceiver() {
        if (bluetoothStateReceiver != null) {
            unregisterReceiver(bluetoothStateReceiver);
        }
    }

    // 蓝牙状态改变的广播接收器
    private class BluetoothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (bluetoothState == BluetoothAdapter.STATE_ON) {
                    unregisterBluetoothStateReceiver();
                    checkLocation();
                }
            }
        }
    }


    //Lua段开始

    public LuaState L;

    private void initLua() throws Exception {
        L = LuaStateFactory.newLuaState();
        L.openLibs();
        L.pushJavaObject(this);
        L.setGlobal("activity");
        L.getGlobal("activity");
        L.setGlobal("this");
        L.pushContext(this);
        L.getGlobal("luajava");
        L.pushString(getLuaExtDir());
        L.setField(-2, "luaextdir");
        L.pushString(filesDir);
        L.setField(-2, "luadir");
        L.pushString(getLuaPath());
        L.setField(-2, "luapath");
        L.pop(1);

        StringBuilder output = new StringBuilder();

        JavaFunction print = new JavaFunction(L) {
            @Override
            public int execute() throws LuaException {
                if (L.getTop() < 2) {
                    makeToast("");
                    return 0;
                }
                for (int i = 2; i <= L.getTop(); i++) {
                    int type = L.type(i);
                    String val = null;
                    String stype = L.typeName(type);
                    if (stype.equals("userdata")) {
                        Object obj = L.toJavaObject(i);
                        if (obj != null)
                            val = obj.toString();
                    } else if (stype.equals("boolean")) {
                        val = L.toBoolean(i) ? "true" : "false";
                    } else {
                        val = L.toString(i);
                    }
                    if (val == null)
                        val = stype;
                    output.append("\t");
                    output.append(val);
                    output.append("\t");
                }
                makeToast(output.toString().substring(1, output.length() - 1));
                output.setLength(0);
                return 0;
            }
        };
        print.register("print");

        L.getGlobal("package");
        L.pushString(filesDir + "/?.lua;");
        L.setField(-2, "path");
        L.pop(1);

        JavaFunction set = new JavaFunction(L) {
            @Override
            public int execute() throws LuaException {
                LuaThread thread = (LuaThread) L.toJavaObject(2);

                thread.set(L.toString(3), L.toJavaObject(4));
                return 0;
            }
        };
        set.register("set");

        JavaFunction call = new JavaFunction(L) {
            @Override
            public int execute() throws LuaException {
                LuaThread thread = (LuaThread) L.toJavaObject(2);

                int top = L.getTop();
                if (top > 3) {
                    Object[] args = new Object[top - 3];
                    for (int i = 4; i <= top; i++) {
                        args[i - 4] = L.toJavaObject(i);
                    }
                    thread.call(L.toString(3), args);
                } else if (top == 3) {
                    thread.call(L.toString(3));
                }

                return 0;
            }

        };
        call.register("call");

    }

    public Object doString(String funcSrc, Object... args) {
        try {
            L.setTop(0);
            int ok = L.LloadString(funcSrc);

            if (ok == 0) {
                L.getGlobal("debug");
                L.getField(-1, "traceback");
                L.remove(-2);
                L.insert(-2);

                int l = args.length;
                for (Object arg : args) {
                    L.pushObjectValue(arg);
                }

                ok = L.pcall(l, 1, -2 - l);
                if (ok == 0) {
                    return L.toJavaObject(-1);
                }
            }
            throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
        } catch (LuaException e) {
            makeToast(e.getMessage());
            Log.e("run", e.toString());
        }
        return null;
    }

    private String errorReason(int error) {
        switch (error) {
            case 6:
                return "error error";
            case 5:
                return "GC error";
            case 4:
                return "Out of memory";
            case 3:
                return "Syntax error";
            case 2:
                return "Runtime error";
            case 1:
                return "Yield error";
        }
        return "Unknown error " + error;
    }

    //将assets内库释放至程序内部空间
    private void releaseAssets() {
        try {
            AssetManager assetManager = getAssets();
            String[] assetList = assetManager.list("lua");
            if (assetList == null) return;
            for (String asset : assetList) {
                String assetPath = "lua/" + asset;
                InputStream inputStream = assetManager.open(assetPath);
                FileOutputStream outputStream = null;
                outputStream = new FileOutputStream(new File(filesDir, asset));
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            makeToast("释放文件失败");
            Log.e("releaseAssets", e.toString());
        }
    }

    //配置残缺版LuaContext
    @Override
    public ArrayList<ClassLoader> getClassLoaders() {
        return mLuaDexLoader.getClassLoaders();
    }

    public HashMap<String, String> getLibrarys() {
        return mLuaDexLoader.getLibrarys();
    }

    @Override
    public void call(String func, Object... args) {

    }

    @Override
    public void set(String name, Object value) {

    }

    @Override
    public String getLuaPath() {
        return filesDir + "/main.lua";
    }

    @Override
    public String getLuaPath(String path) {
        return new File(getLuaDir(), path).getAbsolutePath();
    }

    @Override
    public String getLuaPath(String dir, String name) {
        return new File(getLuaDir(dir), name).getAbsolutePath();
    }

    @Override
    public String getLuaDir() {
        return filesDir;
    }

    @Override
    public String getLuaDir(String name) {
        File dir = new File(filesDir + "/" + name);
        if (!dir.exists())
            if (!dir.mkdirs())
                return null;
        return dir.getAbsolutePath();
    }

    @Override
    public String getLuaExtDir() {
        return new File(filesDir, "AndroLua").getAbsolutePath();
    }

    @Override
    public String getLuaExtDir(String name) {
        File dir = new File(getLuaExtDir(), name);
        if (!dir.exists())
            if (!dir.mkdirs())
                return dir.getAbsolutePath();
        return dir.getAbsolutePath();
    }

    @Override
    public void setLuaExtDir(String dir) {

    }

    @Override
    public String getLuaExtPath(String path) {
        return new File(getLuaExtDir(), path).getAbsolutePath();
    }

    @Override
    public String getLuaExtPath(String dir, String name) {
        return new File(getLuaExtDir(), name).getAbsolutePath();
    }

    @Override
    public String getLuaLpath() {
        return filesDir + "/?.lua";
    }

    @Override
    public String getLuaCpath() {
        return filesDir + "/lib?.so";
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public LuaState getLuaState() {
        return L;
    }

    @Override
    public Object doFile(String filePath, Object[] args) {
        int ok;
        try {
            if (filePath.charAt(0) != '/')
                filePath = filesDir + "/" + filePath;

            L.setTop(0);
            ok = L.LloadFile(filePath);

            if (ok == 0) {
                L.getGlobal("debug");
                L.getField(-1, "traceback");
                L.remove(-2);
                L.insert(-2);
                int l = args.length;
                for (int i = 0; i < l; i++) {
                    L.pushObjectValue(args[i]);
                }
                ok = L.pcall(l, 1, -2 - l);
                if (ok == 0) {
                    return L.toJavaObject(-1);
                }
            }
            Intent res = new Intent();
            res.putExtra("data", L.toString(-1));
            setResult(ok, res);
            throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
        } catch (LuaException e) {
            sendMsg(e.getMessage());
            String s = e.getMessage();
            String p = "android.permission.";
            int i = s.indexOf(p);
            if (i > 0) {
                i = i + p.length();
                int n = s.indexOf(".", i);
                if (n > i) {
                    String m = s.substring(i, n);
                    L.getGlobal("require");
                    L.pushString("permission");
                    L.pcall(1, 0, 0);
                    L.getGlobal("permission_info");
                    L.getField(-1, m);
                    if (L.isString(-1))
                        m = m + " (" + L.toString(-1) + ")";
                    sendMsg("权限错误: " + m);
                    return null;
                }
            }
        }

        return null;
    }

    @Override
    public void sendMsg(String msg) {
        Log.e("msg", msg);
        makeToast(msg);
    }

    @Override
    public void sendError(String title, Exception msg) {
        Object ret = runFunc("onError", title, msg);
        if (ret != null && ret.getClass() == Boolean.class && (Boolean) ret) {
        } else sendMsg(title + ": " + msg.getMessage());
    }

    private int mWidth;
    private int mHeight;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO: Implement this method
        super.onConfigurationChanged(newConfig);
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        //wm.getDefaultDisplay().getRealMetrics(outMetrics);
        mWidth = outMetrics.widthPixels;
        mHeight = outMetrics.heightPixels;
        runFunc("onConfigurationChanged", newConfig);
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public Map getGlobalData() {
        return null;
    }

    @Override
    public Object getSharedData() {
        return null;
    }

    @Override
    public Object getSharedData(String key) {
        return null;
    }

    @Override
    public Object getSharedData(String key, Object def) {
        return null;
    }

    @Override
    public boolean setSharedData(String key, Object value) {
        return false;
    }

    private final ArrayList<LuaGcable> gclist = new ArrayList<LuaGcable>();

    @Override
    public void regGc(LuaGcable obj) {
        gclist.add(obj);
    }

    public Object runFunc(String funcName, Object... args) {
        if (L != null) {
            synchronized (L) {
                try {
                    L.setTop(0);
                    L.pushGlobalTable();
                    L.pushString(funcName);
                    L.rawGet(-2);
                    if (L.isFunction(-1)) {
                        L.getGlobal("debug");
                        L.getField(-1, "traceback");
                        L.remove(-2);
                        L.insert(-2);

                        int l = args.length;
                        for (Object arg : args) {
                            L.pushObjectValue(arg);
                        }

                        int ok = L.pcall(l, 1, -2 - l);
                        if (ok == 0) {
                            return L.toJavaObject(-1);
                        }
                        throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
                    }
                } catch (LuaException e) {
                    sendError(funcName, e);
                }
            }
        }
        return null;
    }
}
