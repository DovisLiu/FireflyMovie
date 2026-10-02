package com.fireflymovie.tv.utils;

public class Github {

    // 自有下载站（EdgeOne 加速，回源轻量服务器静态目录），更新链路第一优先。
    // APK 必须走带版本号的目录（firefly/{版本}/）：版本号进 URL → 缓存键唯一，杜绝旧缓存误发；
    // JSON 平铺在 firefly/ 根（源站 no-cache），保证版本检测即时生效。
    private static final String SERVER = "https://download.dovisliu.cn/firefly";

    // 版本检测顺序：服务器 → Gitee → GitHub，配合 Updater.fetchJson 的"有新版即停"短路。
    // 注意：服务器 JSON 为平铺结构（/{mode}.json），Gitee/GitHub 带 apk/ 前缀，故返回完整 URL。
    public static String[] jsonUrls(String mode) {
        return new String[]{
            SERVER + "/" + mode + ".json",
            "https://gitee.com/dovisliu/Release/raw/fireflymovie/apk/" + mode + ".json",
            "https://raw.githubusercontent.com/DovisLiu/Release/fireflymovie/apk/" + mode + ".json"
        };
    }

    // 下载顺序：服务器（带版本号路径）→ GitHub（Gitee 匿名拉大文件 403，不参与下载）
    public static String[] getApkUrls(String version, String name) {
        return new String[]{
            SERVER + "/" + version + "/" + name + ".apk",
            "https://raw.githubusercontent.com/DovisLiu/Release/fireflymovie/apk/" + version + "/" + name + ".apk"
        };
    }

    // 迁移公告 notice.json 三源：自有服务器 → Gitee → GitHub（与版本检测同序）。
    // 服务器文件在 firefly/ 根（no-cache）；镜像放 apk/notice.json。
    // 注意：公告管道地址写死属于流萤自身，流光的下载地址由 notice.json 内容下发（留空间），互不耦合。
    public static String[] noticeUrls() {
        return new String[]{
            SERVER + "/notice.json",
            "https://gitee.com/dovisliu/Release/raw/fireflymovie/apk/notice.json",
            "https://raw.githubusercontent.com/DovisLiu/Release/fireflymovie/apk/notice.json"
        };
    }
}
