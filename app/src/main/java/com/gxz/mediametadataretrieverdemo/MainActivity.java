package com.gxz.mediametadataretrieverdemo;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.gxz.mediametadataretrieverdemo.adapter.VideoEditAdapter;
import com.gxz.mediametadataretrieverdemo.bean.VideoEditInfo;
import com.gxz.mediametadataretrieverdemo.util.DeviceUtils;
import com.gxz.mediametadataretrieverdemo.util.ExtractFrameWorkThread;
import com.gxz.mediametadataretrieverdemo.util.ExtractVideoInfoUtil;
import com.gxz.mediametadataretrieverdemo.util.PictureUtils;

import java.io.File;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private final String path = Environment.getExternalStorageDirectory() + "/2.mp4";
    private final String OutPutFileDirPath = Environment.getExternalStorageDirectory() + "/Extract";

    private TextView mTextViewInfo;
    private ImageView mImageView;
    private RecyclerView mRecyclerView;
    private ExtractVideoInfoUtil mExtractVideoInfoUtil;
    private ExtractFrameWorkThread mExtractFrameWorkThread;
    private VideoEditAdapter videoEditAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initView() {
        mTextViewInfo = (TextView) findViewById(R.id.id_tv_info);
        mImageView = (ImageView) findViewById(R.id.id_image);
        mRecyclerView = (RecyclerView) findViewById(R.id.id_rv);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        videoEditAdapter = new VideoEditAdapter(this,
                (DeviceUtils.getScreenWidth(this)) / 4);
        mRecyclerView.setAdapter(videoEditAdapter);
    }

    private void initData() {
        if (!new File(path).exists()) {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_SHORT).show();
            finish();
        }
        mExtractVideoInfoUtil = new ExtractVideoInfoUtil(path);
    }

    public void onClickExtractInfo(View view) {
        String duration = mExtractVideoInfoUtil.getVideoLength();
        int w = mExtractVideoInfoUtil.getVideoWidth();
        int h = mExtractVideoInfoUtil.getVideoHeight();
        int degree = mExtractVideoInfoUtil.getVideoDegree();
        String mimeType = mExtractVideoInfoUtil.getMimetype();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("duration:").append(duration).append("ms ");
        stringBuilder.append("width:").append(w).append(" ");
        stringBuilder.append("height:").append(h).append(" ");
        stringBuilder.append("degree:").append(degree).append(" ");
        stringBuilder.append("mimeType:").append(mimeType);
        mTextViewInfo.setText(stringBuilder.toString());
    }

    public void onClickExtractOne(View view) {
        String path = mExtractVideoInfoUtil.extractFrames(OutPutFileDirPath);
        Glide.with(this)
                .load("file://" + path)
                .into(mImageView);
    }

    public void onClickExtractMul(View view) {
        long endPosition = Long.valueOf(mExtractVideoInfoUtil.getVideoLength());
        long startPosition = 0;
        int thumbnailsCount = 10;
        int extractW = (DeviceUtils.getScreenWidth(this)) / 4;
        int extractH = DeviceUtils.dip2px(this, 55);
        mExtractFrameWorkThread = new ExtractFrameWorkThread(
                extractW, extractH, mUIHandler, path,
                OutPutFileDirPath, startPosition, endPosition, thumbnailsCount);
        mExtractFrameWorkThread.start();
    }

    private final MainHandler mUIHandler = new MainHandler(this);

    private static class MainHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        MainHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == ExtractFrameWorkThread.MSG_SAVE_SUCCESS) {
                    if (activity.videoEditAdapter != null) {
                        VideoEditInfo info = (VideoEditInfo) msg.obj;
                        activity.videoEditAdapter.addItemVideoInfo(info);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExtractVideoInfoUtil.release();
        if (mExtractFrameWorkThread != null) {
            mExtractFrameWorkThread.stopExtract();
        }
        mUIHandler.removeCallbacksAndMessages(null);
        if (!TextUtils.isEmpty(OutPutFileDirPath)) {
            PictureUtils.deleteFile(new File(OutPutFileDirPath));
        }
    }
}
