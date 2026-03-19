本次前端修改重點
=================

1. RCS Dashboard 頁面
- 新增 WES / AccuPick / DB 對應摘要
- 顯示 RCS 訂單、WES 場景、AccuPick 指令模板、Socket logs

2. WES 頁面
- 顯示 WES 場景資料表(DB01 / DB02 / DBXX)
- 顯示 RCS 回覆模板
- 可設定 WES endpoint 與 websocket URL
- 可手動送出 WES -> RCS payload
- 可手動送出 RCS -> WES payload
- 可自動送出(秒數可調)
- 通訊紀錄保存在 browser localStorage

3. AccuPick 頁面
- 顯示 AccuPick 指令模板
- 可設定 AccuPick endpoint 與 websocket URL
- 可手動送出 Socket 給 AccuPick
- 可自動送出(秒數可調)
- 可連線/斷線 WebSocket
- 可顯示後端透過 websocket 回來的訊息
- 通訊紀錄保存在 browser localStorage

4. 本次主要修改檔案
- client/src/lib/integration.ts
- client/src/pages/RcsView.tsx
- client/src/pages/WesView.tsx
- client/src/pages/AccupickView.tsx

5. 前端目前預設呼叫的 API
- WES: POST /api/socket/wes/send
- AccuPick: POST /api/socket/accupick/send

前端送出的 body 格式：
{
  "target": "wes" 或 "accupick",
  "payload": "實際socket字串"
}

6. 注意
- 瀏覽器前端不能直接開 raw TCP socket。
- 所以「手動/自動傳送 socket」介面是設計成呼叫 RCS 後端 API，再由後端去轉送 TCP/Socket。
- 若你後端 API 路徑不同，請直接在畫面上改 endpoint，或修改 integration.ts 的預設值。
