#!/usr/bin/env bash
# Liga o app (flavor "local") ao gestao-condominio rodando nesta máquina (`make up`): backend (8090) e Keycloak (8180).
# Usa `adb reverse`: o "localhost" do emulador ou do celular no cabo USB passa a ser o deste Mac, então login e
# renovação de token funcionam igual à web (mesmo emissor do token: KEYCLOAK_PUBLIC_URL=http://localhost:8180).
#
#   scripts/local-dev.sh            # liga as portas, instala e abre o app
#   scripts/local-dev.sh --ports    # só liga as portas (ex.: depois de reiniciar o emulador)
#
# Usuários de dev do gestao-condominio: morador@condominio.local, portaria@condominio.local, sindico@condominio.local
set -euo pipefail

cd "$(dirname "$0")/.."
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
PORTS=(8090 8180)
APP_ID="br.com.condovitta.app.local.debug"

check() {
  local name=$1 url=$2
  if curl -s -o /dev/null --max-time 3 "$url"; then echo "  ok   $name ($url)"; else echo "  FORA $name ($url)"; fi
}

echo "Serviços nesta máquina:"
check "backend " "http://localhost:8090/actuator/health"
check "keycloak" "http://localhost:8180/realms/condominio/.well-known/openid-configuration"

DEVICES=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')
if [ -z "$DEVICES" ]; then
  echo "Nenhum aparelho conectado. Abra o emulador (emulator -avd efinances_pixel8) ou ligue o celular com depuração USB."
  exit 1
fi

for d in $DEVICES; do
  for p in "${PORTS[@]}"; do "$ADB" -s "$d" reverse "tcp:$p" "tcp:$p" >/dev/null; done
  echo "Portas ${PORTS[*]} ligadas em $d"
done

if [ "${1:-}" = "--ports" ]; then exit 0; fi

./gradlew :app:installLocalDebug --console=plain -q
for d in $DEVICES; do "$ADB" -s "$d" shell am start -n "$APP_ID/br.com.condovitta.app.MainActivity" >/dev/null; done
echo "Pronto: abra o \"Condovitta Local\" e entre com um usuário de dev do gestao-condominio."
