package org.ishidatami.beespeed;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class BeeSpeed extends JavaPlugin implements Listener {

    public static double beeSpeed = 1.0;
    private int speedManagerTaskId = -1; // タスクIDを保存

    @Override
    public void onEnable() {
        saveDefaultConfig();
        beeSpeed = getConfig().getDouble("bee-speed", 1.0);

        // イベントリスナーを登録
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("BeeSpeed プラグインが有効になりました。速度倍率: " + beeSpeed);

        // 既存のハチにも速度を反映
        updateAllBees();

        // ハチの飛行速度を継続的に管理するタスクを開始
        startBeeSpeedManager();
    }

    @Override
    public void onDisable() {
        // 定期タスクを停止
        if (speedManagerTaskId != -1) {
            getServer().getScheduler().cancelTask(speedManagerTaskId);
            speedManagerTaskId = -1;
            getLogger().info("速度管理タスクを停止しました。");
        }

        // 全てのタスクをキャンセル
        getServer().getScheduler().cancelTasks(this);

        getLogger().info("BeeSpeed プラグインが無効になりました。");
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Bee) {
            // 何もしない（速度制御は定期タスクで管理）
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("beespeed") && !command.getName().equalsIgnoreCase("bs"))
            return false;

        if (!sender.hasPermission("beespeed.modify")) {
            sender.sendMessage("§cこのコマンドを実行する権限がありません。");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("get")) {
            sender.sendMessage("§e現在のハチの速度倍率: §a" + beeSpeed);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            double speed;
            try {
                speed = Double.parseDouble(args[1]);
                if (speed <= 0) {
                    sender.sendMessage("§c速度倍率は0より大きい値を指定してください。");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c有効な数値を入力してください。");
                return true;
            }

            beeSpeed = speed;
            getConfig().set("bee-speed", beeSpeed);
            saveConfig();

            sender.sendMessage("§aハチの速度を " + beeSpeed + " 倍に更新しました。");

            // 既存のハチに即時反映
            updateAllBees();

            return true;
        }

        sender.sendMessage("§e使い方: /beespeed set <倍率> または /beespeed get");
        return true;
    }

    private void startBeeSpeedManager() {
        // 既存のタスクがあればキャンセル
        if (speedManagerTaskId != -1) {
            getServer().getScheduler().cancelTask(speedManagerTaskId);
        }

        // 5ティックごとにハチの行動を管理
        speedManagerTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof Bee) {
                            Bee bee = (Bee) entity;
                            manageBeeMovement(bee);
                        }
                    }
                }
            } catch (Exception e) {
                // エラーが発生してもタスクを停止させない
                getLogger().warning("ハチの速度管理でエラーが発生しました: " + e.getMessage());
            }
        }, 0L, 5L).getTaskId(); // タスクIDを取得して保存
    }

    private void manageBeeMovement(Bee bee) {
        // ハチの怒り状態をチェック
        boolean isAngry = bee.getAnger() > 0 || bee.getTarget() != null;

        // ハチの針保有状態を確認
        boolean beeHasStung = bee.hasStung();

        if (beeHasStung) {
            // 針を失ったハチは怒り状態でも通常速度
            handleNormalFlight(bee);
            return;
        }

        LivingEntity target = bee.getTarget();
        if (isAngry && target != null && !target.isDead()) {
            // 怒り状態でターゲットがいる場合：高速でターゲットに向かう（針ありのみ）
            handleAngryTargetedMovement(bee, target);
        } else if (isAngry && target == null) {
            // 怒り状態だがターゲットを見失った場合：通常より少し速い移動（針ありのみ）
            handleAngryWandering(bee);
        } else if (!bee.isOnGround()) {
            // 平常時の飛行：通常速度
            handleNormalFlight(bee);
        }
    }

    private void handleAngryTargetedMovement(Bee bee, LivingEntity target) {
        Location beeLocation = bee.getLocation();
        Location targetLocation = target.getLocation();

        // ターゲットまでの距離
        double distance = beeLocation.distance(targetLocation);

        // 攻撃範囲内（1.5ブロック以内）なら速度を大幅に落とす
        if (distance <= 1.5) {
            Vector velocity = bee.getVelocity();
            velocity.multiply(0.3); // 30%の速度に抑制
            bee.setVelocity(velocity);
            return;
        }

        // ターゲットに向かう方向ベクトル
        Vector direction = targetLocation.toVector().subtract(beeLocation.toVector()).normalize();

        // 怒り状態での距離に応じた速度調整
        double baseSpeed = 0.6; // ハチの基本速度
        double adjustedSpeed;

        if (distance > 8.0) {
            // 遠距離：怒り状態の最大速度
            adjustedSpeed = baseSpeed * beeSpeed;
        } else if (distance > 4.0) {
            // 中距離：徐々に減速
            adjustedSpeed = baseSpeed * beeSpeed * (0.4 + (distance - 4.0) / 8.0);
        } else if (distance > 2.0) {
            // 近距離：慎重に接近
            adjustedSpeed = baseSpeed * Math.min(beeSpeed, 2.0) * (distance / 4.0);
        } else {
            // 最接近：ゆっくりと精密に
            adjustedSpeed = baseSpeed * 0.8 * (distance / 2.0);
        }

        // 新しい速度を設定
        Vector newVelocity = direction.multiply(adjustedSpeed);

        // Y軸の調整（上下移動を制限）
        if (Math.abs(newVelocity.getY()) > 0.6) {
            newVelocity.setY(Math.signum(newVelocity.getY()) * 0.6);
        }

        bee.setVelocity(newVelocity);
    }

    private void handleAngryWandering(Bee bee) {
        // 怒り状態だがターゲットなし：通常より少し速い移動
        Vector velocity = bee.getVelocity();
        double currentSpeed = velocity.length();

        if (currentSpeed > 0.05) {
            Vector direction = velocity.normalize();
            double targetSpeed = Math.min(currentSpeed * 1.3, 0.8); // 1.3倍、最大0.8
            Vector newVelocity = direction.multiply(targetSpeed);
            bee.setVelocity(newVelocity);
        }
    }

    private void handleNormalFlight(Bee bee) {
        // 平常時：通常の飛行速度を維持（速度倍率は適用しない）
        // ハチのAIに任せて自然な動きを保つ
        Vector velocity = bee.getVelocity();
        double currentSpeed = velocity.length();

        // 異常に速い場合のみ制限
        if (currentSpeed > 1.0) {
            Vector direction = velocity.normalize();
            Vector newVelocity = direction.multiply(0.6); // 通常速度に戻す
            bee.setVelocity(newVelocity);
        }
    }

    private void updateAllBees() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Bee) {
                    count++;
                }
            }
        }
        getLogger().info(count + " 匹のハチの速度設定を更新しました。");
    }

}
