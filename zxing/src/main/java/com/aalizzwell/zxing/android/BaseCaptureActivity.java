package com.aalizzwell.zxing.android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.*;

import com.aalizzwell.zxing.bean.InitOption;
import com.aalizzwell.zxing.view.ViewfinderView;


public abstract class BaseCaptureActivity extends AppCompatActivity {

    private CaptureHelper captureHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(getLayoutView());
        InitOption initConfig = getInitConfig() == null ? new InitOption() : getInitConfig();
        getViewfinderView().setInitConfig(initConfig);
        captureHelper = new CaptureHelper(this, initConfig, getResultListener(), geSurfaceView(), getViewfinderView());
        captureHelper.onCreate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureHelper.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        captureHelper.onPause();
    }

    public abstract int getLayoutView();

    public abstract ViewfinderView getViewfinderView();

    public abstract SurfaceView geSurfaceView();

    public abstract InitOption getInitConfig();

    public abstract OnResultCallback getResultListener();

    public CaptureHelper getCaptureHelper() {
        return captureHelper;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureHelper.onDestroy();
    }
}
