#!/usr/bin/env bash
# Sobe o projeto inteiro: MongoDB -> Servidor Socket -> Ponte HTTP -> Frontend
# Uso:  ./rodar.sh            (normal)
#       ./rodar.sh --popular  (popula o banco com dados de exemplo antes)
# Ctrl+C encerra tudo.
#
# Precisa de: Java 11+, Node 18+ e MongoDB (local ou via `docker compose up -d`).
# O Maven NAO precisa estar instalado: o script usa o ./mvnw do backend.

RAIZ="$(cd "$(dirname "$0")" && pwd)"
LOGS="$RAIZ/logs"
mkdir -p "$LOGS"
MVN="$RAIZ/backend/mvnw"
chmod +x "$MVN" 2>/dev/null

porta_aberta() {
  (echo > /dev/tcp/localhost/$1) >/dev/null 2>&1
}

esperar_porta() {  # $1 = porta, $2 = nome
  for _ in $(seq 1 90); do
    porta_aberta "$1" && { echo "  OK - $2 pronto (porta $1)"; return 0; }
    sleep 1
  done
  echo "  ERRO - $2 nao subiu na porta $1. Veja os logs em $LOGS"
  encerrar; exit 1
}

encerrar() {
  echo
  echo "Encerrando servidor e ponte..."
  [ -n "$PID_PONTE" ] && kill "$PID_PONTE" 2>/dev/null
  [ -n "$PID_SERV" ]  && kill "$PID_SERV"  2>/dev/null
  wait 2>/dev/null
}
trap 'encerrar; exit 0' INT TERM

# --- checagens ---------------------------------------------------------------
command -v java >/dev/null || { echo "Java nao encontrado. Instale o Java 11 ou superior."; exit 1; }
command -v npm  >/dev/null || { echo "Node/npm nao encontrado. Instale o Node 18 ou superior."; exit 1; }

echo "1/4 MongoDB"
if ! porta_aberta 27017; then
  if command -v docker >/dev/null; then
    (cd "$RAIZ" && docker compose up -d) >/dev/null 2>&1
  elif command -v brew >/dev/null; then
    brew services start mongodb-community >/dev/null 2>&1
  elif command -v systemctl >/dev/null; then
    sudo systemctl start mongod 2>/dev/null
  fi
fi
porta_aberta 27017 || sleep 3
porta_aberta 27017 || {
  echo "  ERRO - MongoDB nao esta rodando na porta 27017."
  echo "  Inicie o MongoDB (ou rode 'docker compose up -d') e tente de novo."
  exit 1
}
echo "  OK - MongoDB pronto (porta 27017)"

cd "$RAIZ/backend" || exit 1
echo "Compilando backend (na primeira vez baixa as dependencias, pode demorar)..."
"$MVN" -q compile || { echo "Erro na compilacao"; exit 1; }

if [ "$1" = "--popular" ]; then
  echo "Populando banco com dados de exemplo..."
  "$MVN" -q exec:java@popular
fi

echo "2/4 Servidor Socket"
"$MVN" -q exec:java@servidor > "$LOGS/servidor.log" 2>&1 &
PID_SERV=$!
esperar_porta 12345 "Servidor"

echo "3/4 Ponte HTTP"
"$MVN" -q exec:java@ponte > "$LOGS/ponte.log" 2>&1 &
PID_PONTE=$!
esperar_porta 8080 "Ponte"

echo "4/4 Frontend - abra http://localhost:5173  (Ctrl+C para parar tudo)"
cd "$RAIZ/frontend" || exit 1
[ -d node_modules ] || npm install
npm run dev
encerrar
