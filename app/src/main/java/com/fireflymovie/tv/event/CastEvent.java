package com.fireflymovie.tv.event;

import com.fireflymovie.tv.bean.Config;
import com.fireflymovie.tv.bean.Device;
import com.fireflymovie.tv.bean.History;

import org.greenrobot.eventbus.EventBus;

public record CastEvent(Config config, Device device, History history) {

    public static void post(Config config, Device device, History history) {
        EventBus.getDefault().post(new CastEvent(config, device, history));
    }
}
