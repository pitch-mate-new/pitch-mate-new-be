# 기본 VPC 및 서브넷 조회
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

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

# S3 버킷 (영상 저장)
resource "aws_s3_bucket" "videos" {
  bucket = var.s3_bucket_name

  tags = {
    Name = "pitchmate-videos"
  }
}

resource "aws_s3_bucket_public_access_block" "videos" {
  bucket                  = aws_s3_bucket.videos.id
  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "videos_public_read" {
  bucket     = aws_s3_bucket.videos.id
  depends_on = [aws_s3_bucket_public_access_block.videos]

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicReadGetObject"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.videos.arn}/*"
    }]
  })
}

# EC2가 S3에 업로드할 IAM 역할
resource "aws_iam_role" "ec2_role" {
  name = "pitchmate-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "s3_policy" {
  name = "pitchmate-s3-policy"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"]
      Resource = "${aws_s3_bucket.videos.arn}/*"
    }]
  })
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "pitchmate-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# EC2 인스턴스
resource "aws_instance" "pitchmate" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.pitchmate.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

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

# RDS 보안 그룹 (EC2에서만 5432 접근 허용)
resource "aws_security_group" "rds" {
  name        = "pitchmate-rds-sg"
  description = "RDS security group - allow EC2 only"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.pitchmate.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "pitchmate-rds-sg"
  }
}

# RDS 서브넷 그룹 (최소 2개 AZ 필요)
resource "aws_db_subnet_group" "pitchmate" {
  name       = "pitchmate-db-subnet-group"
  subnet_ids = data.aws_subnets.default.ids

  tags = {
    Name = "pitchmate-db-subnet-group"
  }
}

# RDS PostgreSQL 인스턴스
resource "aws_db_instance" "pitchmate" {
  identifier        = "pitchmate-db"
  engine            = "postgres"
  engine_version    = "16"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.pitchmate.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  publicly_accessible = false
  skip_final_snapshot = true

  tags = {
    Name = "pitchmate-db"
  }
}
