# Ubuntu 22.04 LTS (서울 리전)
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
}

# Security Group
resource "aws_security_group" "pitchmate" {
  name        = "pitchmate-sg"
  description = "PitchMate server security group"

  # SSH
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.my_ip]
  }

  # HTTP
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Spring Boot
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "pitchmate-sg"
  }
}

# EC2 인스턴스
resource "aws_instance" "pitchmate" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.pitchmate.id]

  # 루트 볼륨 30GB
  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  # Java 21 + 방화벽 기본 설정
  user_data = <<-EOF
    #!/bin/bash
    apt-get update -y
    apt-get install -y openjdk-21-jdk
    echo "JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64" >> /etc/environment
  EOF

  tags = {
    Name = "pitchmate-server"
  }
}

# Elastic IP (서버 재시작해도 IP 고정)
resource "aws_eip" "pitchmate" {
  instance = aws_instance.pitchmate.id
  domain   = "vpc"

  tags = {
    Name = "pitchmate-eip"
  }
}
