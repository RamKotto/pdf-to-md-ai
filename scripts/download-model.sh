#!/bin/sh
set -e

MODEL_FILE="/models/qwen2.5-1.5b-instruct-q4_k_m.gguf"
MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"

if [ -f "$MODEL_FILE" ]; then
  echo "Модель уже скачана, пропускаем."
  exit 0
fi

echo "Устанавливаем curl..."
apk add --no-cache curl

echo "Скачиваем модель..."
curl -L -o "$MODEL_FILE" "$MODEL_URL"

echo "Готово."