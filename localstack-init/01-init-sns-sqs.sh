#!/bin/sh
set -eu

# ── productionApproval ↔ topicAprovacao ──────────────────────────────
QUEUE_URL=$(awslocal sqs create-queue --queue-name productionApproval --query 'QueueUrl' --output text)
QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$QUEUE_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)
TOPIC_ARN=$(awslocal sns create-topic --name topicAprovacao --query 'TopicArn' --output text)

awslocal sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$QUEUE_ARN" \
  --attributes RawMessageDelivery=true

echo "SNS topicAprovacao subscribed to SQS productionApproval"

# ── eventServiceOrderRecused ↔ topicServiceOrderRecused ──────────────
RECUSED_DLQ_URL=$(awslocal sqs create-queue --queue-name eventServiceOrderRecused-dlq --query 'QueueUrl' --output text)
RECUSED_DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$RECUSED_DLQ_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

RECUSED_QUEUE_URL=$(awslocal sqs create-queue --queue-name eventServiceOrderRecused --query 'QueueUrl' --output text)
RECUSED_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$RECUSED_QUEUE_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

awslocal sqs set-queue-attributes \
  --queue-url "$RECUSED_QUEUE_URL" \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$RECUSED_DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"}"

RECUSED_TOPIC_ARN=$(awslocal sns create-topic --name topicServiceOrderRecused --query 'TopicArn' --output text)

awslocal sns subscribe \
  --topic-arn "$RECUSED_TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$RECUSED_QUEUE_ARN" \
  --attributes RawMessageDelivery=true

echo "SNS topicServiceOrderRecused subscribed to SQS eventServiceOrderRecused"
