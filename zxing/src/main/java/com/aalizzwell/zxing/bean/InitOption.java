package com.aalizzwell.zxing.bean;

import android.support.annotation.ColorRes;

import com.maizi.zxing.R;

import java.io.Serializable;

/**
 * 配置项
 */
public class InitOption implements Serializable {
    //播放声音
    private boolean playBeep = true;
    //震动
    private boolean vibrate = true;
    //解析条形码
    private boolean decodeBarCode = true;
    //全屏扫描
    private boolean fullScreenScan = true;
    //连续扫码
    private boolean continuousScan = false;
    //连扫时，是否自动重置预览和解码器，默认自动重置
    private boolean autoRestartPreviewAndDecode = true;
    //四个角的颜色
    private int frameCornerColor = R.color.frame_corner_color;
    //扫描框颜色
    private int frameLineColor = R.color.frame_corner_color;
    //扫描线颜色
    private int scanLineColor = R.color.scanLineColor;
    //提示文字颜色
    private int tipTextColor = R.color.scanLineColor;
    //提示文字大小
    private String tipTextContent = "将二维码放入输入框，即可自动扫描";
    private int tipTextSize = 15;
    private Boolean isShowTip = true;

    public void setDecodeBarCode(boolean decodeBarCode) {
        this.decodeBarCode = decodeBarCode;
    }

    public int getTipTextColor() {
        return tipTextColor;
    }

    public void setTipTextColor(int tipTextColor) {
        this.tipTextColor = tipTextColor;
    }

    public String getTipTextContent() {
        return tipTextContent;
    }

    public void setTipTextContent(String tipTextContent) {
        this.tipTextContent = tipTextContent;
    }

    public int getTipTextSize() {
        return tipTextSize;
    }

    public void setTipTextSize(int tipTextSize) {
        this.tipTextSize = tipTextSize;
    }

    public Boolean getShowTip() {
        return isShowTip;
    }

    public void setShowTip(Boolean showTip) {
        isShowTip = showTip;
    }

    public InitOption setScanLineColor(@ColorRes int color) {
        this.scanLineColor = color;
        return this;
    }

    public InitOption setFrameLineColor(@ColorRes int color) {
        this.frameLineColor = color;
        return this;
    }

    public InitOption setFrameCornerColor(@ColorRes int color) {
        this.frameCornerColor = color;
        return this;
    }

    public InitOption setAutoRestartPreviewAndDecode(boolean autoRestartPreviewAndDecode) {
        this.autoRestartPreviewAndDecode = autoRestartPreviewAndDecode;
        return this;
    }

    public InitOption setContinuousScan(boolean continuousScan) {
        this.continuousScan = continuousScan;
        return this;
    }

    public InitOption setFullScreenScan(boolean fullScreenScan) {
        this.fullScreenScan = fullScreenScan;
        return this;
    }

    public InitOption setDecodeBarCodep(boolean decodeBarCode) {
        this.decodeBarCode = decodeBarCode;
        return this;
    }

    public InitOption setPlayBeep(boolean playBeep) {
        this.playBeep = playBeep;
        return this;
    }

    public InitOption setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
        return this;
    }

    public boolean isPlayBeep() {
        return playBeep;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public boolean isDecodeBarCode() {
        return decodeBarCode;
    }

    public boolean isFullScreenScan() {
        return fullScreenScan;
    }

    public boolean isContinuousScan() {
        return continuousScan;
    }

    public boolean isAutoRestartPreviewAndDecode() {
        return autoRestartPreviewAndDecode;
    }

    public int getFrameCornerColor() {
        return frameCornerColor;
    }

    public int getFrameLineColor() {
        return frameLineColor;
    }

    public int getScanLineColor() {
        return scanLineColor;
    }
}
