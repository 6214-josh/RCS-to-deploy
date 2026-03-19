-- RCS System Database Initialization
-- Based on the requirements provided in Image 2

CREATE TABLE IF NOT EXISTS rcs_orders (
    id SERIAL PRIMARY KEY,
    oder_no VARCHAR(50),                -- 1. 訂單號碼 (僅存在雲之萃的RCS TABLE，WES不提供資料)
    orderline_no VARCHAR(50),           -- 2. 訂單行號 (僅存在雲之萃的RCS TABLE，WES不提供資料)
    dc_id VARCHAR(50) DEFAULT 'N1',     -- 3. 物流中心代號 (建議採用N1或按照全聯WMS代號)
    workstation_id VARCHAR(50) DEFAULT 'N101', -- 4. 機器人工作站編號 (建議採用N101或按照全聯WMS代號)
    inbound_carrier_id VARCHAR(100),    -- 5. 載具ID (產品物流箱編號/產品箱)
    product_id VARCHAR(100),            -- 6. 產品編碼
    on_hand_qty INTEGER,                -- 7. 載具原庫存量
    picking_qty INTEGER,                -- 8. 揀貨數量
    outbound_carrier_id VARCHAR(100),   -- 9. 載具ID (訂單物流箱編號/出貨箱)
    command_no VARCHAR(100),            -- 10. 命令號碼 (每個PCS揀貨拆分為一個命令給AccuPick)
    command_time TIMESTAMP,             -- 11. 命令執行完成時間
    command_status VARCHAR(50),         -- 12. 命令執行結果狀態
    abnomal_reason_code VARCHAR(10),    -- 13. 異常狀態碼 (1: CTN SHORTAGE, 2: PICKING FAIL)
    command_control_code VARCHAR(100),  -- 14. 被揀貨箱控制命令
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Comments for abnormal_reason_code:
-- 1. CTN SHORTAGE 原箱短缺 (夾取數量不足時回傳已夾取成功數量在 Picking_Qty)
-- 2. PICKING FAIL 夾取失敗超過三次 (放棄任務後半段，回傳已夾取成功數量在 Picking_Qty)
