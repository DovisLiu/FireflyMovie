package com.fireflymovie.tv.utils;

public class Github {

    // 更新源优先级：GitHub（直连优先） → Gitee（兜底；实测 Gitee 下载大文件需登录，故 GitHub 优先）
    public static final String[] HOSTS = {
        "https://raw.githubusercontent.com/DovisLiu/Release/fireflymovie",
        "https://gitee.com/dovisliu/Release/raw/fireflymovie"
    };

    // 版本判定 JSON 始终放在 apk/ 根目录，地址固定、不随版本号变化，避免自更新找不到文件
    public static String jsonPath(String name) {
        return "/apk/" + name + ".json";
    }

    // 安装包按版本号归档：apk/{version}/{name}.apk
    // 返回按 HOSTS 顺序排好的候选下载地址（GitHub 在前，Gitee 在后）
    public static String[] getApkUrls(String version, String name) {
        String[] urls = new String[HOSTS.length];
        for (int i = 0; i < HOSTS.length; i++) {
            urls[i] = HOSTS[i] + "/apk/" + version + "/" + name + ".apk";
        }
        return urls;
    }
}
