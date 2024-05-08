package com.ebook.common.util;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.ebook.common.BuildConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * 蓝牙版固件升级下载 用于下载文件
 */
public class DownloadUtil {

    private static final String TAG = "DownloadUtil";

    //下载器
    private DownloadManager downloadManager;
    private Context mContext;
    //下载的ID
    private long downloadId;
    private final String name;
    private final String url;
    public String pathstr;
    public String fileMD5 = "";

    public DownloadUtil(Context context, String url, String name, String fileMd5) {
        this.mContext = context;
        this.name = name;
        this.url = url;
        this.fileMD5 = fileMd5;
    }

    public void startDownload() {
        download(url, name);
    }

    /**
     * 下载apk
     *
     * @param url
     * @param name
     */
    private void download(String url, String name) {
        try {
            //开始下载监听
            if (onDownloadCallback != null) {
                onDownloadCallback.onDownloadStart();
            }
            File firmwareFile = new File(mContext.getExternalFilesDir("/xixi"), name);
            pathstr = firmwareFile.getAbsolutePath();
            Log.i(TAG, "downloadAPK: " + firmwareFile);
            if (hasFile(firmwareFile)) {  //已下载文件，在检测逻辑中已发送下载成功消息
                return;
            }
            // 创建下载任务
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            // 移动网络情况下是否允许漫游
            request.setAllowedOverRoaming(true);
            // 在通知栏中显示，默认就是显示的
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setVisibleInDownloadsUi(true);
            request.setDestinationUri(Uri.fromFile(firmwareFile));
            request.setTitle("下载安装包");
            request.allowScanningByMediaScanner();  //准许被系统扫描到
            // 获取DownloadManager
            if (downloadManager == null)
                downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
            // 将下载请求加入下载队列，加入下载队列后会给该任务返回一个long型的id，通过该id可以取消任务，重启任务、获取下载的文件等等
            if (downloadManager != null) {
                downloadId = downloadManager.enqueue(request);
            }
            //注册广播接收者，监听下载状态
            mContext.registerReceiver(receiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),Context.RECEIVER_EXPORTED);

        } catch (Exception e) {
            Log.e(TAG, "downloadAPK: 下载文件失败", e);
            if (onDownloadCallback != null) {
                onDownloadCallback.onDownloadFailure();
            }
        }
    }

    private boolean hasFile(File file) {
        if (file.exists()) {
            String md5Three = getMD5Three(file);
            md5Three = md5Three.toLowerCase();
            if (md5Three.equals(fileMD5.toLowerCase())) {
                Log.i(TAG, "检测到已下载了文件，不用重复下载");
                installApk(mContext,FileProvider.getUriForFile(mContext, BuildConfig.LIBRARY_PACKAGE_NAME, file));
                if (onDownloadCallback != null) {
                    onDownloadCallback.onDownloadSuccessful();
                }
                return true;
            }
        }
        return false;
    }

    //广播监听下载的各个状态
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkStatus();
        }
    };

    /**
     * 检查下载状态
     */
    private void checkStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        //通过下载的id查找
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            // 已经下载文件大小
            int downloadedSize = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            // 下载文件的总大小
            int totalSize = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            //下载状态
            @SuppressLint("Range") int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                //下载暂停
                case DownloadManager.STATUS_PAUSED:
                    Log.i(TAG, "checkStatus: 下载暂停 " + url);
                    break;
                case DownloadManager.STATUS_PENDING:
                    Log.i(TAG, "checkStatus: 下载滞后");
                    break;
                case DownloadManager.STATUS_RUNNING:
                    Log.i(TAG, "checkStatus: 正在下载...");
                    if (onDownloadCallback != null) {
                        onDownloadCallback.onProgress((int) (downloadedSize * 1.f / totalSize * 100));
                    }
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    Log.i(TAG, "checkStatus: 下载成功 " + url);
                    cursor.close();
                    try {
                        File mDstFile = new File(pathstr);
                        if (mDstFile.exists()) {
                            String md5Three = getMD5Three(mDstFile);
                            md5Three = md5Three.toLowerCase();
                            if (md5Three.equals(fileMD5.toLowerCase())) {
                                Log.i(TAG, "checkStatus: 文件下载完成");
                                installApk(mContext,FileProvider.getUriForFile(mContext, BuildConfig.LIBRARY_PACKAGE_NAME, mDstFile));
                                if (onDownloadCallback != null) {
                                    onDownloadCallback.onDownloadSuccessful();
                                }
                            } else {
                                Log.i(TAG, "checkStatus: 下载完成后 md5校验不通过 云端下发：" + fileMD5 + " 本地生成：" + md5Three);
                                // md5 校验不通过,需要删除下载的文件
                                mDstFile.delete();
                                if (onDownloadCallback != null) {
                                    onDownloadCallback.onDownloadFailure();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "checkStatus: 检查下载状态成功，但是处理文件的时候报错", e);
                    }
                    mContext.unregisterReceiver(receiver);
                    break;
                case DownloadManager.STATUS_FAILED:
                    cursor.close();
                    mContext.unregisterReceiver(receiver);
                    if (onDownloadCallback != null) {
                        onDownloadCallback.onDownloadFailure();
                    }
                    break;
            }
        }
    }

    private void installApk(Context context, Uri apkPath) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(apkPath, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    public static String getMD5Three(File f) {
        byte[] b = null;
        try {
            byte[] buffer = new byte[8192];
            int len;
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(f);
            while ((len = fis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            fis.close();
            b = md.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return toHexString(b);
    }

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        String res = formatter.toString();
        formatter.close();
        return res;
    }

    /**
     * 下载状态监听
     */
    private OnDownloadCallback onDownloadCallback;

    public void setOnDownloadCallback(OnDownloadCallback onDownloadCallback) {
        this.onDownloadCallback = onDownloadCallback;
    }
    public interface OnDownloadCallback {
        void onDownloadStart();

        void onProgress(int progress);

        void onDownloadSuccessful();

        void onDownloadFailure();
    }
}