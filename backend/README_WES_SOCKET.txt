RCS 已新增 WES Socket 收送功能

一、Port 規劃
1. RCS REST API: 8080
2. AccuPick TCP Server: 10000
3. RCS 接收 WES Socket: 10010
4. RCS 主動送到 WES: 10001

二、application.properties
rcs.wes.server.port=10010
rcs.wes.client.host=127.0.0.1
rcs.wes.client.port=10001
rcs.wes.client.timeout-ms=5000

三、WES -> RCS Socket 格式
WES|DB01|正常出貨|產品物流箱裝|35010466|阿奇儂鮮乳坊冰淇淋4入|5

四、RCS -> WES Socket 格式
RCS|DB01|AccuPick Finished|產品物流箱裝|35010466|阿奇儂鮮乳坊冰淇淋4入|5|DB01OK|5||||

五、Swagger / API 測試
1. 查 Socket 狀態
GET /api/wes/socket/status

2. 手動送 raw 訊息到 WES
POST /api/wes/socket/send-raw
{
  "message": "RCS|DB01|正常出貨|產品物流箱裝|35010466|阿奇儂鮮乳坊冰淇淋4入|5|DB01OK|5|||"
}

3. 用 jobNo 把某筆訂單結果送到 WES
POST /api/wes/socket/send/{jobNo}

六、程式啟動後
1. RCS 會同時啟動 AccuPick TCP Server
2. RCS 也會啟動 WES TCP Server
3. 當 AccuPick 回傳結果到 /api/accupick/result/{jobNo} 時，RCS 會自動再送 Socket 給 WES

七、注意
1. 目前這版 Socket 採 UTF-8 + 單行文字 + 換行結尾
2. 若 Solomon / WES 正式規格不是這種格式，需再改成固定長度或指定封包格式
3. 若 WES 要直接送到 RCS，請把 WES 端目標 Port 指到 10010
