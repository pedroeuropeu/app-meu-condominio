# Condovitta · Android

App Android do **Condovitta** — *Tecnologia para viver melhor em condomínio* — o mesmo produto da plataforma web de
gestão de condomínios ([`../gestao-condominio`](../gestao-condominio)), no mesmo modelo do app Android do e-Finances
([`../e-finances-app-android`](../e-finances-app-android)). Pacote: `br.com.condovitta.app`.

> O repositório continua `app-meu-condominio`; só o nome do produto mudou para **Condovitta**.

## Identidade visual
| Uso | Cor |
|---|---|
| Azul-marinho (texto "Condo", tinta, topo) | `#0A2C55` |
| Azul da marca (ação principal) | `#0A62B8` |
| Verde-água ("vitta", destaque) | `#01B6A3` |
| Arco do emblema (degradê) | `#1062C4 → #05A8AA → #1BE79A` |
| Portaria (função, não é marca) | cabeçalho escuro `#111827` e azul único `#2563EB` nos ícones e botões |

Títulos em **Lexend**, texto em Inter. Arquivos em `prototype/`: `logo-condovitta.png` (com slogan),
`logo-condovitta-compacta.png` (sem slogan), as versões `-escuro` (texto branco, para fundo escuro) e o emblema
em vetor no `favicon.svg` e no `markSvg()` do `app.js` (base para o ícone adaptativo do Android).

O protótipo aprovado fica em [`prototype/`](prototype/) (HTML navegável, sem dependências). Abra
`prototype/index.html` ou rode o preview `prototipo` do [`.claude/launch.json`](.claude/launch.json)
(`python3 -m http.server 5179 --directory prototype`).

> Feito **por partes**, por módulos. Nem todas as features da web entram de uma vez — começamos pelas principais.

## Dois modos

O app atende dois papéis bem diferentes do condomínio, e o protótipo deixa escolher qual ver na tela de entrada:

### 🛡️ Modo Portaria (simples)
Pensado para o porteiro ganhar **agilidade** no balcão: tela limpa, botões grandes e poucas opções. Cabeçalho escuro e
um azul só, separado do app do morador. Sem barra inferior: o Início é o ponto de partida e o menu do cabeçalho guarda o
resto. Não tem as opções do morador (tema, digital).

| Tela | O que faz |
|---|---|
| **Início** | Busca grande (morador, unidade, placa **ou QR**) e **6 botões**: registrar encomenda, entregar, nova visita, esperados, no condomínio e livro, com os contadores do turno |
| **Registrar encomenda** | 2 passos: a unidade no **teclado** (bloco + número, confere o morador) → foto e transportadora. Gera o **código de retirada** e avisa o morador |
| **Visitantes** | Esperados / Dentro / Saíram; **Liberar** e **Saída** na própria linha, com Desfazer. Ao liberar, a portaria pode **tirar a foto** de quem entrou |
| **Nova visita** | Cadastro rápido de quem chegou sem pré-autorização, libera e avisa o morador |
| **Encomendas** | Aguardando retirada e retiradas de hoje; entrega com código + assinatura |
| **Menu** | Reservas de hoje, veículos e placas, leitura de gás, bloqueados, notificações, trocar de perfil e passar o turno |

### 🏢 App do morador (completo)
A visão do morador, nas cores da marca (azul-marinho, azul e verde-água).

| Tela | O que faz |
|---|---|
| **Início** | Boleto do mês em destaque (Pix + código), atalhos, encomendas para retirar, próximas reservas e avisos |
| **Reservas** | Minhas reservas + catálogo de áreas e o **assistente em 4 passos**: área (mapa das churrasqueiras) → data (calendário verde/âmbar/vermelho) → turno e convidados → termo assinado → confirmação |
| **Boletos e pagamentos** | Boleto do mês, Pix copia-e-cola, composição (taxa, fundo, gás, reservas) e histórico de pagos |
| **Visitantes** | Pré-autorizar visitas e prestadores, QR para compartilhar, visitas de hoje na portaria |
| **Encomendas** | Aguardando retirada com código, autorizar terceiro, histórico |
| **Avisos** | Mural do condomínio com confirmação de leitura |
| **Mais** | Grade de serviços (documentos, chamados, gás, assembleias, prestação de contas, ocorrências, unidade, calendário) e perfil |

No app do morador todas as telas do menu Mais já funcionam; o estado **"Em construção"** fica só para o que ainda não existir.

## Padrões (herdados do e-Finances e da web)
- **Sem toast:** feedback por snackbar (com Desfazer quando faz sentido), erro no lugar do formulário.
- **Exclusão sem "tem certeza?"** → snackbar com Desfazer.
- **Material 3:** pilha de telas com animações, folhas inferiores, diálogos, navegação inferior por modo.
- **Tema** claro/escuro/sistema (seletor na lateral do protótipo).
- Dados de exemplo fictícios do *Residencial Vila do Sol 2* (Goiânia/GO). O "você" do app é a Ana Souza (B-204);
  a portaria é o Carlos Mendes.

