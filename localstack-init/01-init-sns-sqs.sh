#!/bin/sh
set -eu

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
