/* Condovitta · dados de exemplo do protótipo (fictícios).
   Residencial Vila do Sol 2 — Goiânia/GO. O "você" do app completo é a Ana Souza (B-204);
   a portaria é operada pelo Carlos Mendes (turno 06h–14h). Datas âncora em novembro/2026. */
'use strict';
(() => {
    const H = '2026-11-04';           // hoje
    const ONTEM = '2026-11-03';
    const AMANHA = '2026-11-05';

    // unidades para selects (4 blocos, amostra por bloco)
    const resp = {
        'A-102': 'Roberto Lima', 'A-205': 'Felipe Araújo', 'A-302': 'Vinícius Ramos', 'A-403': 'Tatiane Gomes', 'A-501': 'Marcos Teixeira', 'A-604': 'Helena Castro',
        'B-102': 'Patrícia Nunes', 'B-204': 'Ana Souza', 'B-301': 'Rafael Rocha', 'B-402': 'Daniela Pinto', 'B-503': 'Caio Monteiro',
        'C-106': 'Juliana Dias', 'C-202': 'Otávio Freitas', 'C-303': 'Larissa Alves', 'C-305': 'Bruno Campos', 'C-404': 'Sabrina Cardoso', 'C-501': 'Thiago Barros',
        'D-104': 'Carla Mendonça', 'D-201': 'Rafael Torres', 'D-305': 'Mariana Lopes', 'D-402': 'Eduarda Silva', 'D-601': 'Nicolas Moura'
    };
    const unidades = Object.keys(resp).map(id => ({ id, bloco: id[0], responsavel: resp[id] }));
    const nome = id => resp[id] || 'Condomínio';

    const MC = {
        hoje: H, ontem: ONTEM, amanha: AMANHA,
        condo: { nome: 'Residencial Vila do Sol 2', cidade: 'Goiânia · GO', endereco: 'Rua das Acácias, 450 — Setor Bueno', unidades: 120, blocos: 4 },
        unidades, nome,

        // pessoa logada no app completo
        morador: { nome: 'Ana Souza', primeiro: 'Ana', unidade: 'B-204', bloco: 'B', iniciais: 'AS', tipo: 'Proprietária', cpf: '418.293.045-12', tel: '(62) 99812-4471', email: 'ana.souza@email.com' },

        // porteiro de plantão
        porteiro: { nome: 'Carlos Mendes', primeiro: 'Carlos', iniciais: 'CM', cargo: 'Porteiro', turno: '06h – 14h', escala: '12x36' },

        // ---------------- PORTARIA ----------------
        visitantes: [
            { id: 1, nome: 'Lucas Prado', tipo: 'visita', doc: 'RG 5.482.119 SSP/GO', tel: '(62) 99654-2210', unidade: 'C-303', via: 'app', inicio: '14:00', fim: '18:00', status: 'esperado', placa: 'RTA4B21', carro: 'Onix Prata' },
            { id: 2, nome: 'Juliana Martins', tipo: 'prestador', obs: 'Diarista', doc: 'CPF ***.551.208-**', tel: '(62) 99410-8832', unidade: 'B-204', via: 'recorrente', recorrente: 'seg, qua e sex', inicio: '10:00', fim: '17:00', status: 'esperado', placa: '', carro: '' },
            { id: 3, nome: 'Marcelo Dias', tipo: 'prestador', obs: 'Vivo Fibra · instalação', doc: 'CPF ***.903.447-**', tel: '(62) 3201-4400', unidade: 'D-402', via: 'app', inicio: '10:00', fim: '12:00', status: 'esperado', placa: 'QPF2E81', carro: 'Fiorino Branca' },
            { id: 7, nome: 'Rafael Torres', tipo: 'visita', doc: 'CPF ***.320.118-**', tel: '(62) 99877-1203', unidade: 'D-201', via: 'portaria', inicio: '09:38', fim: '12:00', status: 'aguardando', placa: '', carro: '' },
            { id: 4, nome: 'Marcelo Freitas', tipo: 'visita', obs: 'Convidado da festa no salão', doc: 'RG 3.118.702 SSP/DF', tel: '(61) 98122-7710', unidade: 'A-501', via: 'app', inicio: '18:00', fim: '23:59', status: 'esperado', placa: 'SBC7H44', carro: 'HB20 Preto' },
            { id: 8, nome: 'Rogério Campos', tipo: 'prestador', obs: 'Elevadores Atlas · preventiva Bloco A', doc: 'CPF ***.118.620-**', tel: '(62) 3241-0099', unidade: 'Condomínio', via: 'portaria', inicio: '08:00', fim: '12:00', status: 'dentro', entrada: '08:05', placa: 'RKO5D33', carro: 'Strada Branca' },
            { id: 9, nome: 'Patrícia Nunes', tipo: 'visita', doc: 'CPF ***.774.301-**', tel: '(62) 98450-6612', unidade: 'B-102', via: 'app', inicio: '07:30', fim: '09:30', status: 'dentro', entrada: '07:41', placa: 'PQA1B23', carro: 'Kwid Vermelho' },
            { id: 11, nome: 'Amanda Lopes', tipo: 'prestador', obs: 'Babá', doc: 'RG 6.210.554 SSP/GO', tel: '(62) 99812-3390', unidade: 'D-305', via: 'recorrente', recorrente: 'seg a sex', inicio: '07:00', fim: '19:00', status: 'dentro', entrada: '06:58', placa: '', carro: '' },
            { id: 13, nome: 'Thiago Barbosa', tipo: 'visita', doc: 'CPF ***.630.552-**', tel: '(62) 99333-1020', unidade: 'C-501', via: 'app', inicio: '06:30', fim: '08:30', status: 'saiu', entrada: '06:41', saida: '07:55', placa: '', carro: '' },
            { id: 15, nome: 'Vinícius Ramos', tipo: 'visita', doc: 'CPF ***.215.990-**', tel: '(62) 98111-2233', unidade: 'A-302', via: 'app', data: ONTEM, inicio: '15:00', fim: '22:00', status: 'saiu', entrada: '15:20', saida: '21:40', placa: 'SDE2F18', carro: 'Tracker Preto' }
        ],
        bloqueados: [
            { id: 1, nome: 'Gilberto Ramos', doc: 'CPF ***.218.554-**', motivo: 'Tentou entrar usando o nome de outro morador.', por: 'Roberto Lima (síndico)', desde: '14/09/2026' },
            { id: 2, nome: 'Kleber Santana', doc: 'vendedor ambulante', motivo: 'Venda de porta em porta, proibida pelo regimento.', por: 'Administração', desde: '02/06/2026' }
        ],
        veiculos: [
            { placa: 'SBA2C04', modelo: 'Compass Branco', unidade: 'B-204', vaga: 'B-17', acesso: 'TAG 004821', status: 'ativo' },
            { placa: 'RXL1A02', modelo: 'Corolla Cross Preto', unidade: 'A-102', vaga: 'A-02', acesso: 'Leitura de placa', status: 'ativo' },
            { placa: 'QPT6B52', modelo: 'Kicks Vermelho', unidade: 'B-204', vaga: 'B-18', acesso: 'TAG 004822', status: 'ativo' },
            { placa: 'RVX3C12', modelo: 'HB20 Prata', unidade: 'C-303', vaga: 'C-11', acesso: 'TAG 003310', status: 'bloqueado', obs: 'Condomínio em atraso há 3 meses' }
        ],

        encomendas: [
            { id: 1, unidade: 'B-204', dest: 'Ana Souza', transp: 'Mercado Livre', tam: 'Médio', recebida: 'hoje 09:29', local: 'Prateleira B2', codigo: '4821', status: 'aguardando', avisos: 1 },
            { id: 2, unidade: 'B-204', dest: 'Ana Souza', transp: 'Amazon', tam: 'Envelope', recebida: '30/10 15:10', local: 'Prateleira B1', codigo: '1937', status: 'aguardando', avisos: 2 },
            { id: 6, unidade: 'A-205', dest: 'Felipe Araújo', transp: 'Amazon', tam: 'Pequeno', recebida: 'hoje 08:15', local: 'Prateleira A2', codigo: '7700', status: 'aguardando', avisos: 1 },
            { id: 12, unidade: 'D-601', dest: 'Nicolas Moura', transp: 'iFood Mercado', tam: 'Médio', recebida: 'hoje 09:05', local: 'Geladeira', codigo: '2214', status: 'aguardando', avisos: 1, perecivel: true },
            { id: 3, unidade: 'C-305', dest: 'Bruno Campos', transp: 'Correios', tam: 'Grande', recebida: '23/10 11:02', local: 'Sala de volumes', codigo: '5540', status: 'aguardando', avisos: 4 },
            { id: 9, unidade: 'B-503', dest: 'Caio Monteiro', transp: 'Loggi', tam: 'Pequeno', recebida: 'ontem 09:12', local: 'Prateleira B3', codigo: '8831', status: 'aguardando', avisos: 1 },
            { id: 15, unidade: 'A-302', dest: 'Vinícius Ramos', transp: 'Amazon', tam: 'Médio', recebida: 'hoje 07:58', local: 'Prateleira A2', codigo: '3390', status: 'retirada', retiradaEm: 'hoje 08:40', retiradoPor: 'Vinícius Ramos' },
            { id: 16, unidade: 'C-501', dest: 'Thiago Barros', transp: 'Mercado Livre', tam: 'Pequeno', recebida: 'ontem 15:20', local: 'Prateleira C1', codigo: '9021', status: 'retirada', retiradaEm: 'hoje 07:10', retiradoPor: 'Thiago Barros' },
            { id: 17, unidade: 'D-305', dest: 'Mariana Lopes', transp: 'Shopee', tam: 'Pequeno', recebida: '31/10 09:00', local: 'Prateleira D1', codigo: '4410', status: 'retirada', retiradaEm: 'ontem 18:22', retiradoPor: 'Amanda Lopes (babá, autorizada)' }
        ],

        transportadoras: ['Mercado Livre', 'Amazon', 'Shopee', 'Correios', 'Magalu', 'Loggi', 'Jadlog', 'Total Express', 'iFood Mercado', 'Outra'],
        locaisGuarda: ['Prateleira A', 'Prateleira B', 'Prateleira C', 'Prateleira D', 'Sala de volumes', 'Geladeira da portaria'],

        // leitura de gás (novembro): A e B ok, C parcial, D pendente
        gas: { lidas: 82, total: 120, pendentes: 38, alertas: 3 },

        livro: [
            { hora: '09:12', tipo: 'ocorrencia', unidade: 'B-102', texto: 'Carro na vaga de visitante há mais de 4 h (placa PQA1B23). Morador avisado.' },
            { hora: '08:30', tipo: 'ronda', texto: 'Ronda da manhã: portão de serviço estava destrancado; trancado e registrado.' },
            { hora: '08:10', tipo: 'chave', unidade: 'A-501', texto: 'Chave do salão emprestada para o A-501 (decoração da festa de hoje).' },
            { hora: '08:05', tipo: 'prestador', texto: 'Técnico da Elevadores Atlas na casa de máquinas do Bloco A.' },
            { hora: '07:12', tipo: 'prestador', texto: 'Jardinagem Verde Vida entrou (Edson + 1 ajudante) para a poda.' },
            { hora: '06:02', tipo: 'passagem', texto: 'Passagem de turno: Antônio Ferreira → Carlos Mendes. Assinada pelos dois.' }
        ],

        // reservas que a portaria vê no dia
        reservasHoje: [
            { id: 901, area: 'Salão de festas', item: 'Salão', turno: 'Noite (17h–23h)', unidade: 'A-501', morador: 'Marcos Teixeira', convidados: 40, chave: 'entregue', obs: 'Aniversário de 40 anos — buffet às 15h' },
            { id: 902, area: 'Churrasqueiras', item: 'Churrasqueira 1 · Deck', turno: 'Noite (17h–23h)', unidade: 'C-202', morador: 'Otávio Freitas', convidados: 12, chave: '—' },
            { id: 903, area: 'Quadra', item: 'Quadra', turno: '18h–19h', unidade: 'D-104', morador: 'Carla Mendonça', convidados: 8, chave: '—' }
        ],

        // ---------------- NOTIFICAÇÕES ----------------
        notifs: {
            morador: [
                { ic: 'inventory_2', tone: 'warn', t: 'Encomenda na portaria', s: 'Mercado Livre · pacote médio. Código 4821.', q: 'há 12 min', lida: false, ir: 'app/encomendas' },
                { ic: 'how_to_vote', tone: 'danger', t: 'Assembleia ao vivo agora', s: 'AGE de novembro começou. Entre para votar.', q: 'há 20 min', lida: false, ir: 'app/mais' },
                { ic: 'description', tone: 'violet', t: 'Documento para assinar', s: 'Termo do novo regimento das churrasqueiras.', q: 'há 2 h', lida: false, ir: 'app/mais' },
                { ic: 'event_available', tone: 'success', t: 'Reserva confirmada', s: 'Churrasqueira 3 · sáb., 14/11 · Almoço.', q: 'ontem', lida: true, ir: 'app/reservas' },
                { ic: 'account_balance_wallet', tone: 'info', t: 'Boleto de novembro disponível', s: 'Vence 10/11 · R$ 612,40.', q: 'há 3 dias', lida: true, ir: 'app/boletos' }
            ],
            portaria: [
                { ic: 'id_card', tone: 'info', t: 'Visitante pré-autorizado', s: 'Lucas Prado p/ C-303, 14h–18h · RTA-4B21.', q: 'há 5 min', lida: false, ir: 'port/visitantes' },
                { ic: 'event', tone: 'warn', t: 'Reserva hoje à noite', s: 'Salão · A-501 · 40 convidados liberados.', q: 'há 1 h', lida: false, ir: 'port/reservas' },
                { ic: 'water_drop', tone: 'violet', t: 'Leitura de gás pendente', s: 'Faltam 38 unidades do Bloco D.', q: 'há 3 h', lida: true, ir: 'port/painel' }
            ]
        },

        // ---------------- APP COMPLETO (morador) ----------------
        boletoAtual: {
            competencia: 'Novembro / 2026', venc: '10/11/2026', valor: 612.40, status: 'em_aberto', dias: 6,
            linha: '03399.63290 64000.000008 00125.201028 4 10110000061240',
            pix: '00020126580014br.gov.bcb.pix0136a1b2c3d4-vila-do-sol-2-condominio5204000053039865802BR',
            itens: [
                { k: 'Taxa condominial', v: 480.00 },
                { k: 'Fundo de reserva (10%)', v: 48.00 },
                { k: 'Consumo de gás (out)', v: 54.40 },
                { k: 'Reserva churrasqueira 3 (14/11)', v: 30.00 }
            ]
        },
        boletosPagos: [
            { competencia: 'Outubro / 2026', venc: '10/10', valor: 598.10, pago: '08/10', via: 'Pix' },
            { competencia: 'Setembro / 2026', venc: '10/09', valor: 581.70, pago: '10/09', via: 'Pix' },
            { competencia: 'Agosto / 2026', venc: '10/08', valor: 574.30, pago: '09/08', via: 'Boleto' }
        ],

        areas: [
            { id: 'churrasqueiras', nome: 'Churrasqueiras', ic: 'local_fire_department', c: '#EA580C', desc: '4 churrasqueiras ao redor da piscina', taxa: 80, turno: true, termo: true, aprova: false,
              itens: [
                  { id: 1, nome: 'Churrasqueira 1 · Deck', cap: 20, taxa: 80, rec: 'Coberta · Pia · Freezer', status: 'ativa' },
                  { id: 2, nome: 'Churrasqueira 2 · Jardim', cap: 15, taxa: 60, rec: 'Ao ar livre · Pia', status: 'ativa' },
                  { id: 3, nome: 'Churrasqueira 3 · Gourmet', cap: 30, taxa: 120, rec: 'Coberta · Forno de pizza · TV', status: 'ativa' },
                  { id: 4, nome: 'Churrasqueira 4 · Playground', cap: 12, taxa: 60, rec: 'Ao lado do parquinho', status: 'manutencao' }
              ] },
            { id: 'salao', nome: 'Salão de festas', ic: 'celebration', c: '#7C3AED', desc: 'Climatizado, até 80 pessoas', taxa: 250, turno: true, termo: true, aprova: true,
              itens: [{ id: 1, nome: 'Salão de festas', cap: 80, taxa: 250, rec: 'Ar-condicionado · Cozinha · Som', status: 'ativa' }] },
            { id: 'quadra', nome: 'Quadra', ic: 'sell', c: '#059669', desc: 'Reservas de 1 h, 7h às 22h', taxa: 0, turno: false, termo: false, aprova: false,
              itens: [{ id: 1, nome: 'Quadra poliesportiva', cap: 12, taxa: 0, rec: 'Iluminação · Rede · Traves', status: 'ativa' }] },
            { id: 'gourmet', nome: 'Espaço gourmet', ic: 'celebration', c: '#D97706', desc: 'Cozinha equipada, mesa p/ 16', taxa: 150, turno: true, termo: true, aprova: false,
              itens: [{ id: 1, nome: 'Espaço gourmet', cap: 16, taxa: 150, rec: 'Cooktop · Forno · Adega', status: 'ativa' }] }
        ],
        turnos: [ { id: 'almoco', nome: 'Almoço', hora: '10h – 16h', ic: 'light_mode' }, { id: 'noite', nome: 'Noite', hora: '17h – 23h', ic: 'bedtime' } ],
        minhasReservas: [
            { id: 1, area: 'Churrasqueira 3 · Gourmet', ic: 'local_fire_department', c: '#EA580C', data: 'sáb., 14 de novembro', turno: 'Almoço (10h–16h)', convidados: 18, status: 'confirmada' },
            { id: 2, area: 'Salão de festas', ic: 'celebration', c: '#7C3AED', data: 'sáb., 12 de dezembro', turno: 'Noite (17h–23h)', convidados: 60, status: 'aprovacao' }
        ],
        // ocupação do calendário (dia -> nº de áreas livres de 3) p/ colorir
        ocupacao: { 7: 'part', 8: 'part', 14: 'full', 15: 'part', 21: 'free', 22: 'part', 28: 'full', 29: 'free' },

        meusVisitantes: [
            { id: 2, nome: 'Juliana Martins', obs: 'Diarista · seg, qua e sex', status: 'recorrente', janela: '10h – 17h' },
            { id: 20, nome: 'Rodrigo Souza', obs: 'Cônjuge · acesso permanente', status: 'permanente', janela: 'Sempre' }
        ],

        avisos: [
            { id: 1, t: 'Interrupção de água — sexta, 06/11', cat: 'Manutenção', tone: 'warn', data: '01/11', autor: 'Roberto Lima (síndico)', fixado: true, lido: false, txt: 'Na sexta faremos a limpeza semestral das caixas d’água. O fornecimento será interrompido das 9h às 13h em todos os blocos. Reserve água para o período.' },
            { id: 2, t: 'Novo regimento das churrasqueiras', cat: 'Regras', tone: 'info', data: '29/10', autor: 'Administração', fixado: false, lido: false, txt: 'A partir de 15/11 as reservas das churrasqueiras passam a ser por turno (almoço ou noite) e exigem assinatura do termo no próprio app.' },
            { id: 3, t: 'Prestação de contas de outubro', cat: 'Financeiro', tone: 'success', data: '28/10', autor: 'Administração', fixado: false, lido: true, txt: 'O balancete de outubro já está disponível, com todas as notas fiscais anexadas.' },
            { id: 5, t: 'Festa das crianças — 15/11', cat: 'Social', tone: 'violet', data: '18/10', autor: 'Comissão de eventos', fixado: false, lido: true, txt: 'Pula-pula, pipoca e algodão-doce no salão. Confirme a presença das crianças pelo app.' }
        ],

        // grade do "Mais" do app completo
        maisMorador: [
            { ir: 'app/documentos', ic: 'description', c: '#7C3AED', l: 'Documentos' },
            { ir: 'app/chamados', ic: 'handyman', c: '#2563EB', l: 'Chamados' },
            { ir: 'app/gas', ic: 'water_drop', c: '#0891B2', l: 'Gás' },
            { ir: 'app/assembleias', ic: 'how_to_vote', c: '#DB2777', l: 'Assembleias' },
            { ir: 'app/prestacao', ic: 'fact_check', c: '#059669', l: 'Prest. contas' },
            { ir: 'app/ocorrencias', ic: 'warning', c: '#D97706', l: 'Ocorrências' },
            { ir: 'app/unidade', ic: 'apartment', c: '#1F4FE0', l: 'Minha unidade' },
            { ir: 'app/calendario', ic: 'calendar_month', c: '#0D9488', l: 'Calendário' }
        ]
    };

    window.MC_DATA = MC;
})();
