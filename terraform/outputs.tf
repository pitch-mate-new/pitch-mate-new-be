output "public_ip" {
  description = "서버 공인 IP (Elastic IP)"
  value       = aws_eip.pitchmate.public_ip
}

output "ssh_command" {
  description = "SSH 접속 명령어"
  value       = "ssh -i ~/.ssh/${var.key_name}.pem ubuntu@${aws_eip.pitchmate.public_ip}"
}
