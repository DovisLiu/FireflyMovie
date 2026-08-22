#!/usr/bin/env bash
# 流萤影视：将 4 个 APK 从 Gitee 的 Release 仓库（apk/ 目录）挂到 Gitee 的 FireflyMovie 发行版附件。
# 适用于本机手动发版，或供 Gitee Go 持续集成复用相同逻辑。
#
# 用法：
#   GITEE_TOKEN=xxx ./gitee_release_sync.sh [tag]
# 若不传 tag，则尝试从当前 git 仓库推断（git describe / git tag --points-at HEAD）。
set -euo pipefail

OWNER="${OWNER:-dovisliu}"
REPO="${REPO:-FireflyMovie}"
RELEASE_REPO="${RELEASE_REPO:-Release}"
TOKEN="${GITEE_TOKEN:?请设置 GITEE_TOKEN 环境变量（Gitee 私人令牌）}"

TAG="${1:-}"
if [ -z "$TAG" ]; then
  TAG=$(git describe --tags --exact-match 2>/dev/null || git tag --points-at HEAD | head -1)
fi
[ -z "$TAG" ] && { echo "无法确定版本 tag"; exit 1; }
echo "版本 tag: $TAG"
TAG_VER="${TAG#v}"

# 轮询等待 Release 仓库 apk/ 的版本与 tag 对齐，避免与 FireflyMovie 镜像竞态导致挂错 APK
META_URL="https://gitee.com/$OWNER/$RELEASE_REPO/raw/fireflymovie/apk/leanback.json?access_token=$TOKEN"
NAME=""
for i in $(seq 1 15); do
  NAME=$(curl -sSL --retry 3 --retry-delay 5 "$META_URL" \
    | python3 -c "import sys,json;print(json.load(sys.stdin).get('name',''))" 2>/dev/null || true)
  if [ "$NAME" = "$TAG_VER" ]; then
    echo "第 $i 次探测：版本匹配 ($NAME == $TAG_VER)"
    break
  fi
  echo "第 $i 次探测：版本未对齐 (JSON name='$NAME', 期望 '$TAG_VER')，等待 Release 镜像追平 ..."
  sleep 15
done
if [ "$NAME" != "$TAG_VER" ]; then
  echo "超时：Release 仓库 apk/ 版本仍与 tag 不一致，放弃本次以免挂错版本"
  exit 1
fi

APKS=(leanback-arm64_v8a.apk leanback-armeabi_v7a.apk mobile-arm64_v8a.apk mobile-armeabi_v7a.apk)
mkdir -p _apks
for f in "${APKS[@]}"; do
  echo "下载 $f ..."
  curl -sSL --retry 3 --retry-delay 5 -o "_apks/$f" "https://gitee.com/$OWNER/$RELEASE_REPO/raw/fireflymovie/apk/$f?access_token=$TOKEN"
  ls -lh "_apks/$f"
done

# 查找或创建 Gitee 发行版
REL_ID=$(curl -sS "https://gitee.com/api/v5/repos/$OWNER/$REPO/releases/tags/$TAG?access_token=$TOKEN" | jq -r '.id // empty')
if [ -z "$REL_ID" ]; then
  echo "创建发行版 $TAG"
  REL_ID=$(curl -sS -X POST "https://gitee.com/api/v5/repos/$OWNER/$REPO/releases?access_token=$TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"tag_name\":\"$TAG\",\"name\":\"$TAG\",\"body\":\"流萤影视 $TAG 发布\",\"target_commitish\":\"main\",\"prerelease\":false}" \
    | jq -r '.id // empty')
fi
[ -z "$REL_ID" ] && { echo "创建发行版失败"; exit 1; }
echo "Gitee 发行版 id: $REL_ID"

# 逐个上传附件（同名跳过，幂等）
EXISTING=$(curl -sS "https://gitee.com/api/v5/repos/$OWNER/$REPO/releases/$REL_ID/attach_files?access_token=$TOKEN" | jq -r '.[].name' || true)
for f in "${APKS[@]}"; do
  if echo "$EXISTING" | grep -qx "$f"; then
    echo "已存在，跳过 $f"
    continue
  fi
  echo "上传 $f ..."
  curl -sSL --retry 3 --retry-delay 5 --max-time 600 -X POST "https://gitee.com/api/v5/repos/$OWNER/$REPO/releases/$REL_ID/attach_files?access_token=$TOKEN" \
    -F "file=@_apks/$f"
  echo "  -> done"
done
echo "完成。"
