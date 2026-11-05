# Plano · App Android nativo do Condovitta

Mesmo padrão do `../e-finances-app-android` (estrutura, bibliotecas, flavors, segurança, testes e CI), ligado à API do
`../gestao-condominio`. Visual e comportamento = `prototype/` deste repositório. Marca: **Condovitta** (o repositório segue `app-meu-condominio`).

## 1. Stack (copiada do e-Finances)
Kotlin 2.2 · Jetpack Compose (BOM 2025.07) · Material 3 · Hilt · Navigation Compose com rotas tipadas · Retrofit 2.11 +
kotlinx.serialization · OkHttp 4.12 · Coil · DataStore · Biometric · Timber · JUnit/MockK/Turbine/Truth/MockWebServer.
`gradle/libs.versions.toml` igual ao do e-Finances. compileSdk/targetSdk 36, minSdk 26, JDK 17.
Ícone do app: ícone adaptativo a partir do emblema em vetor (`prototype/favicon.svg`), fundo branco.
Extras deste app: **CameraX** (foto da encomenda/livro), **ML Kit Barcode** (ler QR do visitante — fase 3),
**Firebase Messaging** (push — fase 4).

## 2. Arquitetura
```text
app/src/main/java/br/com/condovitta/app/
├── domain/        modelos (Visitante, Acesso, Encomenda, Reserva, Boleto, Comunicado…), regras e contratos dos repositórios
├── data/
│   ├── remote/    Retrofit + DTOs fiéis ao backend, Bearer, X-Condominio-Id, renovação de token, ApiError → AppError
│   ├── repository/ implementações da API (prod e local)
│   ├── demo/      dados do Residencial Vila do Sol 2 (os mesmos do prototype/data.js)
│   └── local/     DataStore (condomínio e perfil em uso, tema)
├── core/
│   ├── designsystem/ tema Condovitta (marinho #0A2C55, azul #0A62B8, verde-água #01B6A3, Lexend; acento âmbar na Portaria), componentes do protótipo
│   │                (kpi, badge, chip, placa, feature card, snackbar com Desfazer, estado "Em construção")
│   ├── security/  sessão cifrada (Keystore AES-256-GCM), bloqueio por biometria
│   └── common/    datas e dinheiro pt-BR, relógio injetável
├── feature/
│   ├── auth/      login (com TOTP), esqueci a senha, escolha de condomínio/perfil
│   ├── portaria/  painel, visitantes, nova visita, encomendas, reservas de hoje, mais (livro, gás, veículos)
│   └── morador/   início, reservas, boletos, visitantes, encomendas, avisos, mais
├── ui/            navegação: dois grafos (Portaria e Morador), cada um com sua barra inferior e uma pilha por aba
└── di/            Hilt (src/demo e src/prod ligam a implementação de cada flavor; local usa a do prod)
```
MVVM + fluxo unidirecional (`StateFlow` por tela), `Outcome<T>`/`AppError` como no e-Finances.

### Dois modos = um app, dois grafos
Depois do login, `GET /api/v1/me` traz as `memberships` (condomínio + papel). Regra:
- só MORADOR (ou SINDICO/CONSELHO com unidade) → grafo **Morador**;
- só PORTARIA → grafo **Portaria**;
- mais de um → tela "Entrar como" (como a tela de entrada do protótipo), lembrada no DataStore e trocável em Mais.
O condomínio escolhido vai em todo `/api/**` pelo cabeçalho `X-Condominio-Id`.

## 3. Flavors
| Flavor | Pacote | Dados |
|---|---|---|
| `demo` | `br.com.condovitta.app.demo` | exemplo no aparelho (qualquer senha; e-mail com "portaria" entra no modo Portaria) |
| `local` | `br.com.condovitta.app.local` | `make up` do gestao-condominio: API `localhost:8090`, Keycloak `localhost:8180` via `adb reverse` |
| `prod` | `br.com.condovitta.app` | domínio de produção (a definir) |
`scripts/local-dev.sh` igual ao do e-Finances (portas 8090 e 8180). Usuários de dev: `morador@condominio.local`,
`portaria@condominio.local`, `sindico@condominio.local`.

Emulador: reaproveitar os AVDs já instalados no Mac (ex.: `efinances_pixel8`, Pixel 8 / Android 16) —
`~/Library/Android/sdk/emulator/emulator -avd <nome>` e `adb install -r app/build/outputs/apk/demo/debug/app-demo-debug.apk`.

## 4. API (já existe no backend)
- **Auth:** `POST /public/auth/login` (`email`, `password`, `remember`, `totp`), `/public/auth/logout`,
  `/forgot-password`; renovação direto no Keycloak (realm `condominio`, client `condominio-client`, `grant_type=refresh_token`).
- **Contexto:** `GET /api/v1/me`.
- **Portaria:** `/api/v1/portaria/painel`, `/busca`, `/unidades`, `/acesso`, `/visitantes` (+ `/{id}/entrada`,
  `/{id}/consultar`), `/acessos/{id}/saida|recusar|desfazer`, `/bloqueados`, `/portaria/encomendas` (multipart com foto,
  `/{id}/avisar`, `/{id}/entregar`, `/{id}/desfazer-entrega`, `/destinatarios/{unidadeId}`), `/reservas/portaria`,
  `/livro`, `/rondas`, `/chaves`, `/passagem`, `/portaria/gas`, `/portaria/veiculos` (+ `/placa/{placa}`).
