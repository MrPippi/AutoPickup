# AutoPickup

Paper 插件：玩家挖掘方塊時，掉落物自動進入背包。背包已滿時，多餘物品會掉落在方塊原位。

支援每位玩家獨立的開關狀態與物品過濾清單（白名單 / 黑名單），資料在重啟後仍會保留。

> **2.0.0 起需要 Java 25 與 Paper 26.2。** 1.21.x 伺服器請繼續使用 AutoPickup 1.x。版本差異見 [CHANGELOG.md](CHANGELOG.md)。

---

## 需求

| 項目 | 版本 |
|------|------|
| 伺服器 | Paper 26.2（或相容分支）；1.21.x 請使用 AutoPickup 1.x |
| Java（伺服器） | 25 |
| 建置 | JDK 25；Maven 由 `./mvnw` 提供（3.9.16），不需另外安裝 |
| 選用插件 | [PlaceholderAPI](#placeholderapi可選)（建議 2.12.3+）、[VeinMiner](#veinminer-整合可選) |

---

## 建置

```bash
./mvnw clean package     # Windows：mvnw.cmd clean package
```

產出 JAR：`target/AutoPickup-2.0.0.jar`

> 建置前請將 `JAVA_HOME` 設為 JDK 25；enforcer 會拒絕其他版本。

`./mvnw clean package` 會一併執行 `src/test` 的單元測試（JUnit 5 + Mockito）。這些是 characterization tests，記錄的是**目前的行為**；遊戲內的 GUI 與事件流程仍需手動測試。GitHub Actions 會在每次 push 到 `main` 與每個 PR 上執行同樣的建置，並把 JAR 上傳成 artifact。

### 依賴

| 依賴 | 版本 | 範圍 | 說明 |
|------|------|------|------|
| `io.papermc.paper:paper-api` | 26.2.build.132-stable | compile（伺服器提供） | 內含 Adventure 5.2.0 |
| `me.clip:placeholderapi` | 2.11.7 | compile、optional | 不打包進 JAR；執行期使用伺服器上安裝的版本 |
| `org.junit.jupiter:junit-jupiter` | 5.14.4 | test | |
| `org.mockito:mockito-core` | 5.24.0 | test | 以 `-javaagent` 載入（surefire `argLine`） |

Maven repositories：`https://repo.papermc.io/repository/maven-public/`、`https://repo.helpch.at/releases/`（PlaceholderAPI；舊網址 `repo.extendedclip.com` 已改為轉址）。

建置插件：maven-compiler 3.16.0、maven-enforcer 3.6.3、maven-surefire 3.6.0、maven-dependency 3.8.1、maven-wrapper 3.2.0。

輸出的 JAR 不含任何第三方函式庫（沒有 shade）。

---

## 安裝

1. 將 `AutoPickup-2.0.0.jar` 放入伺服器 `plugins/` 目錄。
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

  # 拾取時在 ActionBar 顯示收到的物品與數量
  actionbar:
    enabled: true
    # 最後一次拾取後，ActionBar 保留的時間（tick；20 tick = 1 秒）
    display-ticks: 40

messages:
  # …（見下方說明）
```

> **注意：** 目前所有聊天訊息都從 `lang.yml` 讀取，`config.yml` 裡的 `messages:` 區塊**不會被使用**。要修改訊息請編輯 `lang.yml`。

### `gui.yml`

控制 GUI 所有文字，包含標題格式、按鈕名稱 / 說明、模式顯示名稱等。支援相同的色碼語法及以下執行期佔位符：

| 佔位符 | 說明 |
|--------|------|
| `{mode}` | 目前過濾模式 |
| `{page}` / `{total_pages}` | 當前頁 / 總頁數 |
| `{search}` / `{search_display}` | 搜尋關鍵字 |
| `{count}` | 過濾清單物品數 |

### `lang.yml`

所有聊天訊息與 ActionBar 文字的來源。支援 `&` 色碼（如 `&a` 綠色）與 MiniMessage 標籤（如 `<green>`、`<#RRGGBB>`），兩種可以混用。

```yaml
messages:
  toggled-on:    "&aAuto-pickup has been &fenabled&a."
  toggled-off:   "&cAuto-pickup has been &fdisabled&c."
  no-permission: "&cYou do not have permission to use this command."
  players-only:  "&cThis command can only be used by players."
  invalid-usage: "&cUsage: /autopickup [on|off|mode|reload]"
  reloaded:      "&aConfiguration reloaded."

  # ActionBar：{entries} = 各物品文字（以逗號分隔），{total} = 總數量
  actionbar: "&a+ &f{entries}"
  # 每種物品的格式：{item} = 物品翻譯鍵，{count} = 數量
  actionbar-entry: "<translate:{item}> &7x{count}"
```

---

## 資料檔

| 檔案 | 說明 |
|------|------|
| `plugins/AutoPickup/players.yml` | 每位玩家的開關狀態（UUID → true/false） |
| `plugins/AutoPickup/filters.yml` | 每位玩家的過濾模式與物品清單 |

每次切換開關、或在 GUI 中修改過濾設定後會立即存檔；伺服器關閉時也會再存一次。`/autopickup reload` **不會**存檔，而是從磁碟重新讀取設定檔與這兩個資料檔。

---

## PlaceholderAPI（可選）

安裝 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) 後自動啟用（Paper 26.2 建議使用 PlaceholderAPI 2.12.3 以上，該版修正了 Paper 新版本號格式的解析）：

| 佔位符 | 回傳值 |
|--------|--------|
| `%autopickup%` | `ON` 或 `OFF` |
| `%autopickup_status%` | `ON` 或 `OFF` |

---

## VeinMiner 整合（可選）

偵測到 [VeinMiner](https://github.com/MiraculixxT/Veinminer)（插件名稱 `Veinminer`）時會自動啟用：連鎖挖掘產生的掉落物同樣會依開關狀態與過濾清單直接進入背包。整合透過反射讀取 VeinMiner 的事件，若 VeinMiner 的 API 改版，此功能會自動停用而不會報錯。
