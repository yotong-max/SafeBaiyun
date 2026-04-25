# GitHub Actions 自动打包说明

## 触发条件

工作流会在以下情况自动运行：
1. **推送代码**到 `main` 或 `master` 分支
2. **发起 Pull Request**到 `main` 或 `master` 分支
3. **手动触发**（在 Actions 页面点击 "Run workflow"）

## 输出产物

每次构建成功后，你可以在以下位置下载 APK：

### 1. Actions 页面（临时存储）
- 进入仓库的 **Actions** 标签页
- 点击最新的工作流运行记录
- 在 **Artifacts** 区域下载 `app-release` 文件

### 2. Release 页面（永久存储）
- 当你推送一个 tag（如 `v1.2.0`）时，APK 会自动上传到 Release 页面
- 进入仓库的 **Releases** 页面即可下载

## 手动打包步骤

### 方式一：推送代码自动触发
```bash
git add .
git commit -m "更新功能"
git push origin main
```
然后到 GitHub Actions 页面等待构建完成。

### 方式二：手动触发工作流
1. 打开 GitHub 仓库页面
2. 点击 **Actions** 标签
3. 选择 **Build APK** 工作流
4. 点击 **Run workflow** 按钮
5. 选择分支，点击 **Run workflow**

### 方式三：发布 Release 版本
```bash
# 创建并推送 tag
git tag -a v1.2.0 -m "版本 1.2.0"
git push origin v1.2.0
```
GitHub 会自动创建 Release 并上传 APK。

## 注意事项

1. **签名问题**：此工作流构建的是未签名 APK，安装时需要允许"安装未知来源应用"
2. **首次运行**：第一次推送代码后，需要等待约 3-5 分钟完成构建
3. **Artifact 保留期**：Actions 页面的构建产物默认保留 30 天
