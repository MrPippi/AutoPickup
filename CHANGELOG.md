# Changelog

本專案的重要變更都記錄在這裡。格式參考 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)，版本號遵循 [Semantic Versioning](https://semver.org/lang/zh-TW/)。

## [Unreleased]

### 變更

- 所有 GitHub Actions 改用完整的 commit SHA 固定版本：`actions/checkout` v7.0.1、`actions/setup-java` v6.0.1（Build 與 Release workflow）、`actions/upload-artifact` v7.0.2（Build workflow）。避免上游 tag 被改動時執行到不同的程式碼，Release workflow 有 `contents: write` 權限，這點尤其重要。

## [2.0.1] - 2026-10-09

插件功能與 2.0.0 相同，伺服器需求不變（Java 25、Paper 26.2），可直接替換 JAR。

### 變更

- 測試依賴：JUnit 5.14.4 → 6.1.3。只影響測試；除了版本號，JAR 內容與 2.0.0 相同。81 個測試不需修改即全數通過。

## [2.0.0] - 2026-10-09

### ⚠️ 不相容變更

- **伺服器需要 Java 25 與 Paper 26.2。** 外掛改以 Java 25 編譯（class 版本 69），`plugin.yml` 的 `api-version` 從 `'1.21'` 改為 `'26.2'`。這個版本**無法**在 Java 21 或 Paper 1.21.x 伺服器上載入；1.21.x 請繼續使用 1.x。
- **建置需要 JDK 25。** maven-enforcer 現在只接受 `[25,26)`。

### 變更

- Paper API：`1.21.1-R0.1-SNAPSHOT` → `1.21.11-R0.1-SNAPSHOT` → `26.2.build.132-stable`。Adventure 隨之從 4.17 升到 5.2.0。主程式碼不需修改，編譯時也沒有 deprecation 警告。
- PlaceholderAPI（compile-only）：2.11.6 → 2.11.7。PAPI 不會打包進 JAR，執行期使用的是伺服器上安裝的版本。
- PlaceholderAPI 的 Maven repository 改為 `https://repo.helpch.at/releases/`。舊網址 `repo.extendedclip.com` 現在只會轉址，在有網路白名單的環境會導致建置失敗。
- Maven 插件（都維持 3.x）：compiler 3.11.0 → 3.16.0、enforcer 3.4.1 → 3.6.3、surefire 3.2.5 → 3.6.0。Java 版本改由單一的 `maven.compiler.release` 屬性設定。
- 測試依賴：JUnit 5.10.2 → 5.14.4、Mockito 5.11.0 → 5.24.0。surefire 3.6.0 之後 Mockito 無法在測試 JVM 中 self-attach，因此改用 `-javaagent` 載入，由 maven-dependency-plugin 的 `properties` goal 提供 jar 路徑。
- **`config.yml` 的 `messages:` 現在會生效。** 以前所有訊息都只從 `lang.yml` 讀取，`config.yml` 裡的訊息改了也沒反應。現在的規則是：`config.yml` 中被改過的訊息（跟內建預設不同，或是內建沒有的 key）優先；其餘從 `lang.yml` 讀取；兩邊都沒有就用內建預設。ActionBar 模板（`messages.actionbar`、`messages.actionbar-entry`）也套用同一套規則。原本在 `lang.yml` 改過的訊息不受影響。
- 文件：README 新增依賴說明、ActionBar 設定與 VeinMiner 整合的說明，並修正 `/autopickup reload` 的描述：它不會存檔，而是重新讀取檔案。

### 新增

- Maven Wrapper（`mvnw` / `mvnw.cmd`），固定使用 Maven 3.9.16。`.gitattributes` 讓 `*.cmd` 使用 CRLF 換行。
- GitHub Actions（`.github/workflows/build.yml`）：每次 push 到 `main` 和每個 PR 都會用 JDK 25 執行 `./mvnw -B verify`，並上傳 JAR。使用 `actions/checkout@v7`、`actions/setup-java@v6`、`actions/upload-artifact@v7`（皆為 Node.js 24 runtime）。
- Release workflow（`.github/workflows/release.yml`）：推送 `v*` tag，或在 Actions 頁面手動執行（可指定 commit、JDK 版本），就會建置 JAR 並發布成 GitHub Release，說明文字取自本檔案對應的版本段落。
- 本檔案（`CHANGELOG.md`）。

### 移除

- 不再把 `target/` 的建置產物放進 git。原本有 19 個檔案被追蹤，其中包含過時的 JAR。

### 升級注意事項（伺服器管理員）

1. 先確認伺服器是 **Paper 26.2** 並使用 **Java 25**。
2. 如果有用 PlaceholderAPI，建議升到 **2.12.3 以上**。根據 PlaceholderAPI 的版本說明，該版修正了 Paper 26.2 新版本號格式的解析問題。
3. 如果有用 VeinMiner，請確認你的 VeinMiner 版本支援 Paper 26.2。
4. `plugins/AutoPickup/` 底下的設定檔與資料檔（`players.yml`、`filters.yml`）格式沒有變，可以直接沿用。
5. 如果你曾經在 `config.yml` 的 `messages:` 改過訊息（以前不會生效），升級後這些修改會開始生效，並優先於 `lang.yml`。

## [1.0.0]

首個版本：挖掘時掉落物自動進入背包、每位玩家獨立開關、白名單／黑名單過濾 GUI、ActionBar 拾取通知、PlaceholderAPI 佔位符與 VeinMiner 整合。適用於 Java 21、Paper 1.21.x。

### 修正

- 修正無法編譯的問題：`AutoPickupPlugin.legacyToMiniMessage` 原本是 package-private，但 `ActionBarManager` 從另一個 package 呼叫它。

### 新增

- 核心功能的 characterization tests（JUnit 5 + Mockito，共 73 個），記錄目前的行為。
- 測試用的 `TestRegistryAccess`，讓 `Material.isAir()` 不需要啟動伺服器也能在測試中使用。

[Unreleased]: https://github.com/MrPippi/AutoPickup/compare/v2.0.1...HEAD
[2.0.1]: https://github.com/MrPippi/AutoPickup/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/MrPippi/AutoPickup/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/MrPippi/AutoPickup/tree/6c3257c
