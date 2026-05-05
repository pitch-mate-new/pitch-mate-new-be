#!/bin/bash
# EC2 최초 1회 실행 - Java 설치 및 systemd 서비스 등록

set -e

echo "=== PitchMate EC2 초기화 ==="

# Java 21 설치
sudo apt-get update -y
sudo apt-get install -y openjdk-21-jdk

# 앱 디렉토리 생성
mkdir -p /home/ubuntu/app

# 환경변수 파일 생성 (실제 값은 GitHub Actions에서 덮어씀)
sudo touch /etc/pitchmate.env
sudo chmod 600 /etc/pitchmate.env

# systemd 서비스 등록
sudo tee /etc/systemd/system/pitchmate.service > /dev/null << 'EOF'
[Unit]
Description=PitchMate Spring Boot Server
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/app
EnvironmentFile=/etc/pitchmate.env
ExecStart=/usr/bin/java -jar /home/ubuntu/app/pitchmate-server.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable pitchmate

echo "=== 초기화 완료 ==="
echo "GitHub Actions로 첫 배포 후 서비스가 시작됩니다."
