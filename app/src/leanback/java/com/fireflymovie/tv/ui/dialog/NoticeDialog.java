package com.fireflymovie.tv.ui.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fireflymovie.tv.BuildConfig;
import com.fireflymovie.tv.R;
import com.fireflymovie.tv.bean.Notice;
import com.fireflymovie.tv.databinding.DialogNoticeBinding;
import com.fireflymovie.tv.utils.Download;
import com.fireflymovie.tv.utils.FileUtil;
import com.fireflymovie.tv.utils.Notify;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

public class NoticeDialog extends BaseAlertDialog implements Download.Callback {

    private DialogNoticeBinding binding;
    private Notice notice;
    private int stage;
    private Download download;

    public static NoticeDialog create() {
        return new NoticeDialog();
    }

    public NoticeDialog notice(Notice notice) {
        this.notice = notice;
        return this;
    }

    public NoticeDialog stage(int stage) {
        this.stage = stage;
        return this;
    }

    public NoticeDialog show(FragmentActivity activity) {
        // DialogFragment 自身持有独立 mCancelable（默认 true），onStart 时会覆盖 AlertDialog.Builder
        // 的 setCancelable，必须在 DialogFragment 层同步设置，否则 stage>=2 点外部/返回键仍能关闭
        setCancelable(stage == 1);
        show(activity.getSupportFragmentManager(), null);
        return this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(stage == 1);
        return dialog;
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogNoticeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        // 仅提示阶段（stage=1）可点外部/返回键关闭；强制与停止阶段不可关闭
        return builder().setView(getBinding().getRoot()).setCancelable(stage == 1);
    }

    @Override
    protected void initView() {
        binding.title.setText(TextUtils.isEmpty(notice.getTitle()) ? getString(R.string.notice_title) : notice.getTitle());
        binding.desc.setText(TextUtils.isEmpty(notice.getMessage()) ? getString(stage >= 3 ? R.string.notice_stop : R.string.notice_message) : notice.getMessage());
        // 文案（含电视用户说明）由 notice.json 下发，APK 内不写死
        binding.download.setText(TextUtils.isEmpty(notice.getButtonText()) ? getString(R.string.notice_confirm) : notice.getButtonText());
        binding.later.setText(TextUtils.isEmpty(notice.getCancelText()) ? getString(R.string.notice_cancel) : notice.getCancelText());
        binding.later.setVisibility(stage == 1 ? View.VISIBLE : View.GONE);
        binding.exit.setVisibility(stage >= 3 ? View.VISIBLE : View.GONE);
        if (stage >= 3) binding.download.requestFocus();
    }

    @Override
    protected void initEvent() {
        binding.download.setOnClickListener(this::download);
        binding.later.setOnClickListener(view -> dismiss());
        binding.exit.setOnClickListener(view -> requireActivity().finishAffinity());
    }

    private void download(View view) {
        String url = getFlowUrl();
        if (TextUtils.isEmpty(url)) return;
        // 电视端：多数无浏览器/下载器，改为应用内下载 APK 并调起安装器（复用 Updater 的 Download + FileProvider 安装能力）
        if ("leanback".equals(BuildConfig.FLAVOR_mode)) {
            startInternalDownload(url);
        } else {
            view.setEnabled(false);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            if (stage < 3) dismiss();
        }
    }

    private void startInternalDownload(String url) {
        if (download != null) download.cancel();
        binding.download.setEnabled(false);
        download = Download.create(url, Path.cache("notice.apk"));
        download.start(this);
    }

    @Override
    public void progress(int value) {
        if (binding != null) binding.download.setText(value + "%");
    }

    @Override
    public void error(String msg) {
        download = null;
        if (binding != null) {
            binding.download.setEnabled(true);
            binding.download.setText(TextUtils.isEmpty(notice.getButtonText()) ? getString(R.string.notice_confirm) : notice.getButtonText());
        }
        Notify.show(msg);
    }

    @Override
    public void success(File file) {
        download = null;
        FileUtil.openFile(file);
        if (stage < 3) dismiss();
    }

    // 下载地址全部由 notice.json 下发（flowApkUrl/flowMobileUrl/flowUrl），不写死：
    // 流光侧改仓库名/改 JSON 名/改应用名都只需改服务器 JSON，1.0.8 及更早版本无需再发版
    private String getFlowUrl() {
        String url = "leanback".equals(BuildConfig.FLAVOR_mode) ? notice.getFlowApkUrl() : notice.getFlowMobileUrl();
        return TextUtils.isEmpty(url) ? notice.getFlowUrl() : url;
    }
}
