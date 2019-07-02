package com.aalizzwell.zxing.android;

import android.graphics.Bitmap;
import com.google.zxing.Result;

public interface OnHandleDecodeListener {

    void onHandleDecode(Result result, Bitmap barcode, float scaleFactor);
}
