# BeeSpeed

Minecraft Spigot 1.21.4用のハチ速度制御プラグイン

## ビルド方法

```
./gradlew clean build
```
生成物: `build/libs/BeeSpeed-<version>.jar`

## インストール

1. JARをサーバーの `plugins` フォルダに配置
2. サーバー再起動

## コマンド

- `/beespeed set <倍率>` : ハチの怒り時の速度倍率を変更（デフォルトは1.0）
- `/beespeed get` : 現在の倍率を確認
- `/bs` : beespeedコマンドの省略形

## 権限

- `beespeed.modify` : コマンド実行権限（デフォルト: OP）

## 注意

- 速度倍率は0より大きい値のみ有効
- 依存ライブラリはGradleが自動取得