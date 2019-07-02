package com.maizi.zxing.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.maizi.zxing.decode.DecodeFormatManager;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


public class QRCodeUtil {

    private static String TAG = QRCodeUtil.class.getSimpleName();

    /**
     * 识别二维码图片
     * 二维码识别算法主要有两种，分别是HybridBinarizer和GlobalHistogramBinarizer
     * 识别过程：RGB图像→灰度化图像→获取直方图→滤波→识别
     */
    public static Result parseQRCode(String bitmapPath) {
        Result result = null;
        try {
            MultiFormatReader reader = new MultiFormatReader();
            reader.setHints(getDecodeHintType());
            RGBLuminanceSource source = getRGBLuminanceSource(getBitmap(bitmapPath, 400, 400));
            BinaryBitmap hybBitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                result = reader.decodeWithState(hybBitmap);
            } catch (Exception e) {
                BinaryBitmap ghbBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
                try {
                    result = reader.decodeWithState(ghbBitmap);
                } catch (Exception ne) {
                    Log.e(TAG, "parseCode: 解析二维码失败");
                }
            } finally {
                reader.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 生成二维码
     *
     * @param content 文本内容
     * @param size    宽高
     * @return
     */
    public static Bitmap createQRCode(String content, int size) {
        return createQRCode(content, size, null);
    }

    /**
     * 生成二维码
     */
    public static Bitmap createQRCode(String content, int size, Bitmap logo) {
        try {
            //图像数据转换，使用了矩阵转换
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, getEncodeHintType());
            int[] pixels = new int[size * size];
            //创建像素数组,并根据BitMatrix(位矩阵)对象为数组元素赋颜色值 */
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * size + x] = 0xff000000;// 黑色色块像素设置
                    } else {
                        pixels[y * size + x] = 0xffffffff;// 白色色块像素设置
                    }
                }
            }
            //创建Bitmap对象,根据像素数组设置Bitmap每个像素点的颜色值,之后返回Bitmap对象
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);

            if (logo != null) {
                bitmap = addLogo(bitmap, logo);
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 在二维码中间添加Logo图案
     */
    private static Bitmap addLogo(Bitmap src, Bitmap logo) {
        if (src == null || logo == null) {
            return null;
        }
        //获取图片的宽高
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();

        if (srcWidth == 0 || srcHeight == 0) {
            return null;
        }
        if (logoWidth == 0 || logoHeight == 0) {
            return src;
        }
        //logo大小为二维码整体大小的1/6
        float scaleFactor = srcWidth * 1.0f / 6 / logoWidth;
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(src, 0, 0, null);
            canvas.scale(scaleFactor, scaleFactor, srcWidth >> 1, srcHeight >> 1);
            canvas.drawBitmap(logo, (srcWidth - logoWidth) >> 1, (srcHeight - logoHeight) >> 1, null);
            canvas.save();
            canvas.restore();
        } catch (Exception e) {
            bitmap = null;
            e.printStackTrace();
        }

        return bitmap;
    }

    /**
     * 获取RGBLuminanceSource
     * 从相册取到的图片，都是RGB图像
     *
     * @param bitmap QRBitmap
     * @return RGBLuminanceSource
     */
    private static RGBLuminanceSource getRGBLuminanceSource(@NonNull Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        return new RGBLuminanceSource(width, height, pixels);

    }

    /**
     * 根据路径获取图片
     *
     * @param filePath  文件路径
     * @param maxWidth  图片最大宽度
     * @param maxHeight 图片最大高度
     * @return bitmap
     */
    private static Bitmap getBitmap(final String filePath, final int maxWidth, final int maxHeight) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }


    /**
     * Return the sample size.
     *
     * @param options   The options.
     * @param maxWidth  The maximum width.
     * @param maxHeight The maximum height.
     * @return the sample size
     */
    private static int calculateInSampleSize(final BitmapFactory.Options options,
                                             final int maxWidth,
                                             final int maxHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        while ((width >>= 1) >= maxWidth && (height >>= 1) >= maxHeight) {
            inSampleSize <<= 1;
        }
        return inSampleSize;
    }

    /**
     * 配置二维码识别参数
     */
    private static Map<DecodeHintType, Object> getDecodeHintType() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        Vector<BarcodeFormat> decodeFormats = new Vector<>();
        //二维码
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
        //QR_CODE使用hard模式编码，速度精度都有明显上升
        hints.put(DecodeHintType.TRY_HARDER, DecodeFormatManager.QR_CODE_FORMATS);
        //编码格式
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        return hints;
    }

    /**
     * 配置生成二维码参数
     */
    private static Map<EncodeHintType, Object> getEncodeHintType() {
        //配置参数
        Map<EncodeHintType, Object> hints = new HashMap<>();
        //字符转码格式设置
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        //容错级别设置
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        //空白边距设置
        hints.put(EncodeHintType.MARGIN, 1); //default is 4
        return hints;
    }

}
