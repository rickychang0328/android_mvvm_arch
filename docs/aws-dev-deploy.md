# AWS dev 部署指南（CloudFormation）

本文件對應 `dev`、Region `ap-southeast-2` 的通知後端佈建流程。  
目標是把以下 API 與資料模型部署到 AWS：

- `POST /api/v1/push/register-token`
- `POST /api/v1/internal/notifications/send`
- `POST /api/v1/notifications/{id}/open`

參考規格：

- `docs/api-spec.md`
- `docs/notification-schema.md`
- `docs/notification-flow.md`

---

## 1. IaC 檔案結構

```text
infra/dev/network.yaml
infra/dev/data.yaml
infra/dev/messaging.yaml
infra/dev/compute-api.yaml
infra/dev/compute-worker.yaml
infra/dev/compute-realtime.yaml
infra/dev/monitoring.yaml
infra/dev/root-stack.yaml
infra/scripts/deploy-dev.sh
```

Nested stack 依賴順序：

1. network
2. data
3. messaging
4. compute-api
5. compute-realtime（WebSocket API + 連線表，需先於 worker 建立以提供連線表與 management endpoint）
6. compute-worker（依賴 compute-realtime 輸出）
7. monitoring
8. root（聚合與輸出）

---

## 2. 先決條件

- AWS CLI v2（已設定有效 credentials）
- 目標 Region：`ap-southeast-2`
- 兩個 S3 bucket：
  - Template bucket（存放 CloudFormation YAML）
  - Artifact bucket（存放 Lambda zip）
- Lambda artifact 檔案：
  - `infra/artifacts/api-handler.zip`
  - `infra/artifacts/push-worker.zip`
  - `infra/artifacts/realtime-handler.zip`

必要 IAM 權限（部署者）：

- CloudFormation：`CreateChangeSet` / `ExecuteChangeSet` / `Describe*`
- S3：對 template/artifact bucket 的 `PutObject` / `GetObject`
- IAM：建立或更新 Lambda execution roles（`CAPABILITY_NAMED_IAM`）
- Lambda、API Gateway（HTTP + WebSocket）、SQS、DynamoDB、RDS、Secrets Manager、CloudWatch（建立/更新資源）

---

## 3. 一鍵部署（dev）

```bash
export TEMPLATE_BUCKET="your-cfn-template-bucket"
export ARTIFACT_BUCKET="your-lambda-artifact-bucket"
export JWT_ISSUER="https://issuer.example.com"
export JWT_AUDIENCE="android-mvvm-arch-app"

bash infra/scripts/deploy-dev.sh
```

常用可覆寫參數：

- `REGION`（預設 `ap-southeast-2`）
- `STACK_NAME`（預設 `android-mvvm-arch-dev-notifications`）
- `DB_INSTANCE_CLASS`（預設 `db.t4g.micro`）
- `DB_ALLOCATED_STORAGE`（預設 `20`）
- `ALARM_SNS_TOPIC_ARN`（可空白）
- `AUTO_EXECUTE_CHANGESET`（`true/false`）

---

## 4. Change Set 與預先驗證

`deploy-dev.sh` 會：

1. 上傳 nested templates 與 Lambda artifacts 到 S3
2. 建立 CloudFormation Change Set
3. 執行 `describe-events` 顯示 pre-deploy validation 事件
4. 視設定自動執行 Change Set
5. 等待 stack 完成並輸出 API endpoint

如果沒有變更，腳本會偵測 `didn't contain changes` 並安全退出。

---

## 5. Smoke Test（部署後）

以下測試建議使用 Postman 或 curl（實際 token 需替換）：

1. `POST /api/v1/push/register-token`
2. `POST /api/v1/internal/notifications/send`
3. `POST /api/v1/notifications/{id}/open`

WebSocket 連線測試（使用 `wscat`，token 需替換）：

```bash
# WebSocketApiEndpoint 來自 root stack output
wscat -H "Authorization: Bearer <JWT>" \
  -c "wss://<api-id>.execute-api.ap-southeast-2.amazonaws.com/dev"
```

連線成功後，對該使用者觸發 `internal/notifications/send`，應在連線中即時收到推送訊息（而非走 FCM）。

同時檢查：

- CloudWatch Logs：API handler / push worker / realtime handler
- SQS：
  - main queue backlog 是否可被消化
  - DLQ 是否出現堆積
- DynamoDB：`ws-connections` 在 `$connect` 後出現項目、`$disconnect` 後移除
- RDS：`push_registrations`、`notification_events`、`notification_recipients`、`user_notifications` 寫入正常

---

## 6. 回滾與故障處理

- 優先手段：`CloudFormation rollback`（以 change set 為單位回滾）
- Worker 異常時：
  - 先停用 `EventSourceMapping`
  - 檢查 DLQ 訊息與 `failure_reason`
- Lambda 程式回退：
  - 重新上傳既有穩定版 zip，建立新 change set 套用

---

## 7. 注意事項

- dev 預設為單區 RDS（Single-AZ），不可直接視為高可用生產配置。
- `internal/notifications/send` 必須走服務間授權，避免一般使用者 JWT 誤用。
- `fcm_token` 屬敏感資料，禁止寫入明文日誌。
