package com.fireflymovie.tv;

import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.fireflymovie.tv.impl.UpdateListener;
import com.fireflymovie.tv.setting.Setting;
import com.fireflymovie.tv.ui.dialog.UpdateDialog;
import com.fireflymovie.tv.utils.Download;
import com.fireflymovie.tv.utils.FileUtil;
import com.fireflymovie.tv.utils.Github;
import com.fireflymovie.tv.utils.Notify;
import com.fireflymovie.tv.utils.ResUtil;
import com.fireflymovie.tv.utils.Task;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;

import org.json.JSONObject;

import java.io.File;

public class Updater implements Download.Callback, UpdateListener {

    private UpdateDialog dialog;
    private String version;          // 目标版本号（来自 JSON 的 name），用于拼接 APK 下载路径
    private String[] apkUrls;         // 候选下载地址，按优先级排列（GitHub → Gitee）
    private int apkIndex = 0;         // 当前尝试到第几个下载源
    private Download download;

    private Updater() {
    }

    public static Updater create() {
        return new Updater();
    }

    private File getFile() {
        return Path.cache("update.apk");
    }

    public Updater force() {
        Notify.show(R.string.update_check);
        Setting.putUpdate(true);
        return this;
    }

    public void start(FragmentActivity activity) {
        if (!Setting.getUpdate()) return;
        Task.execute(() -> doInBackground(activity));
    }

    private void doInBackground(FragmentActivity activity) {
        try {
            JSONObject object = fetchJson();
            String name = object.optString("name");
            String desc = object.optString("desc");
            int code = object.optInt("code");
            if (code <= BuildConfig.VERSION_CODE) return;
            this.version = name;
            App.post(() -> show(activity, name, desc));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 短路式"有新版即停"：依次问 服务器 → Gitee → GitHub，任一渠道返回比当前版本新的 code 立即采用；
    // 渠道无响应或返回的不是新版（如服务器忘了同步 JSON）则继续问下一个；全问完取见到的最新一份
    private JSONObject fetchJson() throws Exception {
        JSONObject best = null;
        Exception last = null;
        for (String url : Github.jsonUrls(BuildConfig.FLAVOR_mode)) {
            try {
                JSONObject object = new JSONObject(OkHttp.string(url));
                if (object.optInt("code") > BuildConfig.VERSION_CODE) return object;
                if (best == null || object.optInt("code") > best.optInt("code")) best = object;
            } catch (Exception e) {
                last = e;
            }
        }
        if (best != null) return best;
        throw last != null ? last : new Exception("update check failed");
    }

    private void show(FragmentActivity activity, String version, String desc) {
        dismiss();
        dialog = UpdateDialog.create().title(ResUtil.getString(R.string.update_version, version)).desc(desc).listener(this).show(activity);
    }

    @Override
    public void onConfirm(View view) {
        view.setEnabled(false);
        apkIndex = 0;
        apkUrls = Github.getApkUrls(version, BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_abi);
        startDownload();
    }

    private void startDownload() {
        download = Download.create(apkUrls[apkIndex], getFile());
        download.start(this);
    }

    @Override
    public void onCancel(View view) {
        Setting.putUpdate(false);
        if (download != null) download.cancel();
        dismiss();
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void progress(int progress) {
        if (dialog != null) dialog.setProgress(progress);
    }

    @Override
    public void error(String msg) {
        // 当前下载源失败，自动切换到下一个更新源重试
        if (apkIndex + 1 < apkUrls.length) {
            apkIndex++;
            startDownload();
            return;
        }
        Notify.show(msg);
        dismiss();
    }

    @Override
    public void success(File file) {
        FileUtil.openFile(file);
        dismiss();
    }
}
