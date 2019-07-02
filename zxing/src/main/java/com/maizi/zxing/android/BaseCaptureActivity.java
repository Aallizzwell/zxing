package com.maizi.zxing.android;

import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import com.maizi.zxing.bean.InitOption;
import com.maizi.zxing.R;
import com.maizi.zxing.view.ViewfinderView;
import com.maizi.zxing.common.Constant;


public class BaseCaptureActivity extends AppCompatActivity implements OnCaptureCallback {

    public CaptureHelper captureHelper;
    public InitOption initConfig;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        int layoutId = getLayoutId();
        if (isContentView(layoutId)) {
            setContentView(layoutId);
        }
        /*先获取配置信息*/
        try {
            initConfig = (InitOption) getIntent().getSerializableExtra(Constant.INTENT_INIT_OPTION);
        } catch (Exception e) {
            Log.i("initConfig", e.toString());
        }
        if (initConfig == null) {
            initConfig = new InitOption();
        }
        SurfaceView surfaceView = findViewById(getSurfaceViewId());
        ViewfinderView viewfinderView = findViewById(getViewfinderViewId());
        viewfinderView.setInitConfig(initConfig);
        captureHelper = new CaptureHelper(this, initConfig, surfaceView, viewfinderView);
        captureHelper.setOnCaptureCallback(this);
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

    /**
     * 布局id
     *
     * @return
     */
    public int getLayoutId() {
        return R.layout.activity_capture;
    }

    /**
     * 返回true时会自动初始化{@link #setContentView(int)}，返回为false是需自己去初始化{@link #setContentView(int)}
     *
     * @param layoutId
     * @return 默认返回true
     */
    public boolean isContentView(@LayoutRes int layoutId) {
        return true;
    }


    /**
     * ViewfinderView的id
     *
     * @return id
     */
    public int getViewfinderViewId() {
        return R.id.viewfinderView;
    }

    /**
     * 预览界面SurfaceView的id
     *
     * @return id
     */
    public int getSurfaceViewId() {
        return R.id.surfaceView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        captureHelper.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    /**
     * 接收扫码结果回调
     *
     * @param result 扫码结果
     * @return 返回true表示拦截，将不自动执行后续逻辑，为false表示不拦截，默认不拦截
     */
    @Override
    public boolean onResultCallback(String result) {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureHelper.onDestroy();
    }
}
