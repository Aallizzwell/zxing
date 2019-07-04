package com.aalizzwell.zxing.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import com.google.zxing.Result;
import com.aalizzwell.zxing.AmbientLightManager;
import com.maizi.zxing.R;
import com.aalizzwell.zxing.bean.InitOption;
import com.aalizzwell.zxing.camera.CameraManager;
import com.aalizzwell.zxing.view.ViewfinderView;

import java.io.IOException;

public class CaptureHelper implements ActivityLifecycle {


    public static final String TAG = CaptureHelper.class.getSimpleName();

    private Activity activity;
    private CameraManager cameraManager;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;
    private SurfaceView surfaceView;
    private ViewfinderView viewfinderView;
    private SurfaceHolder surfaceHolder;
    private SurfaceHolder.Callback surfaceCallback;
    private OnResultCallback onResultCallback;
    public InitOption initOption;
    private CaptureActivityHandler captureHandler;
    private OnHandleDecodeListener onHandleDecodeListener;
    private boolean hasSurface;

    CaptureHelper(Activity activity, InitOption initOption, OnResultCallback onResultCallback, SurfaceView surfaceView, ViewfinderView viewfinderView) {
        this.activity = activity;
        this.surfaceView = surfaceView;
        this.viewfinderView = viewfinderView;
        this.initOption = initOption;
        this.onResultCallback = onResultCallback;
    }

    @Override
    public void onCreate() {
        hasSurface = false;
        surfaceHolder = surfaceView.getHolder();
        inactivityTimer = new InactivityTimer(activity);
        beepManager = new BeepManager(activity, initOption);
        ambientLightManager = new AmbientLightManager(activity);
        surfaceCallback = new SurfaceHolder.Callback() {
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
        onHandleDecodeListener = new OnHandleDecodeListener() {
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
        cameraManager = new CameraManager(activity.getApplication(), initOption);
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
            // Install the surfaceCallback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(surfaceCallback);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView surfaceCallback?");
            return;
        }
        try {
            // 打开Camera硬件设备
            cameraManager.openDriver(surfaceHolder);
            // 创建一个handler来打开预览，并抛出一个运行时异常
            if (captureHandler == null) {
                captureHandler = new CaptureActivityHandler(this, cameraManager, onHandleDecodeListener);
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
        displayFrameworkBugMessageAndExit();
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
            surfaceHolder.removeCallback(surfaceCallback);
        }
    }


    private void onResult(final Result result) {
        if (initOption.isContinuousScan()) {
            if (onResultCallback != null) {
                onResultCallback.onResultCallback(result);
            }
            if (initOption.isAutoRestartPreviewAndDecode()) {
                restartPreviewAndDecode();
            }
            return;
        }
        //如果播放音效，则稍微延迟一点，给予播放音效时间
        if (initOption.isPlayBeep()) {
            captureHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onResultCallback.onResultCallback(result);
                }
            }, 100);
        }
    }


    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public Handler getCaptureHandler() {
        return captureHandler;
    }

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    private void displayFrameworkBugMessageAndExit() {
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
        builder.setTitle("扫一扫");
        builder.setMessage(activity.getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(activity));
        builder.setOnCancelListener(new FinishListener(activity));
        builder.show();
    }

    /**
     * 重新启动扫码和解码器
     */
    private void restartPreviewAndDecode() {
        if (captureHandler != null) {
            captureHandler.restartPreviewAndDecode();
        }
    }

    /**
     * 切换闪光灯
     */
    void switchFlash() {
        cameraManager.switchFlash();
    }

    /**
     * 是否有闪光灯
     */
    static boolean isSupportCameraFlash(PackageManager pm) {
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


    @Override
    public void onDestroy() {
        inactivityTimer.shutdown();
        viewfinderView.stopAnimator();
    }
}
