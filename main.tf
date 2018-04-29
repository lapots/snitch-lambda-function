terraform {
  backend "s3" {
    bucket = "s3-terraform-state-backend"
    region = "eu-central-1"
    key = "snitch-lambda-function/terraform.tfstate"
  }
}

variable "travisci_token" {}
variable "judge_url" {
  default = "http://judge.eu-central-1.elasticbeanstalk.com/judge/rest/player"
}

provider "travisci" {
  github_owner = "lapots"
  travisci_token = "${var.travisci_token}"
}

resource "travisci_repository" "travis_resource" {
  slug = "lapots/snitch-lambda-function"
}

provider "aws" {
  region = "eu-central-1"
}

resource "aws_s3_bucket" "game_rules" {
  bucket = "game_rules"
  acl = "private"

  tags {
    Name = "Game rules in XML or JSON"
    Environment = "Dev"
  }
}

resource "aws_iam_role" "iam_for_lambda" {
  name = "iam_for_lambda"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_lambda_permission" "allow_bucket" {
  statement_id = "AllowExecutionFromS3Bucket"
  action = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.snitch.arn}"
  principal = "s3.amazonaws.com"
  source_arn = "${aws_s3_bucket.game_rules.arn}"
}

# TODO: investigate
resource "aws_lambda_function" "snitch" {
  filename = "build/libs/snitch-lambda-function-1.0.jar"
  function_name = "snitch_rule_function"
  role = "${aws_iam_role.iam_for_lambda.arn}"
  handler = "com.lapots.breed.snitch.SnitchRuleCreationHandler"
  runtime = "java8"

  environment {
    variables {
      "aws.judge.url" = "${var.judge_url}"
    }
  }
}

resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = "${aws_s3_bucket.game_rules.id}"
  lambda_function {
    lambda_function_arn = "${aws_lambda_function.snitch.arn}"
    events = ["s3:ObjectCreated:*"]
    filter_prefix = "AWSLogs/"
    filter_suffix = ".log"
  }
}