## Estrutura do protótipo
```
prototype/
├── index.html   # palco: índice de telas, aparelho ao centro, notas da tela
├── styles.css   # marca Condovitta, moldura do aparelho, componentes Material 3, claro/escuro
├── data.js      # dados de exemplo do condomínio
└── app.js       # motor de navegação + todas as telas
```

## Branches
- `main` — base.
- `develop` — desenvolvimento.
- `homolog` — homologação.
- `production` — produção.

## App Android (Kotlin + Jetpack Compose)
O app nativo fica em [`app/`](app/), no mesmo padrão do e-Finances. Plano completo e fases em
[`docs/PLANO-ANDROID.md`](docs/PLANO-ANDROID.md).

| Fase | Entrega | Situação |
|---|---|---|
| 0 · Fundação | Projeto, flavors, marca, rede/sessão, login (com 2FA), `/me`, escolha de perfil, os dois modos com telas "Em construção", CI | ✅ |
| 1 · Portaria | Painel do turno com busca, visitantes e acesso (liberar/saída/recusar com Desfazer), nova visita, bloqueados, encomendas com foto e entrega com código + assinatura | ✅ |
| 2 · Morador | Início (boleto do mês, pergunta da portaria), boletos (Pix copia e cola, QR, PDF, composição), visitantes (pré-autorizar, recorrente, código para compartilhar, revogar), encomendas (código, quem pode retirar), avisos com ciência | ✅ |
| 3 · Reservas | Minhas reservas e catálogo de áreas; assistente em 4 passos (espaço, calendário de disponibilidade, horário e convidados, termo com assinatura desenhada ou digitada); cancelar com Desfazer; portaria com reservas de hoje, check-in de convidados e chave; leitor de QR do visitante | ✅ |
| 4 · Notificações e mais | Central de notificações com contador no sino, aviso na barra do Android por consulta periódica (30 s com o app aberto, 15 min em segundo plano) abrindo a tela certa; livro da portaria, veículos e placas, leitura de gás; minha unidade (pets e funcionários) | ✅ (FCM depois) |
| 5 · Serviços do morador | Chamados (conversa, fotos, avaliação), ocorrências (registro, defesa no prazo, decisão), documentos para assinar (desenho + código por e-mail, evidências), assembleias e plenário ao vivo (presença, voto, palavra, chat, procuração), prestação de contas, consumo de gás e calendário (lembrete e agenda) | ✅ |
| 6 · Loja | Ícone, capturas, AAB assinado, teste interno | adiada |

### Flavors
| Flavor | Pacote | Dados |
|---|---|---|
| `demo` | `br.com.condovitta.app.demo` | exemplo no aparelho, qualquer senha. E-mail com `portaria` → porteiro; com `sindico` → escolhe o modo; outro → moradora |
| `local` | `br.com.condovitta.app.local` | `make up` do gestao-condominio (API `8090`, Keycloak `8180`) via `adb reverse` — `scripts/local-dev.sh` |
| `prod` | `br.com.condovitta.app` | `https://meucondominio.efinances.top/` (troque com `-Pcondovitta.prodUrl=...`) |

```bash
./gradlew assembleDemoDebug          # APK de demonstração
./gradlew testDemoDebugUnitTest      # testes unitários
scripts/local-dev.sh                 # instala o flavor local ligado ao backend desta máquina
# contrato com o backend local (entra com os usuários de dev e chama cada leitura do app):
CONDOVITTA_LOCAL=1 ./gradlew :app:testProdDebugUnitTest --tests '*BackendLocalTest*'
```
JDK 17+ (o CI usa 21). Emulador: `~/Library/Android/sdk/emulator/emulator -avd efinances_pixel8`.

### Decisões
- **Portaria em aparelho compartilhado:** sem biometria, sessão longa e **"Passar o turno"** em Mais (sai e deixa o
  login vazio para o próximo porteiro).
- **Quem entra em qual modo:** Portaria = papéis que operam a portaria no backend (PORTARIA, SÍNDICO, ADMINISTRAÇÃO);
  Morador = MORADOR, ou SÍNDICO/CONSELHO com unidade. Mais de um perfil → "Como você quer entrar?", lembrado no aparelho.
- **Notificações sem Firebase por enquanto:** o app consulta `/api/v1/notificacoes` a cada 30 s com o app aberto e a cada
  15 min em segundo plano (WorkManager). O FCM entra depois, só para o aviso instantâneo com o app fechado.
