# Healthy KyoFai

> Updated : 16/10/2026 · v1.2

記錄每日飲食同運動嘅手機 app。影相俾 AI 估卡路里同營養，用 MET 計運動消耗，記錄飲水同體重，一鍵匯出 Excel。

- **Android APK**：[最新版本下載](../../releases/latest)
- **網頁版**：__PAGES_URL__

## 功能

| 分頁 | 做到乜 |
|---|---|
| 主頁 | 一週日曆、當日仲剩幾多 kcal、蛋白質／碳水／脂肪進度、「案底」記錄 |
| 又食嘢？加啦 | 影相或者揀相 → AI 分析每樣食物嘅份量同營養；亦可以用文字描述、收藏或者手動輸入 |
| 做咗運動？ | 15 款常見運動，按分鐘同體重用 MET 計消耗；可以上載手錶或 Strava 截圖俾 AI 讀數 |
| 飲水 | 一撳加 250／500／750 ml，最近 7 日圖表 |
| 進度 | 上磅記錄、體重走勢、預計達標日期、最近 7 日攝取 |
| 匯出 | Excel：每日總結（淨攝取、剩餘係公式）、飲食記錄（連食物相）、運動記錄；個人資料預設唔匯出 |
| 個人 | 個人資料、每日計劃（BMR、TDEE、目標）、AI 設定、備份同還原 |

右上角嘅眼仔可以隨時收埋體重、年齡、身高等數字，適合喺課室或者投影時用。

## 第一次用

1. 喺「個人」填性別、年齡、身高、體重同目標體重。
2. 喺「個人 › AI 設定」貼上 Anthropic API key（喺 [platform.claude.com](https://platform.claude.com/) 申請）。唔設定都可以用，只係要手動輸入營養。
3. 如果之前喺 Claude 入面用過，喺嗰邊「個人 › 匯出備份」，再喺呢度「還原備份」。

## 私隱

- 所有記錄、相片同個人資料只會儲存喺你部手機（或者瀏覽器）入面，呢個 repo 冇任何個人資料。
- API key 只儲存喺本機，AI 分析時直接連去 Anthropic，唔經其他伺服器。
- AI 分析只會傳送食物相片同你寫嘅補充，唔會傳送個人資料。
- 間中喺「個人」匯出備份（.json），換機或者清除資料之後可以還原。

## 安裝 APK

1. 用 Android 手機開 [Releases](../../releases/latest)，下載 `HealthyKyoFai-v1.2.x.apk`。
2. 第一次安裝要允許「安裝不明來源嘅 app」。
3. 之後有新版本，直接安裝就會覆蓋舊版，記錄會保留。

## 點樣 build

- `www/index.html` 係成個 app（單一檔案，連 `www/vendor/exceljs.min.js`）。改完 push 去 `main`：
  - **Build Android APK** workflow 會將 `www/` 包入 WebView app，build 好簽名 APK 再發佈到 Releases。
  - **Deploy web version** workflow 會將 `www/` 發佈到 GitHub Pages。
- APK 用 repo secret `SIGNING_KEYSTORE_BASE64` 或者 `HEALTHYKYOFAI`（release keystore 嘅 base64）簽名。冇咗呢個 secret，每次 build 會用臨時 key，新 APK 就唔可以覆蓋安裝舊版。
- Android 部分：`android/`（Java、minSdk 26、targetSdk 34），只用 `androidx.webkit` 同 `androidx.core`。

## 計算方法

- BMR：Mifflin-St Jeor 公式；TDEE = BMR × 活動量系數。
- 每日目標 = TDEE − 減重速度 × 7,700 ÷ 7，最低男 1,500／女 1,200 kcal；運動消耗會加返落當日額度。
- 蛋白質每公斤體重 1.4 g，脂肪佔 27% 熱量，其餘係碳水。
- 運動消耗 = MET × 體重(kg) × 時間(小時)。
- 飲水目標：每公斤體重 35 ml（可以自訂）。

AI 估算只係參考，唔可以代替營養師或者醫生嘅意見。

第三方：ExcelJS（MIT License）。
