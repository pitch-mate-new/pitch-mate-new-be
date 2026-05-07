output "public_ip" {
  description = "서버 공인 IP (Elastic IP)"
  value       = aws_eip.pitchmate.public_ip
}

output "ssh_command" {
  description = "SSH 접속 명령어"
  value       = "ssh -i ~/.ssh/${var.key_name}.pem ubuntu@${aws_eip.pitchmate.public_ip}"
}

output "rds_endpoint" {
  description = "RDS 엔드포인트"
  value       = aws_db_instance.pitchmate.endpoint
}

output "rds_jdbc_url" {
  description = "Spring Boot JDBC URL"
  value       = "jdbc:postgresql://${aws_db_instance.pitchmate.endpoint}/${var.db_name}"
}

output "s3_bucket_name" {
  description = "S3 버킷 이름"
  value       = aws_s3_bucket.videos.bucket
}

output "s3_bucket_url" {
  description = "S3 버킷 기본 URL"
  value       = "https://${aws_s3_bucket.videos.bucket}.s3.ap-northeast-2.amazonaws.com"
}
