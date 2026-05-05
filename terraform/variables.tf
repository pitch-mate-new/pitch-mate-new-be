variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2" # 서울
}

variable "instance_type" {
  description = "EC2 인스턴스 타입"
  type        = string
  default     = "t3.micro"
}

variable "key_name" {
  description = "SSH 접속용 키페어 이름 (AWS에 등록된 키페어)"
  type        = string
}

variable "my_ip" {
  description = "SSH 허용할 내 IP (예: 123.456.789.0/32)"
  type        = string
}
