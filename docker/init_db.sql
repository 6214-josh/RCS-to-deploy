CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'ADMIN'
);

INSERT INTO users (username, password, role)
VALUES ('admin', 'admin', 'ADMIN')
ON CONFLICT (username) DO NOTHING;

CREATE TABLE IF NOT EXISTS picking_orders (
    id SERIAL PRIMARY KEY,
    command_no VARCHAR(48) UNIQUE,
    order_no VARCHAR(32),
    orderline_no VARCHAR(8),
    dc_id VARCHAR(8),
    workstation_id VARCHAR(16),
    inbound_carrier_id VARCHAR(32),
    product_id VARCHAR(32),
    on_hand_qty INTEGER,
    picking_qty INTEGER,
    outbound_carrier_id VARCHAR(32),
    command_control_code VARCHAR(16),
    command_status VARCHAR(16),
    actual_qty INTEGER,
    abnormal_reason_code INTEGER,
    command_time BIGINT,
    error_detail VARCHAR(64),
    callback_acknowledged BOOLEAN DEFAULT FALSE,
    job_no VARCHAR(100),
    job_status VARCHAR(100),
    ng_code VARCHAR(50),
    comment TEXT,
    image_url TEXT,
    line_id VARCHAR(50),
    robot_id VARCHAR(50),
    source_payload TEXT,
    ack_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = NOW();
   RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_picking_orders_updated_at ON picking_orders;
CREATE TRIGGER update_picking_orders_updated_at
BEFORE UPDATE ON picking_orders
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

INSERT INTO picking_orders (
  command_no, order_no, orderline_no, dc_id, workstation_id, inbound_carrier_id, product_id,
  on_hand_qty, picking_qty, outbound_carrier_id, command_control_code, command_status, actual_qty,
  abnormal_reason_code, command_time, error_detail, callback_acknowledged, job_no, job_status, ng_code, comment
)
VALUES
('DB01-0042', 'ORD-001', 'LN01', 'N1', 'N101', 'IN-B001', '35010466', 12, 5, 'OUT-B003', 'NORMAL', 'OK', 5, 0, 4230, NULL, TRUE, 'JOB-1001', 'AccuPick ACK received', '0', 'AccuPick completed successfully'),
('DB02-0043', 'ORD-002', 'LN01', 'N1', 'N101', 'IN-B002', '35010468', 8, 3, 'OUT-B004', 'DBOX', 'Processing', NULL, 0, NULL, NULL, FALSE, 'JOB-1002', 'Sent to AccuPick', NULL, 'Waiting for ACK'),
('DB03-0050', 'ORD-003', 'LN01', 'N1', 'N101', 'IN-B003', '35010470', 3, 5, 'OUT-B003', 'NORMAL', 'NG', 3, 1, 5120, 'SHORTAGE', TRUE, 'JOB-1003', 'AccuPick ACK received', '1', 'SHORTAGE'),
('CTRL-0001', 'CTRL', 'CTRL', 'N1', 'N101', 'CTRL-IN', 'CTRL', 0, 0, 'CTRL-OUT', 'DBXX', 'OK', NULL, 0, 850, NULL, TRUE, 'JOB-1004', 'AccuPick ACK received', '0', 'AccuPick completed successfully')
ON CONFLICT (command_no) DO NOTHING;

CREATE TABLE IF NOT EXISTS communication_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    direction VARCHAR(10) NOT NULL,
    protocol VARCHAR(20) NOT NULL,
    message_type VARCHAR(50),
    content TEXT,
    job_no VARCHAR(255),
    carrier_id VARCHAR(255),
    remote_address VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_communication_log_timestamp ON communication_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_communication_log_job_no ON communication_log(job_no);
