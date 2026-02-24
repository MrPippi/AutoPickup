# AutoPickup

Paper/Purpur 插件：玩家挖掘方塊時，掉落物自動進入背包；背包滿時則正常掉落地面。

## 需求

- **建置**：JDK 21+、Maven 3.6+（Paper API 1.21.x 使用 Java 21 編譯，請將 `JAVA_HOME` 設為 JDK 21）
- **運行**：Paper 或 Purpur 1.21.x（Java 21）

## 建置

```bash
# 若已安裝 Maven
mvn clean package

# 或使用專案內 mvnw（會呼叫系統 mvn）
.\mvnw.cmd clean package
```

產出 JAR：`target/AutoPickup-1.0.0.jar`

## 遊戲外測試（不需進遊戲）

在專案目錄執行單元測試，可驗證狀態儲存與指令邏輯是否正常：

```bash
.\mvnw.cmd test
```

- **PlayerStateManagerTest**：測試 `players.yml` 的讀寫、預設值、多玩家狀態是否正確持久化。
- **AutoPickupCommandTest**：測試無權限時送出無權限訊息、非玩家時送出僅玩家可用、玩家執行時切換狀態並存檔。

測試通過即表示核心邏輯正常；實際挖礦與掉落仍需在伺服器內測試。

## 安裝與遊戲內測試

1. 將 `AutoPickup-1.0.0.jar` 放入伺服器 `plugins/`。
2. 啟動或重載伺服器。
3. 進遊戲後執行：
   - `/autopickup` — 切換自動收集（並顯示目前狀態）
   - `/autopickup on` / `/autopickup off` — 直接開啟/關閉
4. 開啟狀態下挖掘方塊，確認掉落物進背包；清空部分背包後再挖，確認多餘物品掉在地上。
5. 重登後再執行 `/autopickup`，確認狀態仍為上次設定（讀取 `plugins/AutoPickup/players.yml`）。
6. （可選）安裝 PlaceholderAPI 後，使用 `%autopickup_status%` 應顯示 ON 或 OFF。

## 權限

- `autopickup.use` — 可使用 `/autopickup`（預設：true）

## 設定

- `config.yml`：`settings.default-enabled`（新玩家預設）、`messages.*`（訊息與顏色碼 `&`）。
