package com.maizi.zxing.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.maizi.zxing.AmbientLightManager;
import com.maizi.zxing.R;
import com.maizi.zxing.bean.InitOption;
import com.maizi.zxing.camera.CameraManager;
import com.maizi.zxing.view.ViewfinderView;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class CaptureHelper implements CaptureLifecycle, CaptureTouchEvent, CaptureManager {


    public static final String TAG = CaptureHelper.class.getSimpleName();

    private Activity activity;

    private CameraManager cameraManager;

    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;

    private SurfaceView surfaceView;
    private ViewfinderView viewfinderView;
    private SurfaceHolder surfaceHolder;
    private SurfaceHolder.Callback callback;

    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, Object> decodeHints;
    private String characterSet;

    private boolean hasSurface;


    private OnCaptureCallback onCaptureCallback;
    public InitOption initConfig;
    private CaptureActivityHandler captureHandler;
    private OnCaptureListener onCaptureListener;

    public CaptureHelper(Activity activity, InitOption initConfig, SurfaceView surfaceView, ViewfinderView viewfinderView) {
        this.activity = activity;
        this.surfaceView = surfaceView;
        this.viewfinderView = viewfinderView;
        this.initConfig = initConfig;
        surfaceHolder = surfaceView.getHolder();
        hasSurface = false;

    }

    @Override
    public void onCreate() {

        hasSurface = false;
        inactivityTimer = new InactivityTimer(activity);
        beepManager = new BeepManager(activity, initConfig);
        ambientLightManager = new AmbientLightManager(activity);

        callback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (holder == null) {
                    Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
                }
                if (!hasSurface) {
                    hasSurface = true;
                    initCamera(holder);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                hasSurface = false;
            }
        };

        onCaptureListener = new OnCaptureListener() {

            @Override
            public void onHandleDecode(Result result, Bitmap barcode, float scaleFactor) {
                inactivityTimer.onActivity();
                beepManager.playBeepSoundAndVibrate();
                onResult(result);
            }

        };
    }

    @Override
    public void onResume() {
        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = new CameraManager(activity.getApplication(), initConfig);
        viewfinderView.setCameraManager(cameraManager);
        surfaceHolder = surfaceView.getHolder();
        //屏幕方向
        activity.setRequestedOrientation(getCurrentOrientation());
        beepManager.updatePrefs();
        inactivityTimer.onResume();
        ambientLightManager.start(cameraManager);
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(callback);
        }
    }

    @Override
    public void onPause() {
        if (captureHandler != null) {
            captureHandler.quitSynchronously();
            captureHandler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        if (!hasSurface) {
            surfaceHolder.removeCallback(callback);
        }
    }

    @Override
    public void onDestroy() {
        inactivityTimer.shutdown();
        viewfinderView.stopAnimator();
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public BeepManager getBeepManager() {
        return null;
    }

    @Override
    public AmbientLightManager getAmbientLightManager() {
        return null;
    }

    @Override
    public InactivityTimer getInactivityTimer() {
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            // 打开Camera硬件设备
            cameraManager.openDriver(surfaceHolder);
            // 创建一个handler来打开预览，并抛出一个运行时异常
            if (captureHandler == null) {
                captureHandler = new CaptureActivityHandler(this, cameraManager, onCaptureListener);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    public void onResult(Result result) {
        final String text = result.getText();
        if (initConfig.isContinuousScan()) {
            if (onCaptureCallback != null) {
                onCaptureCallback.onResultCallback(text);
            }
            if (initConfig.isAutoRestartPreviewAndDecode()) {
                restartPreviewAndDecode();
            }
            return;
        }

        if (initConfig.isPlayBeep()) {//如果播放音效，则稍微延迟一点，给予播放音效时间
            captureHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //如果设置了回调，并且onCallback返回为true，则表示拦截
                    if (onCaptureCallback != null && onCaptureCallback.onResultCallback(text)) {
                        return;
                    }
                    Intent intent = new Intent();
                    intent.putExtra(Intents.Scan.RESULT, text);
                    activity.setResult(Activity.RESULT_OK, intent);
                    activity.finish();
                }
            }, 100);
            return;
        }

        //如果设置了回调，并且onCallback返回为true，则表示拦截
        if (onCaptureCallback != null && onCaptureCallback.onResultCallback(text)) {
            return;
        }
//        Intent intent = new Intent();
//        intent.putExtra(Intents.Scan.RESULT, text);
//        activity.setResult(Activity.RESULT_OK, intent);
//        activity.finish();
    }

    private void displayFrameworkBugMessageAndExit() {
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("扫一扫");
        builder.setMessage(activity.getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(activity));
        builder.setOnCancelListener(new FinishListener(activity));
        builder.show();
    }

    /**
     * @param pm
     * @return 是否有闪光灯
     */
    public static boolean isSupportCameraLedFlash(PackageManager pm) {
        if (pm != null) {
            FeatureInfo[] features = pm.getSystemAvailableFeatures();
            if (features != null) {
                for (FeatureInfo f : features) {
                    if (f != null && PackageManager.FEATURE_CAMERA_FLASH.equals(f.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 重新启动扫码和解码器
     */
    public void restartPreviewAndDecode() {
        if (captureHandler != null) {
            captureHandler.restartPreviewAndDecode();
        }
    }

    /**
     * 设置扫码回调
     *
     * @param callback
     * @return
     */
    public CaptureHelper setOnCaptureCallback(OnCaptureCallback callback) {
        this.onCaptureCallback = callback;
        return this;
    }

    private int getCurrentOrientation() {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            }
            return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        } else {
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
            return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
    }

    /*切换闪光灯*/
    public void switchFlashLight() {
        cameraManager.switchFlashLight();
    }

    public Handler getCaptureHandler() {
        return captureHandler;
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }
}
