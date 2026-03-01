# AutoPickup

Paper 插件：玩家挖掘方塊時，掉落物自動進入背包。背包已滿時，多餘物品會掉落在方塊原位。

支援每位玩家獨立的開關狀態與物品過濾清單（白名單 / 黑名單），資料在重啟後仍會保留。

---

## 需求

| 項目 | 版本 |
|------|------|
| 伺服器 | Paper 1.21.x（或相容分支，如 Purpur） |
| Java | 21 |
| 建置 | JDK 21、Maven 3.6+ |

---

## 建置

```bash
mvn clean package
```

產出 JAR：`target/AutoPickup-1.0.0.jar`

> 建置前請將 `JAVA_HOME` 設為 JDK 21；enforcer 會拒絕其他版本。

---

## 安裝

1. 將 `AutoPickup-1.0.0.jar` 放入伺服器 `plugins/` 目錄。
2. 啟動或重載伺服器（`/reload confirm` 或重啟）。
3. 插件會自動建立 `plugins/AutoPickup/config.yml`、`gui.yml`、`lang.yml`。

---

## 指令

別名：`/ap`

| 指令 | 說明 | 權限 |
|------|------|------|
| `/autopickup` | 切換自動收集（開 ↔ 關） | `autopickup.use` |
| `/autopickup on` | 開啟自動收集 | `autopickup.use` |
| `/autopickup off` | 關閉自動收集 | `autopickup.use` |
| `/autopickup mode` | 開啟物品過濾 GUI | `autopickup.mode` |
| `/autopickup reload` | 重新載入設定檔 | `autopickup.reload` |

---

## 權限

| 節點 | 預設 | 說明 |
|------|------|------|
| `autopickup.use` | 所有玩家 | 切換自動收集開關 |
| `autopickup.mode` | 所有玩家 | 使用物品過濾 GUI |
| `autopickup.reload` | OP | 重新載入設定檔 |

---

## 物品過濾 GUI

執行 `/autopickup mode` 開啟 6 列箱子 GUI，可設定哪些物品要收集。

### 過濾模式

| 模式 | 說明 |
|------|------|
| **None**（無） | 收集所有物品（不過濾） |
| **Whitelist**（白名單） | 只收集清單內的物品 |
| **Blacklist**（黑名單） | 收集清單外的所有物品 |

### 操作

- **物品格（0–44）**：點擊切換該物品是否在過濾清單中；綠色勾 = 已選取。
- **分頁**：GUI 底列左右按鈕可翻頁瀏覽所有可用物品。
- **切換模式**：點擊底列中間的「Filter Mode」按鈕可循環切換 None → Whitelist → Blacklist。
- **搜尋**：點擊「Search Items」後在聊天框輸入物品名稱關鍵字（輸入 `cancel` 可取消搜尋）。
- **清空清單**：點擊「Clear Filter List」移除所有已選取物品。
- **關閉**：點擊「Close」或按 `Esc`。

---

## 設定檔

### `config.yml`

```yaml
settings:
  # 未使用過 /autopickup 的新玩家預設狀態
  default-enabled: false

messages:
  toggled-on:    "&aAuto-pickup has been &fenabled&a."
  toggled-off:   "&cAuto-pickup has been &fdisabled&c."
  no-permission: "&cYou do not have permission to use this command."
  players-only:  "&cThis command can only be used by players."
  invalid-usage: "&cUsage: /autopickup [on|off|mode|reload]"
  reloaded:      "&aConfiguration reloaded."
```

訊息支援 `&` 色碼（如 `&a` 綠色）與 MiniMessage 標籤（如 `<green>`、`<#RRGGBB>`）。

### `gui.yml`

控制 GUI 所有文字，包含標題格式、按鈕名稱 / 說明、模式顯示名稱等。支援相同的色碼語法及以下執行期佔位符：

| 佔位符 | 說明 |
|--------|------|
| `{mode}` | 目前過濾模式 |
| `{page}` / `{total_pages}` | 當前頁 / 總頁數 |
| `{search}` / `{search_display}` | 搜尋關鍵字 |
| `{count}` | 過濾清單物品數 |

### `lang.yml`

與 `config.yml` 中的 `messages` 相同結構，未來可依語言切換。

---

## 資料檔

| 檔案 | 說明 |
|------|------|
| `plugins/AutoPickup/players.yml` | 每位玩家的開關狀態（UUID → true/false） |
| `plugins/AutoPickup/filters.yml` | 每位玩家的過濾模式與物品清單 |

伺服器關閉或執行 `/autopickup reload` 時自動儲存；每次切換狀態後也會即時存檔。

---

## PlaceholderAPI（可選）

安裝 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) 後自動啟用：

| 佔位符 | 回傳值 |
|--------|--------|
| `%autopickup%` | `ON` 或 `OFF` |
| `%autopickup_status%` | `ON` 或 `OFF` |
