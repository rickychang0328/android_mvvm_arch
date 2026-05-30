#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

REGION="${REGION:-ap-southeast-2}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
PROJECT_NAME="${PROJECT_NAME:-android-mvvm-arch}"
STACK_NAME="${STACK_NAME:-${PROJECT_NAME}-${ENVIRONMENT}-notifications}"

TEMPLATE_BUCKET="${TEMPLATE_BUCKET:-}"
TEMPLATE_PREFIX="${TEMPLATE_PREFIX:-infra/dev}"

ARTIFACT_BUCKET="${ARTIFACT_BUCKET:-${TEMPLATE_BUCKET}}"
ARTIFACT_PREFIX="${ARTIFACT_PREFIX:-artifacts/${ENVIRONMENT}}"

API_ARTIFACT_PATH="${API_ARTIFACT_PATH:-${ROOT_DIR}/infra/artifacts/api-handler.zip}"
WORKER_ARTIFACT_PATH="${WORKER_ARTIFACT_PATH:-${ROOT_DIR}/infra/artifacts/push-worker.zip}"
REALTIME_ARTIFACT_PATH="${REALTIME_ARTIFACT_PATH:-${ROOT_DIR}/infra/artifacts/realtime-handler.zip}"

JWT_ISSUER="${JWT_ISSUER:-https://your-issuer.example.com}"
JWT_AUDIENCE="${JWT_AUDIENCE:-android-mvvm-arch-app}"
NOTIFICATION_SERVICE_SCOPE="${NOTIFICATION_SERVICE_SCOPE:-notifications:send}"

DB_NAME="${DB_NAME:-notifications}"
DB_USERNAME="${DB_USERNAME:-app_user}"
DB_INSTANCE_CLASS="${DB_INSTANCE_CLASS:-db.t4g.micro}"
DB_ALLOCATED_STORAGE="${DB_ALLOCATED_STORAGE:-20}"
ALARM_SNS_TOPIC_ARN="${ALARM_SNS_TOPIC_ARN:-}"

AUTO_EXECUTE_CHANGESET="${AUTO_EXECUTE_CHANGESET:-true}"

if [[ -z "${TEMPLATE_BUCKET}" ]]; then
  echo "ERROR: TEMPLATE_BUCKET is required."
  exit 1
fi

if [[ ! -f "${API_ARTIFACT_PATH}" ]]; then
  echo "ERROR: API artifact not found at ${API_ARTIFACT_PATH}"
  exit 1
fi

if [[ ! -f "${WORKER_ARTIFACT_PATH}" ]]; then
  echo "ERROR: Worker artifact not found at ${WORKER_ARTIFACT_PATH}"
  exit 1
fi

if [[ ! -f "${REALTIME_ARTIFACT_PATH}" ]]; then
  echo "ERROR: Realtime artifact not found at ${REALTIME_ARTIFACT_PATH}"
  exit 1
fi

upload_template() {
  local filename="$1"
  aws s3 cp \
    "${ROOT_DIR}/infra/dev/${filename}" \
    "s3://${TEMPLATE_BUCKET}/${TEMPLATE_PREFIX}/${filename}" \
    --region "${REGION}" >/dev/null
}

echo "Uploading nested templates to s3://${TEMPLATE_BUCKET}/${TEMPLATE_PREFIX}/ ..."
upload_template "network.yaml"
upload_template "data.yaml"
upload_template "messaging.yaml"
upload_template "compute-api.yaml"
upload_template "compute-worker.yaml"
upload_template "compute-realtime.yaml"
upload_template "monitoring.yaml"
upload_template "root-stack.yaml"

echo "Uploading lambda artifacts to s3://${ARTIFACT_BUCKET}/${ARTIFACT_PREFIX}/ ..."
aws s3 cp "${API_ARTIFACT_PATH}" "s3://${ARTIFACT_BUCKET}/${ARTIFACT_PREFIX}/api-handler.zip" --region "${REGION}" >/dev/null
aws s3 cp "${WORKER_ARTIFACT_PATH}" "s3://${ARTIFACT_BUCKET}/${ARTIFACT_PREFIX}/push-worker.zip" --region "${REGION}" >/dev/null
aws s3 cp "${REALTIME_ARTIFACT_PATH}" "s3://${ARTIFACT_BUCKET}/${ARTIFACT_PREFIX}/realtime-handler.zip" --region "${REGION}" >/dev/null

API_CODE_KEY="${ARTIFACT_PREFIX}/api-handler.zip"
WORKER_CODE_KEY="${ARTIFACT_PREFIX}/push-worker.zip"
REALTIME_CODE_KEY="${ARTIFACT_PREFIX}/realtime-handler.zip"

if aws cloudformation describe-stacks --stack-name "${STACK_NAME}" --region "${REGION}" >/dev/null 2>&1; then
  CHANGE_SET_TYPE="UPDATE"
  WAIT_COMMAND="stack-update-complete"
else
  CHANGE_SET_TYPE="CREATE"
  WAIT_COMMAND="stack-create-complete"
fi

