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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class NoticeDialog extends BaseAlertDialog {

    private DialogNoticeBinding binding;
    private Notice notice;
    private int stage;

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
        // 按钮文案默认「确认/取消」，可由 notice.json buttonText/cancelText 覆盖
        // （新应用名称与叫法由服务器 JSON 决定，APK 内不写死任何新应用名）
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
        // 下载地址完全由 notice.json 下发；JSON 未配地址时按钮不动作（不写死任何兜底 URL）
        if (TextUtils.isEmpty(url)) return;
        view.setEnabled(false);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        // 停止阶段弹窗常驻：从浏览器返回后 onResume 继续拦截；其余阶段跳转后关闭
        if (stage < 3) dismiss();
    }

    // 下载地址全部由 notice.json 下发（flowApkUrl/flowMobileUrl/flowUrl），不写死：
    // 流光侧改仓库名/改 JSON 名/改应用名都只需改服务器 JSON，1.0.8 无需再发版
    private String getFlowUrl() {
        String url = "leanback".equals(BuildConfig.FLAVOR_mode) ? notice.getFlowApkUrl() : notice.getFlowMobileUrl();
        return TextUtils.isEmpty(url) ? notice.getFlowUrl() : url;
    }
}
