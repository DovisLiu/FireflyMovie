package com.fireflymovie.tv.playback;

import com.fireflymovie.tv.bean.Result;

public record PlaybackResult<T>(T request, Result result) {
}
