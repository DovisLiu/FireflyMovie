package com.fireflymovie.tv;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.fireflymovie.tv.event.EventIndex;
import com.fireflymovie.tv.setting.Setting;
import com.fireflymovie.tv.ui.activity.CrashActivity;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.List;

import cat.ereza.customactivityoncrash.config.CaocConfig;

public class Startup implements Initializer<Void> {

    @NonNull
    @Override
    public Void create(@NonNull Context context) {
        CaocConfig.Builder.create().trackActivities(true).backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT).errorActivity(CrashActivity.class).apply();
        hookBackgroundCrashes();
        Logger.addLogAdapter(new AndroidLogAdapter(PrettyFormatStrategy.newBuilder().methodCount(0).showThreadInfo(false).tag("TV").build()));
        EventBus.builder().addIndex(new EventIndex()).installDefaultEventBus();
        OkHttp.dns().setDoh(() -> Doh.objectFrom(Setting.getDoh()));
        return null;
    }

    /**
     * 第三方源 jar 常在自己的子线程里抛异常（如 GoProxy 下载 so 失败后 dlopen），
     * 会直接杀死进程并弹崩溃页，甚至造成启动死循环。
     * 非主线程的未捕获异常只记录日志、不杀进程；主线程崩溃仍交回原处理器（CAOC）。
     */
    private void hookBackgroundCrashes() {
        Thread.UncaughtExceptionHandler delegate = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (thread != Looper.getMainLooper().getThread()) {
                Log.e("TV", "Uncaught exception on background thread: " + thread.getName(), throwable);
                return;
            }
            if (delegate != null) delegate.uncaughtException(thread, throwable);
        });
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
