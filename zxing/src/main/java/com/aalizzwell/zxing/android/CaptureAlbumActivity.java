package com.aalizzwell.zxing.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.Result;
import com.maizi.zxing.R;
import com.aalizzwell.zxing.common.Constant;
import com.aalizzwell.zxing.decode.DecodeImgCallback;
import com.aalizzwell.zxing.decode.DecodeImgThread;
import com.aalizzwell.zxing.utils.ImageUtil;

public class CaptureAlbumActivity extends BaseCaptureActivity implements View.OnClickListener {

    private AppCompatImageView flashLightIV;
    private TextView flashLightTV;
    public boolean isOpen = false;

    @Override
    public int getLayoutId() {
        return R.layout.activity_capture_album;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        //切换闪光灯
        if (id == R.id.flashLightLayout) {
            captureHelper.switchFlashLight();
            switchFlashImg(isOpen);
        } else if (id == R.id.albumLayout) {
            //打开相册
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
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        flashLightIV = findViewById(R.id.flashLightIv);
        flashLightTV = findViewById(R.id.flashLightTv);
        LinearLayoutCompat flashLightLayout = findViewById(R.id.flashLightLayout);
        flashLightLayout.setOnClickListener(this);

        LinearLayoutCompat albumLayout = findViewById(R.id.albumLayout);
        albumLayout.setOnClickListener(this);

        /*有闪光灯就显示手电筒按钮  否则不显示*/
        if (CaptureHelper.isSupportCameraLedFlash(getPackageManager())) {
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

    /**
     * 扫码结果回调
     *
     * @param result 扫码结果
     */
    @Override
    public boolean onResultCallback(String result) {
        if (initConfig.isContinuousScan()) {//连续扫码时，直接弹出结果
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        }
        return super.onResultCallback(result);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == Constant.REQUEST_IMAGE && resultCode == RESULT_OK) {
            final String path = ImageUtil.getImageAbsolutePath(this, intent.getData());
            new DecodeImgThread(path, new DecodeImgCallback() {
                @Override
                public void onImageDecodeSuccess(Result result) {
                    captureHelper.onResult(result);
                }

                @Override
                public void onImageDecodeFailed() {
                    Toast.makeText(CaptureAlbumActivity.this, R.string.scan_failed_tip, Toast.LENGTH_SHORT).show();
                }
            }).run();
        }
    }
}
