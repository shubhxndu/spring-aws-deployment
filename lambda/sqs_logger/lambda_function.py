import json
import logging
from urllib.parse import unquote_plus


logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)


def lambda_handler(event, context):
    """Log S3 object-created events delivered in an SQS batch."""
    processed_records = 0
    batch_item_failures = []

    for sqs_record in event.get("Records", []):
        message_id = sqs_record.get("messageId", "unknown")

        try:
            message = json.loads(sqs_record.get("body", "{}"))
            s3_records = message.get("Records", [])

            if message.get("Event") == "s3:TestEvent":
                logger.info("Received S3 test event: messageId=%s", message_id)

            for s3_record in s3_records:
                bucket = s3_record.get("s3", {}).get("bucket", {}).get(
                    "name", "unknown"
                )
                encoded_key = s3_record.get("s3", {}).get("object", {}).get(
                    "key", "unknown"
                )
                object_key = unquote_plus(encoded_key)
                event_name = s3_record.get("eventName", "unknown")

                logger.info(
                    "S3 event received: messageId=%s event=%s bucket=%s key=%s",
                    message_id,
                    event_name,
                    bucket,
                    object_key,
                )
                processed_records += 1
        except (json.JSONDecodeError, TypeError, AttributeError):
            logger.exception("Failed to process SQS message: messageId=%s", message_id)
            batch_item_failures.append({"itemIdentifier": message_id})

    return {
        "batchItemFailures": batch_item_failures,
        "processedRecords": processed_records,
    }
