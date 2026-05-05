#!/bin/bash
set -e

echo "======================================"
echo "  PitchMate AWS 환경 설정 스크립트"
echo "======================================"
echo ""

# --- 1. AWS CLI 설치 확인 ---
if ! command -v aws &> /dev/null; then
  echo "[1/5] AWS CLI 설치 중..."
  brew install awscli
else
  echo "[1/5] AWS CLI 이미 설치됨 ($(aws --version))"
fi

# --- 2. Terraform 설치 확인 ---
if ! command -v terraform &> /dev/null; then
  echo "[2/5] Terraform 설치 중..."
  brew install terraform
else
  echo "[2/5] Terraform 이미 설치됨 ($(terraform -version | head -1))"
fi

echo ""
echo "======================================"
echo "  AWS 자격증명 입력"
echo "  (AWS 콘솔 → IAM → 내 보안 자격증명)"
echo "======================================"
echo ""

# --- 3. AWS CLI 자격증명 설정 ---
read -p "AWS Access Key ID     : " AWS_ACCESS_KEY_ID
read -s -p "AWS Secret Access Key : " AWS_SECRET_ACCESS_KEY
echo ""

AWS_REGION="ap-northeast-2"

aws configure set aws_access_key_id "$AWS_ACCESS_KEY_ID"
aws configure set aws_secret_access_key "$AWS_SECRET_ACCESS_KEY"
aws configure set region "$AWS_REGION"
aws configure set output "json"

echo ""
echo "[3/5] AWS CLI 자격증명 저장 완료"

# 자격증명 확인
echo ""
echo "--- 연결 확인 중 ---"
if aws sts get-caller-identity > /dev/null 2>&1; then
  ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
  echo "AWS 계정 연결 성공! (Account ID: $ACCOUNT_ID)"
else
  echo "ERROR: AWS 자격증명이 올바르지 않습니다. Access Key를 다시 확인하세요."
  exit 1
fi

# --- 4. SSH 키페어 생성 ---
echo ""
echo "======================================"
echo "  SSH 키페어 설정"
echo "======================================"

KEY_NAME="pitchmate-key"
KEY_PATH="$HOME/.ssh/${KEY_NAME}.pem"

if [ -f "$KEY_PATH" ]; then
  echo "[4/5] 키페어 이미 존재: $KEY_PATH"
else
  echo "[4/5] AWS에 키페어 생성 중..."

  # 기존 동일 이름 키페어 있으면 삭제
  aws ec2 delete-key-pair --key-name "$KEY_NAME" 2>/dev/null || true

  # 키페어 생성 및 저장
  aws ec2 create-key-pair \
    --key-name "$KEY_NAME" \
    --query "KeyMaterial" \
    --output text > "$KEY_PATH"

  chmod 400 "$KEY_PATH"
  echo "키페어 생성 완료: $KEY_PATH"
fi

# --- 5. terraform.tfvars 자동 생성 ---
echo ""
echo "======================================"
echo "  terraform.tfvars 생성"
echo "======================================"

MY_IP=$(curl -s https://checkip.amazonaws.com)/32
echo "내 IP 자동 감지: $MY_IP"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

cat > "$SCRIPT_DIR/terraform.tfvars" << EOF
aws_region    = "ap-northeast-2"
instance_type = "t2.micro"
key_name      = "${KEY_NAME}"
my_ip         = "${MY_IP}"
EOF

echo "[5/5] terraform.tfvars 생성 완료"

# --- 완료 ---
echo ""
echo "======================================"
echo "  설정 완료! 다음 명령어로 EC2 생성:"
echo "======================================"
echo ""
echo "  cd terraform"
echo "  terraform init"
echo "  terraform plan"
echo "  terraform apply"
echo ""
echo "  생성 후 SSH 접속:"
echo "  ssh -i $KEY_PATH ubuntu@<출력된 IP>"
echo "======================================"
