package com.yjm.camera.view;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;
import com.yjm.camera.R;
import com.yjm.camera.model.MyCamera;
import com.yjm.camera.service.FloatWindowService;
import com.yjm.camera.util.BitmapUtility;
import com.yjm.camera.util.Preference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "Camera";
    private TextView mTvState;
    private Button mBtnStart;
    private Button mBtnPort;
    private Button mBtnPassword;
    private Button mBtnQuality;
    private ImageView mIVQRCode;
    private Preference mPreference;
    private LocalBroadcastManager mBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvState = (TextView) findViewById(R.id.tv_info);
        mBtnStart = (Button) findViewById(R.id.btn_start);
        mBtnPort = (Button) findViewById(R.id.btn_port);
        mBtnPassword = (Button) findViewById(R.id.btn_password);
        mBtnQuality = (Button) findViewById(R.id.btn_quality);
        mIVQRCode = (ImageView) findViewById(R.id.am_iv_qrcode);
        mBtnStart.setOnClickListener(this);
        mBtnPort.setOnClickListener(this);
        mBtnPassword.setOnClickListener(this);
        mBtnQuality.setOnClickListener(this);
        mPreference = Preference.getInstance(this);
        mTvState.setOnClickListener(this);
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        updateInfo();
    }

    /*
    * 获取IP、端口、密码并显示在文本控件上
    * */
    private void updateInfo() {
        int port = mPreference.getPort();
        if (port == -1) {//首次使用，生成1024-9999范围的端口号
            int max = 9999;
            int min = 1024;
            Random random = new Random();
            port = random.nextInt(max) % (max - min + 1) + min;
            Preference.getInstance(this).setPort(port);
        }
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Preference.getInstance(this).setDeviceId(androidId);
        mTvState.setText("IP地址：" + ip + " 端口号：" + port + "\r\n密码：" + mPreference.getPassword() + "\r\n设备id:" + androidId);
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("IP", ip);
            jsonObject.put("PORT", port);
            jsonObject.put("PASSWORD", mPreference.getPassword());
            jsonObject.put("CAMNUMBER", MyCamera.getNumberOfCameras());
            jsonObject.put("DEVICEID", androidId);
            Bitmap bitmap = BitmapUtility.encodeAsBitmap(jsonObject.toString());
            mIVQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static final int REQUEST_CAMERA = 0;

    private void requestCameraPermission() {
        Log.i(TAG, "CAMERA permission has NOT been granted. Requesting permission.");
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // BEGIN_INCLUDE(camera_permission_request)
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    // Provide an additional rationale to the user if the permission was not granted
                    // and the user would benefit from additional context for the use of the permission.
                    // For example if the user has previously denied the permission.
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA);
                } else {

                    // Camera permission has not been granted yet. Request it directly.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA);
                }
            }
        }
        // END_INCLUDE(camera_permission_request)
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_start) {
            requestCameraPermission();
            Intent intent = new Intent(MainActivity.this, FloatWindowService.class);
            startService(intent);
            finish();
        } else if (v.getId() == R.id.btn_port) {
            portSetting();
        } else if (v.getId() == R.id.btn_password) {
            passwordSetting();
        } else if (v.getId() == R.id.btn_quality) {
            qualitySetting();
        } else if (v.getId() == R.id.tv_info) {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setText(mPreference.getDeviceId());
            Toast.makeText(this, "设备ID已复制", Toast.LENGTH_SHORT).show();
        }
    }

    private void portSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText edtPort = new EditText(this);
        edtPort.setHint("请输入端口号，范围：1024-9999");
        edtPort.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (edtPort.getText().toString().length() == 0) {
                    Toast.makeText(MainActivity.this, "请输入端口号", Toast.LENGTH_SHORT).show();
                } else if (Integer.parseInt(edtPort.getText().toString()) < 1024 || Integer.parseInt(edtPort.getText().toString()) > 9999) {
                    Toast.makeText(MainActivity.this, "请输入1024-9999范围内的端口号", Toast.LENGTH_SHORT).show();
                } else {
                    mPreference.setPort(Integer.parseInt(edtPort.getText().toString()));
                    mBroadcastManager.sendBroadcast(new Intent("com.yjm.camera.RESTART_LISTEN"));   //发送广播通知重新监听
                    updateInfo();
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.setTitle("端口设置");
        dialog.setView(edtPort);
        dialog.show();
    }

    private void passwordSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText edtPassword = new EditText(this);
        edtPassword.setSingleLine();
        edtPassword.setHint("请输入密码，长度至少5位");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (edtPassword.getText().toString().length() < 5) {
                    Toast.makeText(MainActivity.this, "请输入至少5位密码", Toast.LENGTH_SHORT).show();
                } else {
                    mPreference.setPassword(edtPassword.getText().toString());
                    mBroadcastManager.sendBroadcast(new Intent("com.yjm.camera.RESTART_LISTEN"));   //发送广播通知重新监听
                    updateInfo();
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.setTitle("密码设置");
        dialog.setView(edtPassword);
        dialog.show();
    }

    private void qualitySetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.view_quality, null);
        final SeekBar sbQuality = (SeekBar) view.findViewById(R.id.vq_sb_quality);
        final SeekBar sbResolution = (SeekBar) view.findViewById(R.id.vq_sb_resolution);
        final SeekBar sbFPS = (SeekBar) view.findViewById(R.id.vq_sb_fps);
        final SeekBar sbDrop = (SeekBar) view.findViewById(R.id.vq_sb_drop);
        final TextView tvQuality = (TextView) view.findViewById(R.id.vq_tv_quality);
        final TextView tvResolution = (TextView) view.findViewById(R.id.vq_tv_resolution);
        final TextView tvFPS = (TextView) view.findViewById(R.id.vq_tv_fps);
        final TextView tvDrop = (TextView) view.findViewById(R.id.vq_tv_drop);
        final Switch swDrop = (Switch) view.findViewById(R.id.vq_sw_drop);

        sbQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress <= 20) {
                    seekBar.setProgress(20);
                }
                tvQuality.setText(seekBar.getProgress() + " %");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbResolution.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress <= 40) {
                    seekBar.setProgress(40);
                }
                tvResolution.setText(seekBar.getProgress() + " %");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbFPS.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress <= 5) {
                    seekBar.setProgress(5);
                }
                tvFPS.setText(seekBar.getProgress() + " FPS");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbDrop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress <= 50) {
                    seekBar.setProgress(50);
                }
                tvDrop.setText(seekBar.getProgress() + " %");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        swDrop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sbDrop.setEnabled(isChecked);
            }
        });
        sbQuality.setProgress(mPreference.getQuality());
        sbResolution.setProgress(mPreference.getResolution());
        sbFPS.setProgress(mPreference.getFPS());
        swDrop.setChecked(mPreference.getDrop());
        sbDrop.setProgress(mPreference.getDropSimilarity());
        sbDrop.setEnabled(mPreference.getDrop());
        builder.setView(view);
        builder.setTitle("画质调整");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPreference.setQuality(sbQuality.getProgress());
                mPreference.setResolution(sbResolution.getProgress());
                mPreference.setFPS(sbFPS.getProgress());
                mPreference.setDrop(swDrop.isChecked());//是否丢弃相似帧
                mPreference.setDropSimilarity(sbDrop.getProgress());//检测相似帧的相似程度
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create().show();
    }
}