CHANGE_SET_NAME="${STACK_NAME}-changeset-$(date +%Y%m%d%H%M%S)"
ROOT_TEMPLATE_URL="https://s3.${REGION}.amazonaws.com/${TEMPLATE_BUCKET}/${TEMPLATE_PREFIX}/root-stack.yaml"

echo "Creating change set ${CHANGE_SET_NAME} (${CHANGE_SET_TYPE}) ..."
aws cloudformation create-change-set \
  --stack-name "${STACK_NAME}" \
  --change-set-name "${CHANGE_SET_NAME}" \
  --change-set-type "${CHANGE_SET_TYPE}" \
  --template-url "${ROOT_TEMPLATE_URL}" \
  --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
  --parameters \
    ParameterKey=ProjectName,ParameterValue="${PROJECT_NAME}" \
    ParameterKey=Environment,ParameterValue="${ENVIRONMENT}" \
    ParameterKey=TemplateBucket,ParameterValue="${TEMPLATE_BUCKET}" \
    ParameterKey=TemplatePrefix,ParameterValue="${TEMPLATE_PREFIX}" \
    ParameterKey=ApiCodeS3Bucket,ParameterValue="${ARTIFACT_BUCKET}" \
    ParameterKey=ApiCodeS3Key,ParameterValue="${API_CODE_KEY}" \
    ParameterKey=WorkerCodeS3Bucket,ParameterValue="${ARTIFACT_BUCKET}" \
    ParameterKey=WorkerCodeS3Key,ParameterValue="${WORKER_CODE_KEY}" \
    ParameterKey=RealtimeCodeS3Bucket,ParameterValue="${ARTIFACT_BUCKET}" \
    ParameterKey=RealtimeCodeS3Key,ParameterValue="${REALTIME_CODE_KEY}" \
    ParameterKey=JwtIssuer,ParameterValue="${JWT_ISSUER}" \
    ParameterKey=JwtAudience,ParameterValue="${JWT_AUDIENCE}" \
    ParameterKey=NotificationServiceScope,ParameterValue="${NOTIFICATION_SERVICE_SCOPE}" \
    ParameterKey=DBName,ParameterValue="${DB_NAME}" \
    ParameterKey=DBUsername,ParameterValue="${DB_USERNAME}" \
    ParameterKey=DBInstanceClass,ParameterValue="${DB_INSTANCE_CLASS}" \
    ParameterKey=DBAllocatedStorage,ParameterValue="${DB_ALLOCATED_STORAGE}" \
    ParameterKey=AlarmSnsTopicArn,ParameterValue="${ALARM_SNS_TOPIC_ARN}" \
  --region "${REGION}" >/dev/null

set +e
aws cloudformation wait change-set-create-complete \
  --stack-name "${STACK_NAME}" \
  --change-set-name "${CHANGE_SET_NAME}" \
  --region "${REGION}"
WAIT_EXIT_CODE=$?
set -e

if [[ ${WAIT_EXIT_CODE} -ne 0 ]]; then
  STATUS_REASON="$(aws cloudformation describe-change-set \
    --stack-name "${STACK_NAME}" \
    --change-set-name "${CHANGE_SET_NAME}" \
    --query 'StatusReason' \
    --output text \
    --region "${REGION}")"
  if [[ "${STATUS_REASON}" == *"didn't contain changes"* ]]; then
    echo "No infrastructure changes detected. Exiting."
    exit 0
  fi
  echo "Change set creation failed: ${STATUS_REASON}"
  exit 1
fi

CHANGE_SET_ARN="$(aws cloudformation describe-change-set \
  --stack-name "${STACK_NAME}" \
  --change-set-name "${CHANGE_SET_NAME}" \
  --query 'ChangeSetId' \
  --output text \
  --region "${REGION}")"

echo "Pre-deploy validation events (from change set):"
aws cloudformation describe-events \
  --change-set-id "${CHANGE_SET_ARN}" \
  --region "${REGION}" \
  --max-items 20

if [[ "${AUTO_EXECUTE_CHANGESET}" != "true" ]]; then
  echo "Change set created but not executed (AUTO_EXECUTE_CHANGESET=false)."
  echo "Execute manually with:"
  echo "aws cloudformation execute-change-set --stack-name ${STACK_NAME} --change-set-name ${CHANGE_SET_NAME} --region ${REGION}"
  exit 0
fi

echo "Executing change set ${CHANGE_SET_NAME} ..."
aws cloudformation execute-change-set \
  --stack-name "${STACK_NAME}" \
  --change-set-name "${CHANGE_SET_NAME}" \
  --region "${REGION}" >/dev/null

echo "Waiting for ${WAIT_COMMAND} ..."
aws cloudformation wait "${WAIT_COMMAND}" --stack-name "${STACK_NAME}" --region "${REGION}"

API_ENDPOINT="$(aws cloudformation describe-stacks \
  --stack-name "${STACK_NAME}" \
  --region "${REGION}" \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' \
  --output text)"

echo "Deployment complete."
echo "Stack: ${STACK_NAME}"
echo "Region: ${REGION}"
echo "ApiEndpoint: ${API_ENDPOINT}"
