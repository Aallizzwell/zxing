package com.maizi.zxing.decode;

import com.google.zxing.Result;
import com.maizi.zxing.utils.QRCodeUtil;

public class DecodeImgThread extends Thread {


    /*图片路径*/
    private String imgPath;
    /*回调*/
    private DecodeImgCallback callback;

    public DecodeImgThread(String imgPath, DecodeImgCallback callback) {

        this.imgPath = imgPath;
        this.callback = callback;
    }

    @Override
    public void run() {
        super.run();
        Result rawResult = QRCodeUtil.parseQRCode(imgPath);
        if (rawResult != null) {
            callback.onImageDecodeSuccess(rawResult);
        } else {
            callback.onImageDecodeFailed();
        }

    }

}
