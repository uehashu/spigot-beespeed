# BeeSpeed

このプラグインは、Minecraft Spigot において、ゲーム内のハチの飛行速度を倍率で制御するプラグインです。

- 対象バージョン：Spigot 1.21.4

## ビルド方法

```
./gradlew clean build
```

生成物: `build/libs/BeeSpeed-<version>.jar`

## インストール

1. JAR をサーバーの `plugins` フォルダに配置
2. サーバー再起動

## コマンド

- `/beespeed set <倍率>` : ハチの飛行速度倍率を変更（デフォルトは 1.0）
- `/beespeed get` : 現在のハチ速度倍率を確認
- `/bs` : beespeed コマンドの省略形

## 権限

- `beespeed.modify` : コマンド実行権限（デフォルト: OP）

## 機能詳細

- ゲーム内のハチの飛行速度を調整
- 設定は即座に全てのハチに反映

## 注意

- 速度倍率は 0 より大きい値のみ有効
- 依存ライブラリは Gradle が自動取得