- **Morador:** `/api/v1/financeiro/morador` (+ `/boletos/{id}/pdf`, `/boletos/{id}/pix.png`), `/api/v1/visitantes`
  (`/minhas`, criar, `/{id}/revogar`, `/{id}/qr`, `/consultas/{acessoId}/responder`), `/api/v1/encomendas/minhas`,
  `/api/v1/reservas` (`/minhas`, criar multipart com termo assinado, `/{id}/cancelar`, `/{id}/convidados`),
  `/api/v1/areas-comuns` (+ `/{id}/disponibilidade`, `/{id}/mapa/imagem`), `/api/v1/comunicados/mural` (+ `/lido`,
  `/ciente`), `/api/v1/minha-unidade`, `/api/v1/notificacoes` (+ `/preferencias`), `/api/v1/presenca`.
- **Erros:** `ApiError {code, message, requestId}` → `AppError` (mensagem do servidor já vem em PT).

### O que falta no backend para o app
1. **Push de verdade (FCM):** a preferência `push` existe em `TB_NOTIFICACAO_PREFERENCIA`, mas não há entrega.
   Precisa de `POST/DELETE /api/v1/dispositivos` (token FCM por usuário) e o `NotificacaoService` enviar pelo
   Firebase Admin SDK. Sem isso, "avisar o morador" chega só no sino/e-mail.
2. **Keycloak:** confirmar que `condominio-client` aceita o refresh vindo do app (client público, sem redirect) e
   o emissor igual entre web e app no `local`.
3. **User-Agent do app** (`Condovitta/<versão> (Android …)`) reconhecido na auditoria/“online agora”.

## 5. Segurança (igual ao e-Finances)
Tokens cifrados no Keystore; renovação 30 s antes de vencer e em 401 (uma por vez); `SESSION_ENDED` → login;
biometria/PIN na abertura e após X min em segundo plano (desligável na Portaria, que usa aparelho compartilhado);
`FLAG_SECURE` no release; `filterTouchesWhenObscured`; só HTTPS (exceto flavor local); sem backup na nuvem; logs só
no debug com `Authorization` mascarado; R8 no release.

## 6. Fases
| Fase | Entrega | Pronto quando |
|---|---|---|
| **0 · Fundação** ✅ | Projeto Gradle, flavors, design system da marca, rede/sessão/biometria, login, `/me`, escolha de perfil, navegação dos dois modos com telas "Em construção", CI (GitHub Actions) | `assembleDemoDebug` abre e loga nos dois modos; `local` loga no Keycloak local |
| **1 · Portaria** ✅ | Painel do turno, busca (morador/unidade/placa), visitantes e acesso (liberar/saída/desfazer), nova visita, bloqueados, encomendas (registrar com CameraX → código de retirada, entregar, desfazer) | Porteiro faz o turno inteiro de visitas e encomendas pelo app, e a web reflete |
| **2 · Morador** | Início, boletos (Pix copia-e-cola, QR, PDF), visitantes (pré-autorizar, QR para compartilhar, responder consulta da portaria), encomendas, avisos com ciência | Morador vê e paga o boleto e autoriza visitas sem abrir a web |
| **3 · Reservas** | Catálogo, assistente em 4 passos (mapa, calendário de disponibilidade, turno/convidados, termo com assinatura em Canvas), minhas reservas; Portaria: reservas de hoje e chaves; leitor de QR do visitante | Reserva criada no app aparece aprovada/pendente na web |
| **4 · Push e mais** | FCM (depende do backend), notificações, livro da portaria, gás, veículos, minha unidade | Encomenda registrada vibra no celular do morador |
| **5 · Loja** | Ícone, capturas, `store/play-console.md`, teste interno, AAB assinado | Teste interno publicado |
Depois: chamados, documentos/assinatura, assembleias/plenário, prestação de contas, ocorrências (módulo a módulo).

## 7. Decisões
- `applicationId` → `br.com.condovitta.app`. Produção: `https://meucondominio.efinances.top/` (API + Keycloak em `/auth`),
  o mesmo domínio do deploy do gestao-condominio; trocável com `-Pcondovitta.prodUrl=...`.
- Portaria em aparelho compartilhado: **sessão longa, sem biometria e "Passar o turno"** em Mais (decidido em 2026-11-05).
- Push (FCM): **fase 4**, como no plano (decidido em 2026-11-05).
- Quem entra na Portaria: os papéis de `PapelEnum.OPERACAO` do backend (PORTARIA, SÍNDICO, ADMINISTRAÇÃO).
- Idiomas: só pt-BR por enquanto.

### Em aberto
- Fontes da marca (Lexend nos títulos, Inter no texto): por ora a sans-serif do sistema; falta colocar os `.ttf` em
  `app/src/main/res/font` (licença OFL, Google Fonts) e apontar `DisplayFamily`/`BodyFamily` em `Theme.kt`.
