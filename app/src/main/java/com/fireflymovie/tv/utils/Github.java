package com.fireflymovie.tv.utils;

public class Github {

    public static final String URL = "https://raw.githubusercontent.com/DovisLiu/Release/fireflymovie";

    private static String getUrl(String name) {
        return URL + "/apk/" + name;
    }

    public static String getJson(String name) {
        return getUrl(name + ".json");
    }

    public static String getApk(String name) {
        return getUrl(name + ".apk");
    }
}
