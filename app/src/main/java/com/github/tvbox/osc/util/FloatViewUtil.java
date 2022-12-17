package com.github.tvbox.osc.util;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.CacheManager;
import com.github.tvbox.osc.player.IjkMediaPlayer;
import com.github.tvbox.osc.player.MyVideoView;
import com.github.tvbox.osc.player.TrackInfo;
import com.github.tvbox.osc.player.controller.FloatVodController;
import com.github.tvbox.osc.ui.view.ScaleImage;
import com.lzf.easyfloat.EasyFloat;
import com.lzf.easyfloat.enums.ShowPattern;
import com.lzf.easyfloat.interfaces.OnFloatCallbacks;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;
import xyz.doikki.videoplayer.player.ProgressManager;

/**
 * <pre>
 *     author : derek
 *     time   : 2022/12/03
 *     desc   :
 *     version:
 * </pre>
 */
public class FloatViewUtil {
    private long videoDuration = 0;
    private ScaleImage scaleImage;
    private View fullScreenImage;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable runnable = new DismissRunnable();
    private VodControllerListener listener = new VodControllerListener();

    public static final String FLOAT_TAG = "float_view";
    private FloatVodController floatVodController;

    private MyVideoView myVideoView;
    private VodInfo vodInfo;

    public void openFloat(MyVideoView videoView, String progressKey,
                          JSONObject playConfig, VodInfo vodInfo, boolean isInternalSubTitle) {
        this.myVideoView = videoView;
        this.vodInfo = vodInfo;
        Activity topActivity = ActivityUtils.getTopActivity();
        EasyFloat.dismiss(FLOAT_TAG);
        ProgressManager progressManager = new ProgressManager() {
            @Override
            public void saveProgress(String url, long progress) {
                if (videoDuration == 0) return;
                CacheManager.save(MD5.string2MD5(url), progress);
            }

            @Override
            public long getSavedProgress(String url) {
                int st = 0;
                try {
                    st = playConfig.getInt("st");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                long skip = st * 1000;
                if (CacheManager.getCache(MD5.string2MD5(url)) == null) {
                    return skip;
                }
                long rec = (long) CacheManager.getCache(MD5.string2MD5(url));
                if (rec < skip) return skip;
                return rec;
            }
        };

        EasyFloat.Builder builder = new EasyFloat.Builder(topActivity);
        builder.setLandScape(topActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);

        EasyFloat.with(App.getInstance().getApplicationContext()).setTag(FLOAT_TAG).setShowPattern(ShowPattern.BACKGROUND).setLocation(0, 0).registerCallbacks(new OnFloatCallbacks() {
            @Override
            public void createdResult(boolean b, @Nullable String s, @Nullable View view) {

            }

            @Override
            public void show(@NonNull View view) {
                videoView.requestLayout();
                videoView.resume();
                if (floatVodController != null) {
                    listener.setMyVideoView(videoView);
                    floatVodController.setListener(listener);
                }
                updateSubTitle(videoView, floatVodController);
            }

            @Override
            public void hide(@NonNull View view) {

            }

            @Override
            public void dismiss() {
                if (videoDuration > 0 && videoView != null) {
                    progressManager.saveProgress(progressKey == null ? videoView.getPlayUrl() : progressKey, videoView.getCurrentPosition());
                }
                if (floatVodController != null) {
                    listener = null;
                    floatVodController.setListener(null);
                }
                mHandler.removeCallbacksAndMessages(null);
            }

            @Override
            public void touchEvent(@NonNull View view, @NonNull MotionEvent motionEvent) {
                mHandler.removeCallbacks(runnable);
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (scaleImage != null) {
                        scaleImage.setVisibility(View.VISIBLE);

                    }
                    if (fullScreenImage != null) {
                        fullScreenImage.setVisibility(View.VISIBLE);
                    }
                }
                mHandler.postDelayed(runnable, 6000);

            }

            @Override
            public void drag(@NonNull View view, @NonNull MotionEvent motionEvent) {
                mHandler.removeCallbacks(runnable);

            }

            @Override
            public void dragEnd(@NonNull View view) {
                mHandler.postDelayed(runnable, 6000);

            }
        }).setLayout(R.layout.float_app_scale, view -> {
            RelativeLayout content = view.findViewById(R.id.rlContent);
//                    myVideoView = view.findViewById(R.id.mVideoView);
            floatVodController = new FloatVodController(App.getInstance());
            floatVodController.mSubtitleView.isInternal = isInternalSubTitle;
            String title = vodInfo.name;
            if (vodInfo.seriesMap != null && !vodInfo.seriesMap.isEmpty()) {
                VodInfo.VodSeries vs = vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex);
                title += vs.name;
            }
            floatVodController.setTitle(title);
            videoView.setVideoController(floatVodController);
            videoView.setProgressManager(progressManager);
            ((ViewGroup) videoView.getParent()).removeView(videoView);
            content.addView(videoView, 0);
            listener.setMyVideoView(videoView);
            floatVodController.setListener(listener);
            floatVodController.setPlayerConfig(playConfig);
            videoView.setProgressManager(progressManager);
            topActivity.moveTaskToBack(true);//将应用推到后台
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) content.getLayoutParams();
            scaleImage = view.findViewById(R.id.ivScale);
            scaleImage.setOnScaledListener((x, y, event) -> {
                if (params.width > ScreenUtils.getScreenWidth()) {
                    params.width = ScreenUtils.getScreenWidth();
                } else {//当宽度达到最大的时候，高度不再变化
                    params.height = (int) Math.max(params.height + x, 270);
                }
                params.width = (int) Math.max(params.width + x, 480);
                EasyFloat.updateFloat(FLOAT_TAG, -1, -1, params.width, params.height);
                floatVodController.updateSubInfoTextSize(params.width / 50);
            });
            fullScreenImage = view.findViewById(R.id.ivClose);
            view.findViewById(R.id.ivClose).setOnClickListener(v -> {
                EasyFloat.dismiss(FLOAT_TAG);
                Intent intent = new Intent();
                intent.putExtra("isFromFloat", true);
                intent.putExtra("vodInfo", vodInfo);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(App.getInstance(), topActivity.getClass());
                App.getInstance().startActivity(intent);
            });
        }).show();
    }

    public class DismissRunnable implements Runnable {

        @Override
        public void run() {
            if (scaleImage != null) {
                scaleImage.setVisibility(View.GONE);
            }
            if (fullScreenImage != null) {
                fullScreenImage.setVisibility(View.GONE);
            }
        }
    }

    private class VodControllerListener implements FloatVodController.VodControlListener {

        private MyVideoView myVideoView;

        public void setMyVideoView(MyVideoView videoView) {
            this.myVideoView = videoView;
        }

        @Override
        public void playNext(boolean rmProgress) {
            EventBus.getDefault().post(new PlayChangeEvent("", PlayChangeEvent.NEXT));
        }

        @Override
        public void playPre() {
            EventBus.getDefault().post(new PlayChangeEvent("", PlayChangeEvent.PREVIOUS));
        }

        @Override
        public void changeParse(ParseBean pb) {

        }

        @Override
        public void updatePlayerCfg() {
//                            mVodInfo.playerCfg = mVodPlayerCfg.toString();
//                            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_REFRESH, mVodPlayerCfg));
        }

        @Override
        public void replay(boolean replay) {
            String url = myVideoView.getPlayUrl();
            myVideoView.release();
            myVideoView.setUrl(url);
            myVideoView.start();
        }

        @Override
        public void errReplay() {
        }

        @Override
        public void selectSubtitle() {
        }

        @Override
        public void selectAudioTrack() {
        }

        @Override
        public void prepared() {
            videoDuration = myVideoView.getDuration();
            if (vodInfo == null) return;
            VodInfo.VodSeries vs = vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex);
            String playTitleInfo = vodInfo.name + " " + vs.name;
            floatVodController.setTitle(playTitleInfo);
        }
    }

    void updateSubTitle(MyVideoView mVideoView, FloatVodController floatVodController) {
        if (mVideoView.getMediaPlayer() instanceof IjkMediaPlayer) {
            TrackInfo trackInfo = ((IjkMediaPlayer) (mVideoView.getMediaPlayer())).getTrackInfo();
            if (trackInfo != null && trackInfo.getSubtitle().size() > 0) {
                floatVodController.mSubtitleView.hasInternal = true;
            }
            ((IjkMediaPlayer) (mVideoView.getMediaPlayer())).setOnTimedTextListener(new IMediaPlayer.OnTimedTextListener() {
                @Override
                public void onTimedText(IMediaPlayer mp, IjkTimedText text) {
                    if (floatVodController.mSubtitleView.isInternal) {
                        com.github.tvbox.osc.subtitle.model.Subtitle subtitle = new com.github.tvbox.osc.subtitle.model.Subtitle();
                        subtitle.content = text.getText();
                        floatVodController.mSubtitleView.onSubtitleChanged(subtitle);
                    }
                }
            });
        }

    }

    public static class PlayChangeEvent {
        public static final String PREVIOUS = "previous";
        public static final String NEXT = "next";
        public String mUrl;
        public String mType;

        public PlayChangeEvent(String url, String type) {
            mUrl = url;
            mType = type;
        }
    }
}
