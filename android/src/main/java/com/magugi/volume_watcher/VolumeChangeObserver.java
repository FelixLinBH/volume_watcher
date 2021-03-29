package com.magugi.volume_watcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;
import java.lang.ref.WeakReference;

/**
 * 系统音量监听
 */
public class VolumeChangeObserver {
    public final static String TAG = "volume_watcher";
    private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";

    private VolumeChangeListener mVolumeChangeListener;
    private VolumeBroadcastReceiver mVolumeBroadcastReceiver;
    private Context mContext;
    private AudioManager mAudioManager;
    private boolean mRegistered = false;
    // 最大音量
    private double mMaxVolume;

    private int flag = AudioManager.FLAG_SHOW_UI;

    public interface VolumeChangeListener {
        /**
         * 系统媒体音量变化
         *
         * @param volume
         */
        void onVolumeChanged(double volume);
    }

    public VolumeChangeObserver(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager != null ? mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) : 15;
    }

    /**
     * 获取当前媒体音量
     *
     * @return
     */
    public double getCurrentMusicVolume() {
        int currentMusicVolume = mAudioManager != null ? mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) : -1;
        int maxMusicVolume = mAudioManager != null ? mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) : 15;
        double currentMusic = currentMusicVolume / currentMusicVolume;
        int currentVoiceCallVolume = mAudioManager != null ? mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) : -1;
        int maxVoiceCallVolume = mAudioManager != null ? mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) : 15;
        double currentVoiceCall = currentVoiceCallVolume / maxVoiceCallVolume;
        return (currentMusic > currentVoiceCall) ? currentMusic : currentVoiceCall;
    }

    /**
     * 获取系统最大媒体音量
     *
     * @return
     */
    public double getMaxMusicVolume() {
        return 1.0d;
    }

    /**
     * 设置音量
     * @param value
     */
    public void setVolume(double value){
        double actualValue;
        if (value > 1.0) {
            actualValue = 1.0;
        } else if (value < 0.0) {
            actualValue = 0.0;
        } else {
            actualValue = value;
        }

        int volume = (int)Math.round(actualValue * mMaxVolume);
        if(mAudioManager != null){
            try{
                // 设置音量
                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, 0);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, flag);
                if(volume<1){
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER,  0);
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,  flag);
                }
            }catch (Exception ex){
                //禁止日志
                Log.d(TAG, "setVolume Exception:" + ex.getMessage());
            }
        }
    }

    public VolumeChangeListener getVolumeChangeListener() {
        return mVolumeChangeListener;
    }

    public void setVolumeChangeListener(VolumeChangeListener volumeChangeListener) {
        this.mVolumeChangeListener = volumeChangeListener;
    }

    /**
     * 注册音量广播接收器
     *
     * @return
     */
    public void registerReceiver() {
        mVolumeBroadcastReceiver = new VolumeBroadcastReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(VOLUME_CHANGED_ACTION);
        mContext.registerReceiver(mVolumeBroadcastReceiver, filter);
        mRegistered = true;
    }

    /**
     * 解注册音量广播监听器，需要与 registerReceiver 成对使用
     */
    public void unregisterReceiver() {
        if (mRegistered) {
            try {
                mContext.unregisterReceiver(mVolumeBroadcastReceiver);
                mVolumeChangeListener = null;
                mRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "unregisterReceiver: ", e);
            }
        }
    }

    //监听音量改变
    private static class VolumeBroadcastReceiver extends BroadcastReceiver {
        private WeakReference<VolumeChangeObserver> mObserverWeakReference;

        public VolumeBroadcastReceiver(VolumeChangeObserver volumeChangeObserver) {
            mObserverWeakReference = new WeakReference<>(volumeChangeObserver);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            //媒体音量改变才通知
            if (VOLUME_CHANGED_ACTION.equals(intent.getAction()) && ((intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == AudioManager.STREAM_MUSIC) || (intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1) == AudioManager.STREAM_VOICE_CALL))) {
                VolumeChangeObserver observer = mObserverWeakReference.get();
                if (observer != null) {
                    VolumeChangeListener listener = observer.getVolumeChangeListener();
                    if (listener != null) {
                        double volume = observer.getCurrentMusicVolume();
                        if (volume >= 0) {
                            listener.onVolumeChanged(volume);
                        }
                    }
                }
            }

        }
    }

}
