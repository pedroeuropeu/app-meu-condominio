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
| Portaria (acento de função, não é marca) | âmbar `#E59008` |

Títulos em **Lexend**, texto em Inter. Arquivos em `prototype/`: `logo-condovitta.png` (com slogan),
`logo-condovitta-compacta.png` (sem slogan), as versões `-escuro` (texto branco, para fundo escuro) e o emblema
em vetor no `favicon.svg` e no `markSvg()` do `app.js` (base para o ícone adaptativo do Android).

O protótipo aprovado fica em [`prototype/`](prototype/) (HTML navegável, sem dependências). Abra
`prototype/index.html` ou rode o preview `prototipo` do [`.claude/launch.json`](.claude/launch.json)
(`python3 -m http.server 5179 --directory prototype`).

> Feito **por partes**, por módulos. Nem todas as features da web entram de uma vez — começamos pelas principais.

## Dois modos

O app atende dois papéis bem diferentes do condomínio, e o protótipo deixa escolher qual ver na tela de entrada:

### 🛡️ Modo Portaria (rápido)
Pensado para o porteiro ganhar **agilidade** no balcão. Tudo em acento âmbar para separar da visão do morador.

| Tela | O que faz |
|---|---|
| **Painel do turno** | Saudação do porteiro, busca grande (morador, unidade **ou placa**), números do turno (esperados, no condomínio, encomendas) e **ações de 1 toque** |
| **Registrar encomenda** | O fluxo-estrela: unidade → transportadora (chips) → foto → onde guardar → gera o **código de retirada** e avisa o morador (app + push) |
| **Visitantes e acesso** | Esperados, quem está no condomínio e histórico do dia; **liberar entrada** / **registrar saída** em um toque; lista de bloqueados |
| **Nova visita** | Cadastro rápido de quem chegou sem pré-autorização, libera e avisa o morador |
| **Encomendas** | Aguardando retirada e retiradas de hoje; entrega com código + assinatura |
| **Reservas de hoje** | Lista de convidados na portaria, check-in e controle de chave |
| **Mais** | Livro da portaria, leitura de gás, veículos e placas, trocar de perfil |

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

As telas ainda não detalhadas aparecem com um estado **"Em construção"** honesto — a ideia é ir fechando módulo a módulo.

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

## Próximos passos (sugestão)
Transformar o protótipo em app Android nativo (Kotlin + Jetpack Compose + Material 3), como no e-Finances, ligando
nas APIs do backend `gestao-condominio`. Plano detalhado em [`docs/PLANO-ANDROID.md`](docs/PLANO-ANDROID.md). Seguir fechando módulos: assinatura de documentos, assembleias/plenário,
chamados, gás e prestação de contas.
