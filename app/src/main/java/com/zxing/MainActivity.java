package com.zxing;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.maizi.zxing.android.CaptureAlbumActivity;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.hello_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndPermission.with(MainActivity.this)
                        .runtime()
                        .permission(Permission.Group.CAMERA)
                        .onGranted(permissions -> {
                            Intent intent = new Intent(MainActivity.this, CaptureAlbumActivity.class);
                            startActivityForResult(intent, 666);
                        })
                        .onDenied(permissions -> {
                            // Storage permission are not allowed.
                        })
                        .start();
            }
        });

    }
}
