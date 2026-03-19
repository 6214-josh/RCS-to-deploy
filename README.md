# RCS DOCX-aligned fix

## What was changed

### Backend
- WES -> RCS changed to REST/JSON aligned with the DOCX request fields.
- RCS -> AccuPick changed to TCP KV-CSV aligned with the DOCX CMD/ACK format.
- Added dashboard WebSocket `/ws/dashboard` for live UI updates.
- Added REST endpoints:
  - `POST /api/v1/commands`
  - `GET /api/v1/commands/{commandNo}`
  - `POST /api/v1/commands/{commandNo}/ack`
  - `POST /api/v1/mock/commands/{commandNo}/ack`
  - `GET /api/v1/dashboard/summary`
  - `GET /api/v1/dashboard/orders`
  - `GET /api/v1/dashboard/queue`
  - `GET /api/v1/dashboard/logs`
  - `GET /api/v1/dashboard/db-status`
- Added DOCX-aligned validation and abnormal code handling.
- Added mock AccuPick mode with `rcs.accupick.mock-enabled=true` for local demo.

### Frontend
- Reworked dashboard to match the uploaded dark industrial UI style.
- Table columns now follow the DOCX fields:
  - `Command_No`
  - `Order_No`
  - `Orderline_No`
  - `DC_ID`
  - `WorkStation_ID`
  - `Inbound_Carrier_ID`
  - `Product_ID`
  - `On_hand_qty`
  - `Picking_qty`
  - `Outbound_Carrier_ID`
  - `Command_Control_Code`
  - `Command_Status`
  - `Actual_qty`
  - `Abnormal_reason_code`
  - `Command_time`
- Added live command form, logs panel, detail modal, and mock ACK buttons.

## Important note
- In the current container, Maven was not available, so backend compile/run verification was not completed here.
- Frontend dependency installation was also not fully completed in the container.
- The source code has been updated and organized for you to continue locally.

## Suggested local run order
1. Start PostgreSQL using the provided SQL init.
2. Start backend on port `8080`.
3. Start frontend (Vite) and point it to `http://localhost:8080`.
4. Keep `rcs.accupick.mock-enabled=true` first for dashboard/demo testing.
