package com.aalizzwell.zxing.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.aalizzwell.zxing.bean.InitOption;
import com.aalizzwell.zxing.view.ViewfinderView;
import com.google.zxing.Result;
import com.maizi.zxing.R;
import com.aalizzwell.zxing.common.Constant;
import com.aalizzwell.zxing.decode.DecodeImgCallback;
import com.aalizzwell.zxing.decode.DecodeImgThread;
import com.aalizzwell.zxing.utils.ImageUtil;

public class CaptureAlbumActivity extends BaseCaptureActivity implements View.OnClickListener, OnResultCallback {

    private AppCompatImageView flashLightIV;
    private TextView flashLightTV;
    public boolean isOpen = false;
    CaptureHelper captureHelper;
    private InitOption initOption;

    @Override
    public void onClick(View view) {
        int id = view.getId();
        //切换闪光灯
        if (id == R.id.flashLightLayout) {
            captureHelper.switchFlash();
            switchFlashImg(isOpen);
        }
        //打开相册
        else if (id == R.id.albumLayout) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, Constant.REQUEST_IMAGE);
        } else if (id == R.id.backIv) {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //去除标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去除状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initOption = new InitOption();
        super.onCreate(savedInstanceState);
        initView();
        captureHelper = getCaptureHelper();
    }

    @Override
    public int getLayoutView() {
        return R.layout.activity_capture_album;
    }

    @Override
    public ViewfinderView getViewfinderView() {
        return findViewById(R.id.viewfinderView);
    }

    @Override
    public SurfaceView geSurfaceView() {
        return findViewById(R.id.surfaceView);
    }

    @Override
    public InitOption getInitConfig() {

        return initOption;
    }

    @Override
    public OnResultCallback getResultListener() {
        return this;
    }

    @Override
    public void onResultCallback(Result result) {

    }

    private void initView() {
        flashLightIV = findViewById(R.id.flashLightIv);
        flashLightTV = findViewById(R.id.flashLightTv);
        LinearLayoutCompat flashLightLayout = findViewById(R.id.flashLightLayout);
        flashLightLayout.setOnClickListener(this);

        LinearLayoutCompat albumLayout = findViewById(R.id.albumLayout);
        albumLayout.setOnClickListener(this);

        if (CaptureHelper.isSupportCameraFlash(getPackageManager())) {
            flashLightLayout.setVisibility(View.VISIBLE);
        } else {
            flashLightLayout.setVisibility(View.GONE);
        }

    }

    /**
     * 切换闪光灯图片
     */
    public void switchFlashImg(boolean isOpen) {
        isOpen = !isOpen;
        if (isOpen) {
            flashLightIV.setImageResource(R.drawable.ic_close);
            flashLightTV.setText(R.string.open_flash);
        } else {
            flashLightIV.setImageResource(R.drawable.ic_open);
            flashLightTV.setText(R.string.close_flash);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == Constant.REQUEST_IMAGE && resultCode == RESULT_OK) {
            final String path = ImageUtil.getImageAbsolutePath(this, intent.getData());
            new DecodeImgThread(path, new DecodeImgCallback() {
                @Override
                public void onImageDecodeSuccess(Result result) {
                    onResultCallback(result);
                }

                @Override
                public void onImageDecodeFailed() {
                    Toast.makeText(CaptureAlbumActivity.this, R.string.scan_failed_tip, Toast.LENGTH_SHORT).show();
                }
            }).run();
        }
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
