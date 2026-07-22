import json
import unittest

from lambda_function import lambda_handler


class LambdaFunctionTest(unittest.TestCase):
    def test_logs_s3_object_from_sqs_message(self):
        s3_event = {
            "Records": [
                {
                    "eventName": "ObjectCreated:Put",
                    "s3": {
                        "bucket": {"name": "learning-project-bucket"},
                        "object": {"key": "uploads/test+file.txt"},
                    },
                }
            ]
        }
        event = {
            "Records": [
                {
                    "messageId": "message-123",
                    "body": json.dumps(s3_event),
                }
            ]
        }

        with self.assertLogs("lambda_function", level="INFO") as logs:
            result = lambda_handler(event, None)

        self.assertEqual(1, result["processedRecords"])
        self.assertEqual([], result["batchItemFailures"])
        self.assertIn("learning-project-bucket", " ".join(logs.output))
        self.assertIn("uploads/test file.txt", " ".join(logs.output))


if __name__ == "__main__":
    unittest.main()
