/* Meu Condomínio · Android — protótipo navegável (sem dependências).
   Dois modos: Portaria (rápido) e App completo (morador). Pilha de telas com animações do Material 3,
   folhas inferiores, diálogos e snackbars, tudo dentro do aparelho. */
'use strict';
(() => {
    const D = window.MC_DATA;

    /* ============================== utilidades ============================== */
    const $ = (s, el = document) => el.querySelector(s);
    const $$ = (s, el = document) => [...el.querySelectorAll(s)];
    const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const ic = (n, cls = '') => `<span class="ms ${cls}" aria-hidden="true">${n}</span>`;
    const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const money = v => BRL.format(v);
    const money2 = v => { const s = BRL.format(v); const i = s.lastIndexOf(','); return `${s.slice(0, i)}<small>${s.slice(i)}</small>`; };
    const es0 = s => String(s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
    const go = (id, p) => `data-go="${id}"${p ? ` data-p='${esc(JSON.stringify(p))}'` : ''}`;

    /* ============================== estado ============================== */
    const state = {
        stack: [], theme: 'system', mode: null,
        visitantes: D.visitantes.map(v => ({ ...v })),
        encomendas: D.encomendas.map(e => ({ ...e })),
        notifsM: D.notifs.morador.map(n => ({ ...n })),
        notifsP: D.notifs.portaria.map(n => ({ ...n })),
        avisos: D.avisos.map(a => ({ ...a })),
        rsv: null, encForm: null, visFilter: 'esperado', encFilter: 'aguardando', showPass: false, _nid: 500
    };

    /* ============================== componentes ============================== */
    const avatarTxt = t => `<span class="avatar">${esc(t)}</span>`;
    const top = (title, { back = true, close = false, acts = '' } = {}) =>
        `<header class="top" data-top>${back ? `<button class="ib" data-back aria-label="${close ? 'Fechar' : 'Voltar'}">${ic(close ? 'close' : 'arrow_back')}</button>` : '<span style="width:8px"></span>'}<h1>${title}</h1>${acts}</header>`;

    const badge = (label, tone = 'neutral', icon) => `<span class="badge b-${tone}">${icon ? ic(icon) : ''}${esc(label)}</span>`;
    const kpi = ({ ic: i, v, l, c }) => `<div class="kpi"${c ? ` style="--c:${c}"` : ''}><span class="ic">${ic(i)}</span><div class="v">${v}</div><div class="l">${esc(l)}</div></div>`;
    const sech = (title, sub = '', link = '') => `<div class="sech"><h2>${esc(title)}${sub ? `<small>${esc(sub)}</small>` : ''}</h2>${link}</div>`;
    const linkBtn = (label, attrs) => `<button class="link" ${attrs}>${esc(label)}${ic('chevron_right')}</button>`;
    const plate = p => p ? `<span class="plate"><span class="br">BRASIL</span><span class="pl">${esc(fmtPlaca(p))}</span></span>` : '';
    const fmtPlaca = p => { const s = String(p).toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 7); return /^[A-Z]{3}\d{4}$/.test(s) ? `${s.slice(0, 3)}-${s.slice(3)}` : s; };

    const TIPOS = {
        visita: { l: 'Visita', ic: 'person', c: '#1F4FE0' }, prestador: { l: 'Prestador', ic: 'handyman', c: '#D97706' },
        entrega: { l: 'Entrega', ic: 'inventory_2', c: '#0891B2' }, app: { l: 'App de transporte', ic: 'directions_car', c: '#7C3AED' }
    };
    const VIA = { app: 'Autorizado no app', recorrente: 'Recorrente', portaria: 'Cadastrado na portaria', interfone: 'Liberado pelo interfone' };
    const visStatusBadge = v => {
        if (v.status === 'esperado') return badge('Esperado', 'info', 'schedule');
        if (v.status === 'aguardando') return badge('Aguardando morador', 'warn', 'pending');
        if (v.status === 'dentro') return badge('No condomínio', 'success', 'sensor_occupied');
        if (v.status === 'saiu') return badge('Já saiu', 'neutral', 'logout');
        if (v.status === 'recusado') return badge('Recusado', 'danger', 'close');
        return '';
    };

    /* ---- navegação inferior por modo ---- */
    const NAVS = {
        port: [['port/painel', 'Painel', 'grid_view'], ['port/visitantes', 'Visitantes', 'id_card'], ['port/encomendas', 'Encomendas', 'inventory_2'], ['port/mais', 'Mais', 'menu']],
        app: [['app/inicio', 'Início', 'home'], ['app/reservas', 'Reservas', 'calendar_month'], ['app/boletos', 'Boletos', 'account_balance_wallet'], ['app/mais', 'Mais', 'menu']]
    };
    function navBar(mode, cur) {
        return `<nav class="nav" aria-label="Navegação">${NAVS[mode].map(([id, l, i]) =>
            `<button data-root="${id}" class="${id === cur ? 'on' : ''}" aria-current="${id === cur ? 'page' : 'false'}"><i>${ic(i)}</i>${l}</button>`).join('')}</nav>`;
    }
    const fab = (label, act, extra = '') => `<button class="fab" ${act.startsWith('go:') ? `data-go="${act.slice(3)}"` : `data-act="${act}"`} ${extra}>${ic('add')}<span>${esc(label)}</span></button>`;

    /* ============================== TELAS ============================== */
    const S = {};

    /* ---------- entrada ---------- */
    S['splash'] = {
        bare: true,
        body: () => `<div class="splash">
            <span class="logo">${markSvg(58)}</span>
            <h1>Meu<i> Condomínio</i></h1>
            <p>Gestão inteligente. Condomínio ainda melhor.</p>
            <span class="spin"></span>
        </div>`,
        after: () => setTimeout(() => { if (cur().id === 'splash') navTo('login'); }, 1400),
        notes: { crumb: 'Entrada', title: 'Abertura', text: 'Splash com a marca. Em 1,4 s o app abre a tela de entrada. Toque em qualquer lugar também avança.', tip: 'Toque no aparelho para pular.' },
        tap: () => navTo('login')
    };

    S['login'] = {
        bare: true,
        body: () => `<div class="auth">
            <span class="logo">${markSvg(40)}</span>
            <h1>Bem-vindo de volta</h1>
            <p class="lead">Entre com seu e-mail ou CPF e a senha para acessar o ${esc(D.condo.nome)}.</p>
            <div class="field"><span class="lab">E-mail ou CPF</span><input class="inp" value="ana.souza@email.com"></div>
            <div class="field"><span class="lab">Senha</span><div class="inp-ic"><input class="inp" type="${state.showPass ? 'text' : 'password'}" value="123456"><button class="ib" style="position:absolute;right:2px;top:2px" data-act="togglePass">${ic(state.showPass ? 'visibility_off' : 'visibility')}</button></div></div>
            <div class="row" style="justify-content:space-between;margin-top:12px"><label class="row sm" style="gap:7px"><input type="checkbox" checked> Lembrar de mim</label><a class="link" data-act="esqueci">Esqueci a senha</a></div>
            <button class="btn btn-primary btn-lg btn-block mt20" data-go="perfil">Entrar</button>
            <div class="row" style="justify-content:center;margin-top:16px;color:var(--muted)">${ic('fingerprint')}<span class="sm">Entrar com biometria</span></div>
            <div class="grow"></div>
            <p class="center sm muted mt20">Novo por aqui? <a class="link" data-act="cadastro">Criar conta</a></p>
        </div>`,
        notes: { crumb: 'Entrada', title: 'Entrar', text: 'Login próprio do app (padrão e-Finances): e-mail/CPF e senha, com opção de biometria. Os dados já vêm preenchidos para o protótipo.', bullets: ['Toque em Entrar para escolher o perfil.', 'A senha mostra/esconde no olho.'] }
    };

    S['perfil'] = {
        bare: true,
        body: () => `<div class="auth">
            <span class="logo">${markSvg(40)}</span>
            <h1>Como você quer entrar?</h1>
            <p class="lead">No app real, isso vem do seu papel no condomínio. No protótipo, escolha para ver os dois modos.</p>
            <div class="pick mt10">
                <button class="pk-port" data-act="entrarPort"><span class="ic">${ic('shield')}</span><div class="grow"><div class="t">Modo Portaria</div><div class="d">Rápido para o porteiro: encomendas, visitantes e liberação de entrada.</div></div><span class="chev">${ic('chevron_right')}</span></button>
                <button class="pk-app" data-act="entrarApp"><span class="ic">${ic('apartment')}</span><div class="grow"><div class="t">App do morador</div><div class="d">Completo: reservas, boletos, encomendas, avisos e mais.</div></div><span class="chev">${ic('chevron_right')}</span></button>
            </div>
            <div class="note mt20">${ic('info')}<span>A mesma pessoa pode ter mais de um papel. O porteiro vê o <b>modo rápido</b>; o morador vê o <b>app completo</b>.</span></div>
        </div>`,
        notes: { crumb: 'Entrada', title: 'Escolher o perfil', text: 'Ponte para os dois modos do protótipo. Escolha Portaria (modo rápido) ou o App do morador (completo).', bullets: ['Portaria → painel do turno.', 'Morador → início do app.'] }
    };

    /* =====================================================================
       MODO PORTARIA (rápido)
       ===================================================================== */
    S['port/painel'] = {
        mode: 'port', nav: 'port/painel',
        hero: () => `<div class="hero">${WM}
            <div class="hero-row">
                <span class="avatar">${esc(D.porteiro.iniciais)}</span>
                <div class="grow"><div class="hi">Portaria · ${esc(D.porteiro.turno)}</div><div class="nm">Olá, ${esc(D.porteiro.primeiro)}</div><div class="sub">${esc(D.condo.nome)}</div></div>
                <div class="hero-right">
                    <button class="hero-switch" data-act="entrarApp" title="Trocar para o app do morador">${ic('swap_horiz')}Morador</button>
                    <button class="hero-ic" data-go="port/notifs">${ic('notifications')}${state.notifsP.some(n => !n.lida) ? '<span class="dot">' + state.notifsP.filter(n => !n.lida).length + '</span>' : ''}</button>
                </div>
            </div>
            <div style="margin-top:16px" class="search" onclick="MC.focusSearch()"><span class="ms">search</span><input id="pq" placeholder="Buscar morador, unidade ou placa…" oninput="MC.search(this.value)" autocomplete="off"></div>
        </div>`,
        body: () => {
            const dentro = state.visitantes.filter(v => v.status === 'dentro').length;
            const esper = state.visitantes.filter(v => v.status === 'esperado' || v.status === 'aguardando').length;
            const enc = state.encomendas.filter(e => e.status === 'aguardando').length;
            return `<div id="pres"></div>
            <div class="kgrid mt6">
                ${kpi({ ic: 'schedule', v: esper, l: 'Esperados hoje', c: '#1F4FE0' })}
                ${kpi({ ic: 'sensor_occupied', v: dentro, l: 'No condomínio', c: '#059669' })}
                ${kpi({ ic: 'inventory_2', v: enc, l: 'Encomendas', c: '#D97706' })}
            </div>
            ${sech('Ações rápidas')}
            <div class="qa">
                <button class="hot" data-go="port/nova-encomenda"><span class="ic">${ic('inventory_2')}</span><div class="t">Registrar encomenda</div><div class="d">Chegou um pacote → avisa o morador</div></button>
                <button data-go="port/nova-visita" style="--c:#1F4FE0"><span class="ic">${ic('person_add')}</span><div class="t">Nova visita</div><div class="d">Liberar na hora</div></button>
                <button data-go="port/encomendas" style="--c:#D97706"><span class="ic">${ic('local_shipping')}</span><div class="t">Entregar</div><div class="d">Retirada com código</div></button>
                <button class="wide" data-go="port/reservas" style="--c:#7C3AED"><span class="ic">${ic('event')}</span><div class="bd"><div class="t">Reservas de hoje</div><div class="d">${D.reservasHoje.length} áreas reservadas · check-in de convidados</div></div>${ic('chevron_right')}</button>
            </div>
            ${sech('Esperados agora', '', linkBtn('Ver todos', 'data-go="port/visitantes"'))}
            <div class="card"><div class="list">${state.visitantes.filter(v => v.status === 'esperado' || v.status === 'aguardando').slice(0, 3).map(visRow).join('')}</div></div>
            ${sech('No condomínio agora', '', linkBtn('Ver todos', `data-go="port/visitantes" data-p='{"f":"dentro"}'`))}
            <div class="card"><div class="list">${state.visitantes.filter(v => v.status === 'dentro').slice(0, 3).map(visRow).join('')}</div></div>`;
        },
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Painel do turno', text: 'A tela que o porteiro mais usa. Busca grande no topo, números do turno e as ações de 1 toque. Tudo em âmbar para destacar a função.', bullets: ['Busca por nome, unidade ou placa.', 'Registrar encomenda é a ação em destaque.', 'Esperados e quem está dentro, logo abaixo.'], tip: 'Digite na busca para filtrar na hora.' }
    };

    function visRow(v) {
        const t = TIPOS[v.tipo];
        return `<div class="li tap" ${go('port/visitante', { id: v.id })}>
            <span class="av" style="--c:${t.c}">${ic(t.ic)}</span>
            <div class="bd"><div class="t">${esc(v.nome)}</div><div class="s">${esc(v.unidade)} · ${esc(t.l)}${v.obs ? ' · ' + esc(v.obs) : ''}</div></div>
            <div class="end">${visStatusBadge(v)}<span class="xs muted">${v.status === 'dentro' ? 'entrou ' + v.entrada : v.inicio + '–' + v.fim}</span></div>
        </div>`;
    }

    S['port/visitantes'] = {
        mode: 'port', nav: 'port/visitantes',
        top: () => top('Visitantes e acesso', { back: false, acts: `<button class="ib" data-go="port/bloqueados">${ic('shield')}</button>` }),
        body: (p) => {
            if (p && p.f) state.visFilter = p.f;
            const f = state.visFilter;
            const filt = { esperado: v => v.status === 'esperado' || v.status === 'aguardando', dentro: v => v.status === 'dentro', saiu: v => v.status === 'saiu' || v.status === 'recusado' };
            const list = state.visitantes.filter(filt[f]);
            const chip = (id, l) => `<button class="chip ${f === id ? 'on' : ''}" data-act="visFilter" data-id="${id}">${esc(l)}</button>`;
            return `<div class="chips mt6">${chip('esperado', 'Esperados')}${chip('dentro', 'No condomínio')}${chip('saiu', 'Histórico de hoje')}</div>
            ${list.length ? `<div class="card mt14"><div class="list">${list.map(visRow).join('')}</div></div>` : emptyBox('id_card', 'Nada por aqui', 'Nenhum visitante neste filtro.')}`;
        },
        fab: () => fab('Nova visita', 'go:port/nova-visita'),
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Visitantes e acesso', text: 'Esperados, quem está no condomínio e o histórico do dia. Toque num visitante para liberar a entrada ou registrar a saída.', bullets: ['O ícone do escudo abre a lista de bloqueados.', 'O + cadastra uma visita avulsa.'] }
    };

    S['port/visitante'] = {
        mode: 'port',
        top: () => top('Visitante', {}),
        body: (p) => {
            const v = state.visitantes.find(x => x.id === p.id) || state.visitantes[0];
            const t = TIPOS[v.tipo];
            return `<div class="card cpad block">
                <div class="row"><span class="av av-xl" style="--c:${t.c}">${ic(t.ic)}</span><div class="grow"><div class="b ink" style="font-size:17px;font-family:var(--f-display)">${esc(v.nome)}</div><div class="sm muted mt6">${badge(t.l, 'neutral', t.ic)} ${visStatusBadge(v)}</div></div></div>
                ${v.placa ? `<div class="divline"></div><div class="row" style="justify-content:space-between">${plate(v.placa)}<span class="sm muted">${esc(v.carro)}</span></div>` : ''}
                <div class="rows mt10">
                    <div class="r"><span class="k">Unidade</span><span class="vv">${esc(v.unidade)} · ${esc(D.nome(v.unidade))}</span></div>
                    <div class="r"><span class="k">Documento</span><span class="vv">${esc(v.doc)}</span></div>
                    ${v.tel ? `<div class="r"><span class="k">Telefone</span><span class="vv">${esc(v.tel)}</span></div>` : ''}
                    <div class="r"><span class="k">Janela</span><span class="vv">${esc(v.inicio)} – ${esc(v.fim)}</span></div>
                    <div class="r"><span class="k">Autorização</span><span class="vv">${esc(VIA[v.via])}${v.recorrente ? ' · ' + esc(v.recorrente) : ''}</span></div>
                    ${v.entrada ? `<div class="r"><span class="k">Entrada</span><span class="vv">${esc(v.entrada)}${v.saida ? ' · saída ' + esc(v.saida) : ''}</span></div>` : ''}
                </div>
            </div>
            ${v.status === 'aguardando' ? `<div class="note mt14">${ic('pending')}<span>Este visitante foi cadastrado na portaria e <b>ainda não foi liberado pelo morador</b>. Você pode chamar a unidade pelo interfone ou liberar manualmente.</span></div>` : ''}
            <div class="row gap8 mt14"><button class="btn btn-ghost grow" data-act="ligarUnidade" data-u="${esc(v.unidade)}">${ic('call')}Chamar unidade</button></div>`;
        },
        cta: (p) => {
            const v = state.visitantes.find(x => x.id === p.id) || state.visitantes[0];
            if (v.status === 'dentro') return `<div class="cta"><button class="btn btn-ghost" data-act="voltar">Voltar</button><button class="btn btn-danger grow" data-act="saida" data-id="${v.id}">${ic('logout')}Registrar saída</button></div>`;
            if (v.status === 'esperado' || v.status === 'aguardando') return `<div class="cta"><button class="btn btn-outline" data-act="recusar" data-id="${v.id}">Recusar</button><button class="btn btn-primary grow" data-act="liberar" data-id="${v.id}">${ic('door_front')}Liberar entrada</button></div>`;
            return `<div class="cta"><button class="btn btn-ghost btn-block" data-act="voltar">Voltar</button></div>`;
        },
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Detalhe do visitante', text: 'Tudo que a portaria precisa num cartão: documento, placa, unidade e a janela autorizada. A ação principal fica fixa embaixo.', bullets: ['Esperado → Liberar entrada.', 'No condomínio → Registrar saída.', 'Aguardando → cadastrado na portaria, sem liberação do morador ainda.'] }
    };

    S['port/nova-visita'] = {
        mode: 'port',
        top: () => top('Nova visita', { close: true }),
        body: () => `<p class="muted sm" style="margin:2px 2px 4px">Cadastro rápido de quem chegou sem pré-autorização. O morador é avisado na hora.</p>
            <div class="field"><span class="lab">Unidade de destino</span>${unitSelect('nv-uni')}</div>
            <div class="field"><span class="lab">Nome do visitante</span><input class="inp" id="nv-nome" placeholder="Nome completo"></div>
            <div class="field"><span class="lab">Tipo</span><div class="chips">${['visita', 'prestador', 'entrega', 'app'].map((k, i) => `<button class="chip ${i === 0 ? 'on' : ''}" data-act="pickTipo" data-id="${k}">${ic(TIPOS[k].ic)}${TIPOS[k].l}</button>`).join('')}</div></div>
            <div class="field"><span class="lab">Documento (opcional)</span><input class="inp" id="nv-doc" placeholder="RG ou CPF"></div>
            <div class="field"><span class="lab">Placa do veículo (opcional)</span><input class="inp" id="nv-placa" placeholder="ABC1D23" style="text-transform:uppercase"></div>
            <div class="note mt14">${ic('info')}<span>Ao liberar, o visitante entra como <b>No condomínio</b> e fica no livro da portaria.</span></div>`,
        cta: () => `<div class="cta"><button class="btn btn-primary btn-block" data-act="criarVisita">${ic('door_front')}Liberar e avisar morador</button></div>`,
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Nova visita', text: 'Formulário enxuto para o porteiro não perder tempo: unidade, nome, tipo e, se tiver, documento e placa. Uma ação só: liberar e avisar.', tip: 'No protótipo, pode liberar com os campos vazios para ver o fluxo.' }
    };

    S['port/bloqueados'] = {
        mode: 'port',
        top: () => top('Lista de bloqueados', {}),
        body: () => `<div class="note mt6">${ic('shield')}<span>Pessoas com entrada negada por decisão do síndico ou da administração. Se alguém da lista chegar, avise a administração.</span></div>
            <div class="card mt14"><div class="list">${D.bloqueados.map(b => `<div class="li">
                <span class="av" style="--c:#DC2626">${ic('close')}</span>
                <div class="bd"><div class="t">${esc(b.nome)}</div><div class="s">${esc(b.doc)} · desde ${esc(b.desde)}</div><div class="xs muted mt6">${esc(b.motivo)}</div></div>
            </div>`).join('')}</div></div>`,
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Bloqueados', text: 'Quem não pode entrar, com o motivo e quem decidiu. A busca do painel avisa se um nome bate com a lista.' }
    };

    /* ---- encomendas (portaria) ---- */
    S['port/encomendas'] = {
        mode: 'port', nav: 'port/encomendas',
        top: () => top('Encomendas', { back: false }),
        body: () => {
            const f = state.encFilter;
            const list = state.encomendas.filter(e => f === 'aguardando' ? e.status === 'aguardando' : e.status === 'retirada');
            const chip = (id, l, n) => `<button class="chip ${f === id ? 'on' : ''}" data-act="encFilter" data-id="${id}">${esc(l)}${n != null ? ` · ${n}` : ''}</button>`;
            const aw = state.encomendas.filter(e => e.status === 'aguardando').length;
            return `<div class="chips mt6">${chip('aguardando', 'Aguardando retirada', aw)}${chip('retirada', 'Retiradas hoje')}</div>
            ${list.length ? `<div class="card mt14"><div class="list">${list.map(encRow).join('')}</div></div>` : emptyBox('inventory_2', 'Tudo entregue', 'Nenhuma encomenda neste filtro.')}`;
        },
        fab: () => fab('Registrar', 'go:port/nova-encomenda'),
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Encomendas', text: 'Pacotes aguardando retirada e os já entregues hoje. Toque numa encomenda para entregar com código, ou use o + para registrar uma nova.', bullets: ['O número de avisos mostra há quanto tempo o morador foi lembrado.', 'Perecíveis vão para a geladeira.'] }
    };

    function encRow(e) {
        const late = e.status === 'aguardando' && e.avisos >= 3;
        return `<div class="li tap" ${go('port/encomenda', { id: e.id })}>
            <span class="av" style="--c:${e.perecivel ? '#0891B2' : late ? '#DC2626' : '#D97706'}">${ic(e.perecivel ? 'water_drop' : 'inventory_2')}</span>
            <div class="bd"><div class="t">${esc(e.unidade)} · ${esc(e.dest)}</div><div class="s">${esc(e.transp)} · ${esc(e.tam)} · ${esc(e.local)}</div></div>
            <div class="end">${e.status === 'retirada' ? badge('Retirada', 'success', 'check') : late ? badge(e.avisos + ' avisos', 'danger') : badge('Código ' + e.codigo, 'warn')}<span class="xs muted">${esc(e.recebida)}</span></div>
        </div>`;
    }

    S['port/encomenda'] = {
        mode: 'port',
        top: () => top('Encomenda', {}),
        body: (p) => {
            const e = state.encomendas.find(x => x.id === p.id) || state.encomendas[0];
            return `<div class="card cpad block">
                <div class="row"><span class="av av-xl" style="--c:${e.perecivel ? '#0891B2' : '#D97706'}">${ic(e.perecivel ? 'water_drop' : 'inventory_2')}</span><div class="grow"><div class="b ink" style="font-size:17px;font-family:var(--f-display)">${esc(e.unidade)} · ${esc(e.dest)}</div><div class="sm muted mt6">${e.status === 'retirada' ? badge('Retirada', 'success', 'check') : badge('Código ' + e.codigo, 'warn')} ${e.perecivel ? badge('Perecível', 'info', 'water_drop') : ''}</div></div></div>
                <div class="rows mt10">
                    <div class="r"><span class="k">Transportadora</span><span class="vv">${esc(e.transp)}</span></div>
                    <div class="r"><span class="k">Tamanho</span><span class="vv">${esc(e.tam)}</span></div>
                    <div class="r"><span class="k">Guardada em</span><span class="vv">${esc(e.local)}</span></div>
                    <div class="r"><span class="k">Recebida</span><span class="vv">${esc(e.recebida)}</span></div>
                    ${e.status === 'retirada' ? `<div class="r"><span class="k">Retirada</span><span class="vv">${esc(e.retiradaEm)}</span></div><div class="r"><span class="k">Por</span><span class="vv">${esc(e.retiradoPor)}</span></div>` : `<div class="r"><span class="k">Avisos ao morador</span><span class="vv">${e.avisos}</span></div>`}
                </div>
            </div>
            <div class="sech"><h2>Foto na chegada</h2></div>
            <div class="cam-ph done">${ic('photo_camera')}<span class="sm">Foto registrada</span></div>`;
        },
        cta: (p) => {
            const e = state.encomendas.find(x => x.id === p.id) || state.encomendas[0];
            if (e.status === 'retirada') return `<div class="cta"><button class="btn btn-ghost btn-block" data-act="voltar">Voltar</button></div>`;
            return `<div class="cta"><button class="btn btn-ghost" data-act="avisar" data-id="${e.id}">${ic('notifications')}Avisar</button><button class="btn btn-primary grow" data-act="entregarSheet" data-id="${e.id}">${ic('done_all')}Registrar retirada</button></div>`;
        },
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Detalhe da encomenda', text: 'Foto da chegada, onde está guardada e o código de retirada. "Registrar retirada" confirma quem levou e assina.', bullets: ['"Avisar" manda outro lembrete ao morador.'] }
    };

    S['port/nova-encomenda'] = {
        mode: 'port',
        top: () => top('Registrar encomenda', { close: true }),
        body: () => {
            const f = state.encForm || (state.encForm = { uni: '', transp: 'Mercado Livre', local: 'Prateleira B', foto: false, perec: false });
            return `<p class="muted sm" style="margin:2px 2px 4px">Chegou um pacote. Em 3 toques o morador é avisado com o código de retirada.</p>
            <div class="field"><span class="lab">Unidade de destino</span>${unitSelect('ne-uni', f.uni, "MC.setEnc('uni',this.value)")}</div>
            <div class="field"><span class="lab">Transportadora</span><div class="chips">${D.transportadoras.map(t => `<button class="chip ${f.transp === t ? 'on' : ''}" data-act="pickTransp" data-id="${esc(t)}">${esc(t)}</button>`).join('')}</div></div>
            <div class="field"><span class="lab">Foto do pacote</span><div class="cam-ph ${f.foto ? 'done' : ''}" data-act="tirarFoto">${ic(f.foto ? 'photo_camera' : 'add_a_photo')}<span class="sm">${f.foto ? 'Foto registrada — toque para refazer' : 'Tirar foto'}</span></div></div>
            <div class="field"><span class="lab">Onde vai guardar</span>
                <div class="chips">${D.locaisGuarda.map(l => `<button class="chip ${f.local === l ? 'on' : ''}" data-act="pickLocal" data-id="${esc(l)}">${esc(l)}</button>`).join('')}</div></div>
            <label class="note mt14" style="align-items:center"><input type="checkbox" ${f.perec ? 'checked' : ''} onchange="MC.setEnc('perec',this.checked)"> <span>${ic('water_drop')} É perecível (vai para a geladeira da portaria)</span></label>`;
        },
        cta: () => `<div class="cta"><button class="btn btn-primary btn-block" data-act="criarEncomenda">${ic('notifications_active')}Registrar e avisar morador</button></div>`,
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Registrar encomenda', text: 'O coração do modo rápido. Unidade, transportadora, foto e local — o sistema gera o código de retirada e dispara o aviso ao morador (app + push).', bullets: ['Transportadora e local por chips, sem digitar.', 'Perecível muda o local para a geladeira.'], tip: 'Pode registrar com campos vazios só para ver o fluxo.' }
    };

    /* ---- reservas do dia (portaria) ---- */
    S['port/reservas'] = {
        mode: 'port',
        top: () => top('Reservas de hoje', {}),
        body: () => `<div class="note mt6">${ic('event')}<span>Áreas reservadas para hoje. Faça o check-in dos convidados e controle a chave na entrega.</span></div>
            <div class="mt14">${D.reservasHoje.map(r => `<div class="card cpad block" style="margin-bottom:12px">
                <div class="row"><span class="av av-lg" style="--c:#7C3AED">${ic('event_available')}</span><div class="grow"><div class="b ink">${esc(r.item)}</div><div class="sm muted mt6">${esc(r.unidade)} · ${esc(r.morador)}</div></div><div class="end">${badge(r.convidados + ' convidados', 'violet', 'group')}</div></div>
                <div class="rows mt10"><div class="r"><span class="k">Turno</span><span class="vv">${esc(r.turno)}</span></div><div class="r"><span class="k">Chave</span><span class="vv">${r.chave === 'entregue' ? badge('Entregue', 'success', 'key') : '—'}</span></div>${r.obs ? `<div class="r"><span class="k">Obs.</span><span class="vv" style="max-width:60%">${esc(r.obs)}</span></div>` : ''}</div>
                <div class="row gap8 mt14"><button class="btn btn-soft grow btn-sm" data-act="checkin">${ic('group')}Check-in convidados</button><button class="btn btn-ghost btn-sm" data-act="chave">${ic('key')}Chave</button></div>
            </div>`).join('')}</div>`,
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Reservas de hoje', text: 'A lista de convidados de cada reserva chega na portaria. Check-in dos convidados e controle da chave da área.' }
    };

    /* ---- mais (portaria) ---- */
    S['port/mais'] = {
        mode: 'port', nav: 'port/mais',
        top: () => top('Mais', { back: false }),
        body: () => `<div class="card cpad block"><div class="row"><span class="avatar">${esc(D.porteiro.iniciais)}</span><div class="grow"><div class="b ink">${esc(D.porteiro.nome)}</div><div class="sm muted">${esc(D.porteiro.cargo)} · turno ${esc(D.porteiro.turno)}</div></div></div></div>
            ${sech('Rotina da portaria')}
            <div class="card"><div class="list">
                ${miscRow('port/reservas', 'event', '#7C3AED', 'Reservas de hoje', D.reservasHoje.length + ' áreas')}
                ${miscRow('port/livro', 'history', '#2563EB', 'Livro da portaria', 'Rondas, passagens e ocorrências')}
                ${miscRow('port/gas', 'water_drop', '#0891B2', 'Leitura de gás', D.gas.lidas + ' de ' + D.gas.total + ' lidas')}
                ${miscRow('port/veiculos', 'directions_car', '#0D9488', 'Veículos e placas', 'Garagem e autorizações')}
                ${miscRow('port/bloqueados', 'shield', '#DC2626', 'Bloqueados', D.bloqueados.length + ' na lista')}
            </div></div>
            ${sech('Conta')}
            <div class="card"><div class="list">
                ${miscRow('port/notifs', 'notifications', '#D97706', 'Notificações', '')}
                ${miscAct('trocarPerfil', 'swap_horiz', '#1F4FE0', 'Trocar de perfil', 'Ver o app do morador')}
                ${miscAct('sair', 'logout', '#64748B', 'Sair', '')}
            </div></div>`,
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Mais (portaria)', text: 'O resto da rotina do porteiro: livro, gás, veículos. E a troca de perfil para ver o app do morador.' }
    };

    S['port/livro'] = {
        mode: 'port', top: () => top('Livro da portaria', {}),
        body: () => `<div class="note mt6">${ic('history')}<span>Registros do turno. Toque no + para lançar ronda, ocorrência, chave ou passagem de turno.</span></div>
            <div class="card mt14 cpad"><div class="tline">${D.livro.map(l => `<div class="ti"><div class="tt">${esc(PX_tipo(l.tipo))}${l.unidade ? ' · ' + esc(l.unidade) : ''}</div><div class="ts">${esc(l.hora)} — ${esc(l.texto)}</div></div>`).join('')}</div></div>`,
        fab: () => fab('Lançar', 'lancarLivro'),
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Livro da portaria', text: 'Linha do tempo do turno: rondas, ocorrências, chaves e a passagem de turno assinada pelos dois porteiros.' }
    };
    const PX_tipo = t => ({ ronda: 'Ronda', ocorrencia: 'Ocorrência', chave: 'Chave', prestador: 'Prestador', passagem: 'Passagem de turno' }[t] || t);

    S['port/gas'] = {
        mode: 'port', top: () => top('Leitura de gás', {}),
        body: () => `<div class="feature block"><div class="fl"><div class="cap">Novembro / 2026</div><div class="big">${D.gas.lidas}<small> / ${D.gas.total}</small></div><div class="sm" style="opacity:.85;margin-top:4px">unidades lidas · faltam ${D.gas.pendentes}</div>
        <div class="bar" style="margin-top:12px;background:rgba(255,255,255,.25)"><i style="width:${Math.round(D.gas.lidas / D.gas.total * 100)}%;background:#fff"></i></div></div></div>
        <div class="kgrid k2 mt14">${kpi({ ic: 'check_circle', v: D.gas.lidas, l: 'Lidas', c: '#059669' })}${kpi({ ic: 'warning', v: D.gas.alertas, l: 'Fora do padrão', c: '#D97706' })}</div>
        <div class="note mt14">${ic('info')}<span>Bloco A e B completos, C parcial, D pendente. Cada leitura registra o valor e uma foto do medidor.</span></div>
        <button class="btn btn-primary btn-block mt14" data-act="lerGas">${ic('water_drop')}Lançar leitura de uma unidade</button>`,
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Leitura de gás', text: 'O porteiro registra o m³ de cada unidade com foto do medidor; o consumo entra no boleto do mês.' }
    };

    S['port/veiculos'] = {
        mode: 'port', top: () => top('Veículos e placas', {}),
        body: () => `<div class="card mt6"><div class="list">${D.veiculos.map(v => `<div class="li">${plate(v.placa)}<div class="bd" style="margin-left:4px"><div class="t">${esc(v.modelo)}</div><div class="s">${esc(v.unidade)} · vaga ${esc(v.vaga)} · ${esc(v.acesso)}</div></div><div class="end">${v.status === 'ativo' ? badge('Ativo', 'success') : badge('Bloqueado', 'danger')}</div></div>`).join('')}</div></div>
        ${D.veiculos.some(v => v.obs) ? `<div class="note mt14">${ic('info')}<span>${esc(D.veiculos.find(v => v.obs).obs)} — acesso só pelo portão social.</span></div>` : ''}`,
        notes: { port: true, crumb: 'Portaria · rápido', title: 'Veículos e placas', text: 'Veículos cadastrados por unidade, com vaga e forma de acesso (TAG ou leitura de placa).' }
    };

    S['port/notifs'] = notifScreen('port');

    /* =====================================================================
       APP DO MORADOR (completo)
       ===================================================================== */
    S['app/inicio'] = {
        mode: 'app', nav: 'app/inicio',
        hero: () => `<div class="hero">${WM}
            <div class="hero-row">
                <span class="avatar">${esc(D.morador.iniciais)}</span>
                <div class="grow"><div class="hi">${esc(D.morador.unidade)} · ${esc(D.morador.tipo)}</div><div class="nm">Olá, ${esc(D.morador.primeiro)}</div><div class="sub">${esc(D.condo.nome)}</div></div>
                <div class="hero-right">
                    <button class="hero-switch" data-act="entrarPort" title="Trocar para o modo Portaria">${ic('swap_horiz')}Porteiro</button>
                    <button class="hero-ic" data-go="app/notifs">${ic('notifications')}${state.notifsM.some(n => !n.lida) ? '<span class="dot">' + state.notifsM.filter(n => !n.lida).length + '</span>' : ''}</button>
                </div>
            </div></div>`,
        body: () => {
            const b = D.boletoAtual, enc = state.encomendas.filter(e => e.unidade === D.morador.unidade && e.status === 'aguardando');
            return `<div class="feature block" style="margin-top:6px" data-go="app/boletos"><div class="fl">
                <div class="row" style="justify-content:space-between"><span class="cap">Boleto · ${esc(b.competencia)}</span>${badge('Vence em ' + b.dias + ' dias', 'warn')}</div>
                <div class="big">${money2(b.valor)}</div>
                <div class="row gap8 mt14"><button class="btn btn-sm" style="background:#fff;color:#14305F" data-act="pixSheet">${ic('pix')}Pagar com Pix</button><button class="btn btn-sm" style="background:rgba(255,255,255,.18);color:#fff" data-act="copiarLinha">${ic('content_copy')}Copiar código</button></div>
            </div></div>
            ${sech('Atalhos')}
            <div class="qa">
                <button data-go="app/nova-reserva" style="--c:#EA580C"><span class="ic">${ic('local_fire_department')}</span><div class="t">Reservar área</div><div class="d">Churrasqueira, salão…</div></button>
                <button data-go="app/visitantes" style="--c:#1F4FE0"><span class="ic">${ic('id_card')}</span><div class="t">Autorizar visita</div><div class="d">Pré-autorize e gere QR</div></button>
            </div>
            ${enc.length ? `${sech('Encomendas para retirar', '', linkBtn('Ver', 'data-go="app/encomendas"'))}
            <div class="card"><div class="list">${enc.map(e => `<div class="li tap" data-go="app/encomendas"><span class="av" style="--c:#D97706">${ic('inventory_2')}</span><div class="bd"><div class="t">${esc(e.transp)} · ${esc(e.tam)}</div><div class="s">Retire na portaria · código ${esc(e.codigo)}</div></div>${badge('Aguardando', 'warn')}</div>`).join('')}</div></div>` : ''}
            ${sech('Próximas reservas', '', linkBtn('Todas', 'data-go="app/reservas"'))}
            <div class="card"><div class="list">${D.minhasReservas.map(reservaRow).join('')}</div></div>
            ${sech('Avisos do condomínio', '', linkBtn('Mural', 'data-go="app/avisos"'))}
            <div class="card"><div class="list">${state.avisos.slice(0, 2).map(avisoRow).join('')}</div></div>`;
        },
        notes: { crumb: 'App do morador', title: 'Início', text: 'A casa do morador: o boleto do mês em destaque, atalhos para reservar e autorizar visita, encomendas para retirar, próximas reservas e os avisos.', bullets: ['Toque no cartão azul para abrir os boletos.', 'Pix e código de barras direto do início.'] }
    };

    function reservaRow(r) {
        return `<div class="li tap" data-go="app/reservas"><span class="av" style="--c:${r.c}">${ic(r.ic)}</span><div class="bd"><div class="t">${esc(r.area)}</div><div class="s">${esc(r.data)} · ${esc(r.turno)}</div></div><div class="end">${r.status === 'confirmada' ? badge('Confirmada', 'success', 'check') : badge('Em aprovação', 'warn', 'pending')}</div></div>`;
    }
    function avisoRow(a) {
        return `<div class="li tap" ${go('app/aviso', { id: a.id })}><span class="av" style="--c:${toneColor(a.tone)}">${ic(a.fixado ? 'campaign' : avIcon(a.cat))}</span><div class="bd"><div class="t">${esc(a.t)}</div><div class="s">${esc(a.cat)} · ${esc(a.data)}</div></div>${a.lido ? '' : '<span class="badge b-info dotb"></span>'}</div>`;
    }
    const toneColor = t => ({ warn: '#D97706', info: '#1F4FE0', success: '#059669', violet: '#7C3AED', danger: '#DC2626' }[t] || '#1F4FE0');
    const avIcon = c => ({ 'Manutenção': 'handyman', 'Regras': 'gavel', 'Financeiro': 'account_balance_wallet', 'Social': 'celebration', 'Segurança': 'shield' }[c] || 'campaign');

    /* ---- reservas (morador) ---- */
    S['app/reservas'] = {
        mode: 'app', nav: 'app/reservas',
        top: () => top('Reservas', { back: false }),
        body: () => `${sech('Minhas reservas')}
            <div class="card"><div class="list">${D.minhasReservas.map(r => `<div class="li"><span class="av" style="--c:${r.c}">${ic(r.ic)}</span><div class="bd"><div class="t">${esc(r.area)}</div><div class="s">${esc(r.data)} · ${esc(r.turno)} · ${r.convidados} convidados</div></div><div class="end">${r.status === 'confirmada' ? badge('Confirmada', 'success') : badge('Em aprovação', 'warn')}</div></div>`).join('')}</div></div>
            ${sech('Áreas comuns', 'Toque para reservar')}
            <div class="card"><div class="list">${D.areas.map(a => `<div class="li tap" ${go('app/nova-reserva', { area: a.id })}><span class="av" style="--c:${a.c}">${ic(a.ic)}</span><div class="bd"><div class="t">${esc(a.nome)}</div><div class="s">${esc(a.desc)}${a.taxa ? ' · taxa ' + money(a.taxa) : ' · sem taxa'}</div></div>${ic('chevron_right', 'chev')}</div>`).join('')}</div></div>`,
        fab: () => fab('Nova reserva', 'go:app/nova-reserva'),
        notes: { crumb: 'App do morador', title: 'Reservas', text: 'As reservas do morador e o catálogo de áreas comuns. A reserva é um assistente: área → data → turno → termo → confirmação.', bullets: ['A churrasqueira 4 aparece em manutenção.', 'O salão exige aprovação do síndico.'] }
    };

    S['app/nova-reserva'] = {
        mode: 'app',
        top: () => top('Nova reserva', { close: true }),
        body: (p) => {
            if (!state.rsv || (p && p.area && state.rsv.step === 0)) state.rsv = { step: 0, area: (p && p.area) || null, item: null, dia: null, turno: null, termo: false };
            const r = state.rsv;
            return `<div class="dots mt6" style="margin-bottom:16px">${[0, 1, 2, 3].map(i => `<i class="${i === r.step ? 'on' : i < r.step ? 'on' : ''}" style="${i < r.step ? 'background:var(--primary)' : ''}"></i>`).join('')}</div>
                <div id="rsv-step">${rsvStep()}</div>`;
        },
        cta: () => {
            const r = state.rsv; const labels = ['Escolher data', 'Escolher turno', 'Ler o termo', 'Confirmar reserva'];
            const ok = [r && r.item, r && r.dia, r && r.turno, r && r.termo][r ? r.step : 0];
            if (!r) return '';
            return `<div class="cta">${r.step > 0 ? '<button class="btn btn-ghost" data-act="rsvBack">Voltar</button>' : ''}<button class="btn btn-primary grow" data-act="rsvNext" ${ok ? '' : 'disabled'}>${r.step === 3 ? ic('check') : ''}${labels[r.step]}</button></div>`;
        },
        notes: { crumb: 'App do morador', title: 'Assistente de reserva', text: 'Quatro passos com indicador de progresso: a área (mapa das churrasqueiras), a data (calendário verde/âmbar/vermelho), o turno e o termo de uso assinado.', bullets: ['Dias sem vaga ficam vermelhos.', 'Áreas em manutenção aparecem apagadas.'] }
    };

    function rsvStep() {
        const r = state.rsv, area = D.areas.find(a => a.id === r.area) || D.areas[0];
        if (r.step === 0) {
            r.areaObj = area;
            return `<h2 style="font-family:var(--f-display);font-size:18px;margin:0 2px 12px">${esc(area.nome)}</h2>
            <p class="sm muted" style="margin:0 2px 14px">${esc(area.desc)}. Escolha o espaço:</p>
            <div class="map">${area.itens.map(it => `<button class="${r.item === it.id ? 'sel' : ''} ${it.status !== 'ativa' ? 'off' : ''}" ${it.status === 'ativa' ? `data-act="rsvItem" data-id="${it.id}"` : 'disabled'}><div class="nm">${esc(it.nome)}</div><div class="cap">${it.cap} pessoas · ${it.taxa ? money(it.taxa) : 'grátis'}</div><div class="xs muted mt6">${esc(it.rec)}</div>${it.status !== 'ativa' ? `<div class="mt6">${badge('Em manutenção', 'warn', 'handyman')}</div>` : ''}</button>`).join('')}</div>
            <div class="chips mt14" style="flex-wrap:wrap">${D.areas.map(a => `<button class="chip ${a.id === r.area ? 'on' : ''}" data-act="rsvArea" data-id="${a.id}">${ic(a.ic)}${esc(a.nome)}</button>`).join('')}</div>`;
        }
        if (r.step === 1) {
            return `<h2 style="font-family:var(--f-display);font-size:18px;margin:0 2px 12px">Escolha a data</h2>
            ${calendar()}
            <div class="calleg"><span class="lf">Livre</span><span class="lp">Quase cheio</span><span class="lc">Sem vaga</span></div>`;
        }
        if (r.step === 2) {
            const it = area.itens.find(i => i.id === r.item);
            return `<h2 style="font-family:var(--f-display);font-size:18px;margin:0 2px 12px">Turno e convidados</h2>
            <div class="qa" style="grid-template-columns:1fr 1fr">${D.turnos.map(t => `<button class="${r.turno === t.id ? 'hot' : ''}" data-act="rsvTurno" data-id="${t.id}" style="--c:#EA580C"><span class="ic">${ic(t.ic)}</span><div class="t">${esc(t.nome)}</div><div class="d">${esc(t.hora)}</div></button>`).join('')}</div>
            <div class="field"><span class="lab">Convidados (até ${it ? it.cap : 20})</span><input class="inp" type="number" value="12" min="1" max="${it ? it.cap : 20}"></div>
            <div class="note mt14">${ic('group')}<span>A lista de convidados vai para a portaria no dia, para o check-in na entrada.</span></div>`;
        }
        const it = area.itens.find(i => i.id === r.item) || area.itens[0];
        return `<h2 style="font-family:var(--f-display);font-size:18px;margin:0 2px 12px">Termo de uso</h2>
            <div class="card cpad" style="max-height:180px;overflow:auto"><p class="sm" style="margin:0;line-height:1.6;color:var(--text)">Declaro que li e aceito o regimento de uso do(a) <b>${esc(it.nome)}</b>. Comprometo-me a devolver o espaço limpo, respeitar o horário do turno e o limite de convidados, e responder por eventuais danos. A taxa de ${money(it.taxa || 0)} será lançada no próximo boleto.</p></div>
            <label class="note mt14" style="align-items:center"><input type="checkbox" ${state.rsv.termo ? 'checked' : ''} onchange="MC.rsvTermo(this.checked)"> <span>Li e <b>concordo</b> com o termo de uso.</span></label>
            <div class="rows card cpad mt14"><div class="r"><span class="k">Área</span><span class="vv">${esc(it.nome)}</span></div><div class="r"><span class="k">Data</span><span class="vv">${r.dia ? r.dia + '/11/2026' : '—'}</span></div><div class="r"><span class="k">Turno</span><span class="vv">${r.turno ? D.turnos.find(t => t.id === r.turno).nome : '—'}</span></div><div class="r"><span class="k">Taxa</span><span class="vv">${money(it.taxa || 0)}</span></div></div>`;
    }

    function calendar() {
        // novembro/2026 começa num domingo (1 = dom)
        const first = 0; const days = 30, r = state.rsv;
        const cells = [];
        for (let i = 0; i < first; i++) cells.push('<span class="d off"></span>');
        for (let d = 1; d <= days; d++) {
            const occ = D.ocupacao[d];
            const klass = d < 4 ? 'off' : occ === 'full' ? 'full' : occ === 'part' ? 'part' : 'free';
            const clickable = d >= 4 && occ !== 'full';
            cells.push(`<span class="d ${klass} ${r.dia === d ? 'sel' : ''} ${d === 4 ? 'today' : ''}" ${clickable ? `data-act="rsvDia" data-id="${d}"` : ''}>${d}</span>`);
        }
        return `<div class="card cpad"><div class="row" style="justify-content:space-between;margin-bottom:10px"><button class="ib">${ic('chevron_left')}</button><b class="ink">Novembro 2026</b><button class="ib">${ic('chevron_right')}</button></div>
            <div class="cal"><div class="hd">${['D', 'S', 'T', 'Q', 'Q', 'S', 'S'].map(x => `<span>${x}</span>`).join('')}</div><div class="gd">${cells.join('')}</div></div></div>`;
    }

    /* ---- visitantes (morador) ---- */
    S['app/visitantes'] = {
        mode: 'app',
        top: () => top('Meus visitantes', {}),
        body: () => `<div class="note mt6">${ic('id_card')}<span>Pré-autorize visitas e prestadores. A portaria vê a autorização e você recebe quando entram.</span></div>
            ${sech('Autorizações ativas')}
            <div class="card"><div class="list">${D.meusVisitantes.map(v => `<div class="li"><span class="av" style="--c:#1F4FE0">${ic(v.status === 'recorrente' ? 'event_available' : 'verified_user')}</span><div class="bd"><div class="t">${esc(v.nome)}</div><div class="s">${esc(v.obs)}</div></div><div class="end">${badge(v.janela, 'primary')}</div></div>`).join('')}</div></div>
            ${sech('Visita de hoje na portaria')}
            <div class="card"><div class="list">${state.visitantes.filter(v => v.unidade === D.morador.unidade).map(v => `<div class="li"><span class="av" style="--c:${TIPOS[v.tipo].c}">${ic(TIPOS[v.tipo].ic)}</span><div class="bd"><div class="t">${esc(v.nome)}</div><div class="s">${esc(TIPOS[v.tipo].l)}${v.obs ? ' · ' + esc(v.obs) : ''}</div></div><div class="end">${visStatusBadge(v)}</div></div>`).join('') || '<div class="li"><span class="muted sm" style="padding:8px">Nenhuma visita hoje.</span></div>'}</div></div>`,
        fab: () => fab('Autorizar', 'autorizarSheet'),
        notes: { crumb: 'App do morador', title: 'Meus visitantes', text: 'Pré-autorizações do morador (visitas, diarista, prestadores) e o que está acontecendo na portaria hoje. O + gera um QR para compartilhar.', bullets: ['Recorrente serve para diarista/babá.', 'A portaria vê tudo em tempo real.'] }
    };

    /* ---- encomendas (morador) ---- */
    S['app/encomendas'] = {
        mode: 'app',
        top: () => top('Encomendas', {}),
        body: () => {
            const mine = state.encomendas.filter(e => e.unidade === D.morador.unidade);
            const aw = mine.filter(e => e.status === 'aguardando'), rt = mine.filter(e => e.status === 'retirada');
            return `${aw.length ? `${sech('Aguardando retirada')}<div class="card"><div class="list">${aw.map(e => `<div class="li"><span class="av" style="--c:#D97706">${ic('inventory_2')}</span><div class="bd"><div class="t">${esc(e.transp)} · ${esc(e.tam)}</div><div class="s">Recebida ${esc(e.recebida)} · ${esc(e.local)}</div></div><div class="end">${badge('Código ' + e.codigo, 'warn')}</div></div>`).join('')}</div></div>
            <div class="note mt14">${ic('info')}<span>Retire na portaria informando o <b>código</b>. Quer que outra pessoa retire? Autorize abaixo.</span></div>
            <button class="btn btn-soft btn-block mt14" data-act="autorizarTerceiro">${ic('person_add')}Autorizar outra pessoa a retirar</button>` : emptyBox('inventory_2', 'Nada para retirar', 'Quando chegar uma encomenda, ela aparece aqui.')}
            ${rt.length ? `${sech('Retiradas')}<div class="card"><div class="list">${rt.map(e => `<div class="li"><span class="av" style="--c:#059669">${ic('check_circle')}</span><div class="bd"><div class="t">${esc(e.transp)} · ${esc(e.tam)}</div><div class="s">Retirada ${esc(e.retiradaEm)} · ${esc(e.retiradoPor)}</div></div></div>`).join('')}</div></div>` : ''}`;
        },
        notes: { crumb: 'App do morador', title: 'Encomendas', text: 'O que chegou para a sua unidade, o código de retirada e o histórico. Dá para autorizar outra pessoa (cônjuge, babá) a retirar.' }
    };

    /* ---- boletos (morador) ---- */
    S['app/boletos'] = {
        mode: 'app', nav: 'app/boletos',
        top: () => top('Boletos e pagamentos', { back: false }),
        body: () => {
            const b = D.boletoAtual;
            return `<div class="feature block" style="margin-top:6px"><div class="fl">
                <div class="row" style="justify-content:space-between"><span class="cap">${esc(b.competencia)}</span>${badge('Vence ' + b.venc, 'warn')}</div>
                <div class="big">${money2(b.valor)}</div>
                <div class="sm" style="opacity:.85;margin-top:4px">Em aberto · vence em ${b.dias} dias</div>
                <div class="row gap8 mt14"><button class="btn btn-sm grow" style="background:#fff;color:#14305F" data-act="pixSheet">${ic('pix')}Pagar com Pix</button><button class="btn btn-sm" style="background:rgba(255,255,255,.18);color:#fff" data-act="copiarLinha">${ic('content_copy')}Copiar código</button></div>
            </div></div>
            ${sech('Composição do boleto')}
            <div class="card cpad"><div class="rows">${b.itens.map(i => `<div class="r"><span class="k">${esc(i.k)}</span><span class="vv">${money(i.v)}</span></div>`).join('')}<div class="r"><span class="k b ink">Total</span><span class="vv b" style="color:var(--primary)">${money(b.valor)}</span></div></div></div>
            <button class="btn btn-outline btn-block mt14" data-act="verPdf">${ic('picture_as_pdf')}Ver boleto em PDF</button>
            ${sech('Pagos')}
            <div class="card"><div class="list">${D.boletosPagos.map(p => `<div class="li"><span class="av" style="--c:#059669">${ic('check_circle')}</span><div class="bd"><div class="t">${esc(p.competencia)}</div><div class="s">Pago ${esc(p.pago)} · ${esc(p.via)}</div></div><div class="end"><span class="b ink">${money(p.valor)}</span></div></div>`).join('')}</div></div>`;
        },
        notes: { crumb: 'App do morador', title: 'Boletos e pagamentos', text: 'O boleto do mês com Pix copia-e-cola e código de barras, a composição (taxa, fundo, gás, reservas) e o histórico de pagos.', bullets: ['A taxa da reserva da churrasqueira entra aqui.', 'Pix faz baixa automática.'] }
    };

    /* ---- avisos (morador) ---- */
    S['app/avisos'] = {
        mode: 'app',
        top: () => top('Avisos e comunicados', {}),
        body: () => `<div class="card mt6"><div class="list">${state.avisos.map(avisoRow).join('')}</div></div>`,
        notes: { crumb: 'App do morador', title: 'Avisos', text: 'Mural do condomínio. O fixado fica no topo; os não lidos têm o ponto azul. Abrir um aviso confirma a leitura.' }
    };
    S['app/aviso'] = {
        mode: 'app', top: () => top('Aviso', {}),
        body: (p) => {
            const a = state.avisos.find(x => x.id === p.id) || state.avisos[0]; a.lido = true;
            return `<div class="block"><div class="row gap8">${badge(a.cat, mapTone(a.tone), avIcon(a.cat))}${a.fixado ? badge('Fixado', 'danger', 'campaign') : ''}</div>
            <h2 style="font-family:var(--f-display);font-size:20px;margin:12px 2px 6px;color:var(--ink)">${esc(a.t)}</h2>
            <div class="sm muted" style="margin:0 2px 14px">${esc(a.autor)} · ${esc(a.data)}</div>
            <p style="margin:0 2px;line-height:1.65;color:var(--text)">${esc(a.txt)}</p>
            <div class="note mt20">${ic('check_circle')}<span>Leitura confirmada. O síndico vê quem já leu.</span></div></div>`;
        },
        notes: { crumb: 'App do morador', title: 'Aviso', text: 'O comunicado inteiro, com autor e data. Abrir confirma a leitura — o síndico acompanha a confirmação por unidade.' }
    };
    const mapTone = t => ({ warn: 'warn', info: 'info', success: 'success', violet: 'violet', danger: 'danger' }[t] || 'neutral');

    /* ---- mais (morador) ---- */
    S['app/mais'] = {
        mode: 'app', nav: 'app/mais',
        top: () => top('Mais', { back: false }),
        body: () => `<div class="card cpad block"><div class="row"><span class="avatar">${esc(D.morador.iniciais)}</span><div class="grow"><div class="b ink">${esc(D.morador.nome)}</div><div class="sm muted">${esc(D.morador.unidade)} · ${esc(D.morador.tipo)}</div></div><button class="ib" data-go="app/perfil">${ic('chevron_right')}</button></div></div>
            ${sech('Serviços do condomínio')}
            <div class="card cpad"><div class="mgrid">${D.maisMorador.map(m => `<button ${go(m.ir)} style="--c:${m.c}"><span class="ic">${ic(m.ic)}</span><span class="l">${esc(m.l)}</span></button>`).join('')}</div></div>
            ${sech('Conta')}
            <div class="card"><div class="list">
                ${miscRow('app/notifs', 'notifications', '#D97706', 'Notificações', '')}
                ${miscRow('app/perfil', 'settings', '#64748B', 'Perfil e preferências', 'Notificações, aparência, segurança')}
                ${miscAct('trocarPerfil', 'swap_horiz', '#1F4FE0', 'Trocar de perfil', 'Ver o modo Portaria')}
                ${miscAct('sair', 'logout', '#64748B', 'Sair', '')}
            </div></div>`,
        notes: { crumb: 'App do morador', title: 'Mais', text: 'O resto do app do morador: documentos, chamados, gás, assembleias, prestação de contas, ocorrências, unidade e calendário, além de perfil e preferências.', bullets: ['A grade abre cada serviço.', '"Trocar de perfil" leva ao modo Portaria.'] }
    };

    /* ---- telas "em breve" do app completo (atalhos do Mais) ---- */
    const soon = (id, title, icon, color, desc) => { S[id] = { mode: 'app', top: () => top(title, {}), body: () => soonBody(title, icon, color, desc), notes: { crumb: 'App do morador', title, text: desc } }; };
    soon('app/documentos', 'Documentos e assinaturas', 'description', '#7C3AED', 'Documentos pendentes de assinatura e assinados, com assinatura desenhada/digitada e PDF de evidências.');
    soon('app/chamados', 'Chamados', 'handyman', '#2563EB', 'Abrir chamados de manutenção, limpeza ou dúvidas, com conversa, SLA e avaliação.');
    soon('app/gas', 'Consumo de gás', 'water_drop', '#0891B2', 'Leituras mensais, gráfico e comparação com a média da sua unidade.');
    soon('app/assembleias', 'Assembleias e votações', 'how_to_vote', '#DB2777', 'Plenário virtual: edital, pauta, votação por item com quórum e ata.');
    soon('app/prestacao', 'Prestação de contas', 'fact_check', '#059669', 'Receitas e despesas do mês com notas fiscais e balancete publicado.');
    soon('app/ocorrencias', 'Ocorrências', 'warning', '#D97706', 'Registrar ocorrências contra outra unidade e acompanhar, com direito a defesa.');
    soon('app/unidade', 'Minha unidade', 'apartment', '#1F4FE0', 'Moradores, dependentes, veículos e pets da sua unidade.');
    soon('app/calendario', 'Calendário de eventos', 'calendar_month', '#0D9488', 'Manutenções programadas, assembleias e eventos do condomínio.');
    soon('app/perfil', 'Perfil e preferências', 'settings', '#64748B', 'Seus dados, preferências de notificação (canal e horário), aparência e segurança (biometria).');

    S['app/notifs'] = notifScreen('app');

    /* ============================== helpers de UI ============================== */
    function emptyBox(icon, title, text) { return `<div class="empty"><span class="ic">${ic(icon)}</span><b>${esc(title)}</b><p>${esc(text)}</p></div>`; }
    function soonBody(title, icon, color, desc) {
        return `<div class="empty" style="padding-top:60px"><span class="ic" style="background:color-mix(in srgb,${color} 15%,var(--surface));color:${color};width:76px;height:76px;border-radius:24px">${ic(icon)}</span><b style="font-size:17px">${esc(title)}</b><p style="max-width:250px;margin:0 auto">${esc(desc)}</p><div class="mt20">${badge('Em construção', 'info', 'build')}</div><p class="xs muted mt14" style="max-width:240px;margin:14px auto 0">Esta é uma das próximas partes do app. Estamos fazendo por módulos, como combinado.</p></div>`;
    }
    const miscRow = (id, icon, c, t, s) => `<div class="li tap" ${go(id)}><span class="av" style="--c:${c}">${ic(icon)}</span><div class="bd"><div class="t">${esc(t)}</div>${s ? `<div class="s">${esc(s)}</div>` : ''}</div>${ic('chevron_right', 'chev')}</div>`;
    const miscAct = (act, icon, c, t, s) => `<div class="li tap" data-act="${act}"><span class="av" style="--c:${c}">${ic(icon)}</span><div class="bd"><div class="t">${esc(t)}</div>${s ? `<div class="s">${esc(s)}</div>` : ''}</div>${ic('chevron_right', 'chev')}</div>`;

    function unitSelect(id, value = '', onchange = '') {
        const byBloco = {}; D.unidades.forEach(u => { (byBloco[u.bloco] = byBloco[u.bloco] || []).push(u); });
        return `<select class="sel" id="${id}" ${onchange ? `onchange="${onchange}"` : ''}><option value="">Selecione a unidade…</option>${Object.keys(byBloco).map(b => `<optgroup label="Bloco ${b}">${byBloco[b].map(u => `<option value="${u.id}" ${u.id === value ? 'selected' : ''}>${u.id} — ${esc(u.responsavel)}</option>`).join('')}</optgroup>`).join('')}</select>`;
    }

    function notifScreen(mode) {
        return {
            mode, top: () => top('Notificações', {}),
            body: () => {
                const list = mode === 'port' ? state.notifsP : state.notifsM;
                list.forEach(n => n.lida = true);
                return `<div class="card mt6"><div class="list">${list.map(n => `<div class="li tap" ${go(n.ir)}><span class="av" style="--c:${toneColor(n.tone)}">${ic(n.ic)}</span><div class="bd"><div class="t">${esc(n.t)}</div><div class="s">${esc(n.s)}</div><div class="xs muted mt6">${esc(n.q)}</div></div></div>`).join('')}</div></div>`;
            },
            notes: { port: mode === 'port', crumb: mode === 'port' ? 'Portaria · rápido' : 'App do morador', title: 'Notificações', text: 'A central do sino. Abrir marca como lida. Cada item leva direto para a tela do assunto.' }
        };
    }

    /* marca (emblema Meu Condomínio) */
    function markSvg(size) {
        return `<svg viewBox="0 0 32 32" width="${size}" height="${size}" aria-hidden="true"><defs><linearGradient id="mk${size}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#5a97ff"/><stop offset="1" stop-color="#1b3a86"/></linearGradient></defs><rect width="32" height="32" rx="9" fill="url(#mk${size})"/><g fill="#fff"><path d="M6.2 23V16.2q0-1 1-1h3.1V23Z"/><path d="M11.6 23V13l7.2-5v15Z"/><path d="M18.8 23V12h6.1q1 0 1 1v10Z"/></g><g fill="#cfe0ff"><rect x="20.3" y="14" width="1.5" height="1.5" rx=".3"/><rect x="22.9" y="14" width="1.5" height="1.5" rx=".3"/><rect x="20.3" y="16.8" width="1.5" height="1.5" rx=".3"/><rect x="22.9" y="16.8" width="1.5" height="1.5" rx=".3"/><rect x="20.3" y="19.6" width="1.5" height="1.5" rx=".3"/><rect x="22.9" y="19.6" width="1.5" height="1.5" rx=".3"/></g><path d="M5 23.6Q16 29.4 27 20.9" fill="none" stroke="#fff" stroke-width="2.3" stroke-linecap="round"/></svg>`;
    }
    const WM = `<svg class="wmk" viewBox="0 0 32 32" aria-hidden="true"><g fill="#fff"><path d="M6.2 23V16.2q0-1 1-1h3.1V23Z"/><path d="M11.6 23V13l7.2-5v15Z"/><path d="M18.8 23V12h6.1q1 0 1 1v10Z"/></g><path d="M5 23.6Q16 29.4 27 20.9" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round"/></svg>`;

    /* ============================== MOTOR ============================== */
    const views = $('#views'), appEl = $('#app'), ovl = $('#ovl'), snackEl = $('#snack');
    const cur = () => state.stack[state.stack.length - 1] || { id: 'splash' };

    function navTo(id, params = {}) { state.rsv = (id === 'app/nova-reserva' && params.area) ? null : state.rsv; state.stack.push({ id, params }); render('push'); }
    function goRoot(id) {
        // raiz de uma aba: zera a pilha do modo e entra
        state.stack = [{ id, params: {} }]; render('root');
    }
    function back() { if (state.stack.length > 1) { state.stack.pop(); render('back'); } }

    function render(anim) {
        const c = cur(); const def = S[c.id] || S['splash'];
        if (def.mode) { state.mode = def.mode; }
        appEl.classList.toggle('port', def.mode === 'port');
        if (def.mode === 'app') appEl.classList.remove('port');

        const hasNav = !!def.nav;
        const bodyHtml = def.body ? def.body(c.params) : '';

        let inner = '';
        if (def.bare) {
            inner = bodyHtml;
        } else if (def.hero) {
            inner = `<div class="scroll hero-scroll" data-scroll>${def.hero(c.params)}<div class="pad">${bodyHtml}</div></div>`;
        } else {
            inner = `${def.top ? def.top(c.params) : ''}<div class="scroll" data-scroll><div class="pad">${bodyHtml}</div></div>`;
        }
        if (!def.bare) {
            if (def.fab) inner += def.fab(c.params);
            if (hasNav) inner += navBar(def.mode, def.nav);
            if (def.cta) inner += def.cta(c.params);
        }

        const view = document.createElement('div');
        view.className = 'view';
        if (hasNav) view.classList.add('has-nav'); else view.classList.add('no-nav');
        if (def.fab) view.classList.add('has-fab');
        if (def.cta) view.classList.add('has-cta');
        view.innerHTML = inner;

        // mantém no máximo uma view de saída: remove órfãs de transições rápidas
        const olds = $$('.view', views);
        olds.slice(0, -1).forEach(v => v.remove());
        const old = olds[olds.length - 1];
        if (old && anim === 'push') { old.classList.add('out'); view.classList.add('in'); }
        else if (old && anim === 'back') { old.classList.add('backout'); view.classList.add('backin'); }
        else if (old) { old.classList.add('out'); view.classList.add('in'); }

        views.appendChild(view);
        if (old) setTimeout(() => old.remove(), 340);

        // status bar / hero
        appEl.classList.toggle('hero-top', !!def.hero);
        // tema do app
        applyAppTheme();

        // scroll lifted
        const sc = $('[data-scroll]', view);
        if (sc) {
            const topHdr = $('.top', view);
            sc.addEventListener('scroll', () => {
                if (topHdr) topHdr.classList.toggle('lifted', sc.scrollTop > 6);
                if (def.hero) appEl.classList.toggle('hero-top', sc.scrollTop < 70);
            });
        }
        if (def.after) def.after(c.params);
        updateIndex();
        renderNotes(def);
        // reset menu no mobile
        $('#stage').classList.remove('menu');
    }

    /* ---- ações inline (data-act) ---- */
    const actions = {
        togglePass: () => { state.showPass = !state.showPass; rerender(); },
        esqueci: () => dialog('mail', '#1F4FE0', 'Link enviado', 'Enviamos um link para redefinir a senha no seu e-mail. (demonstração)'),
        cadastro: () => dialog('person_add', '#1F4FE0', 'Criar conta', 'No app real, o cadastro é liberado pela administração do condomínio com a sua unidade.'),
        entrarPort: () => { goRoot('port/painel'); snack('Modo Portaria'); },
        entrarApp: () => { goRoot('app/inicio'); snack('App do morador'); },
        trocarPerfil: () => navTo('perfil'),
        sair: () => { state.stack = []; goRoot('login'); },
        voltar: () => back(),

        // portaria · visitantes
        visFilter: (el) => { state.visFilter = el.dataset.id; rerender(); },
        liberar: (el) => { const v = state.visitantes.find(x => x.id === +el.dataset.id); v.status = 'dentro'; v.entrada = clock(); back(); snack('Entrada liberada · ' + v.nome, 'Desfazer', () => { v.status = 'esperado'; delete v.entrada; }); },
        saida: (el) => { const v = state.visitantes.find(x => x.id === +el.dataset.id); v.status = 'saiu'; v.saida = clock(); back(); snack('Saída registrada · ' + v.nome); },
        recusar: (el) => { const v = state.visitantes.find(x => x.id === +el.dataset.id); v.status = 'recusado'; back(); snack('Entrada recusada'); },
        ligarUnidade: (el) => snack('Chamando ' + el.dataset.u + ' pelo interfone…'),
        pickTipo: (el) => { $$('.chips .chip').forEach(c => c.classList.remove('on')); el.classList.add('on'); state._tipo = el.dataset.id; },
        criarVisita: () => {
            const nome = ($('#nv-nome') && $('#nv-nome').value.trim()) || 'Visitante';
            const uni = ($('#nv-uni') && $('#nv-uni').value) || 'B-204';
            const v = { id: ++state._nid, nome, tipo: state._tipo || 'visita', doc: ($('#nv-doc') && $('#nv-doc').value.trim()) || '—', tel: '', unidade: uni, via: 'portaria', inicio: clock(), fim: '23:59', status: 'dentro', entrada: clock(), placa: ($('#nv-placa') && $('#nv-placa').value.trim()) || '', carro: '' };
            state.visitantes.unshift(v); state._tipo = null;
            goRoot('port/painel'); snack(nome + ' liberado · ' + uni + ' avisado');
        },

        // portaria · encomendas
        encFilter: (el) => { state.encFilter = el.dataset.id; rerender(); },
        avisar: (el) => { const e = state.encomendas.find(x => x.id === +el.dataset.id); e.avisos++; snack('Morador avisado novamente · ' + e.unidade); },
        entregarSheet: (el) => entregarSheet(+el.dataset.id),
        setEnc: null, // definido em MC
        pickTransp: (el) => { state.encForm.transp = el.dataset.id; rerender(); },
        pickLocal: (el) => { state.encForm.local = el.dataset.id; rerender(); },
        tirarFoto: () => { state.encForm.foto = !state.encForm.foto; rerender(); },
        criarEncomenda: () => {
            const f = state.encForm || {}; const uni = ($('#ne-uni') && $('#ne-uni').value) || f.uni || 'B-204';
            const cod = String(Math.floor(1000 + Math.random() * 8999));
            const e = { id: ++state._nid, unidade: uni, dest: D.nome(uni), transp: f.transp || 'Mercado Livre', tam: 'Médio', recebida: 'hoje ' + clock(), local: f.perec ? 'Geladeira' : (f.local || 'Prateleira B'), codigo: cod, status: 'aguardando', avisos: 1, perecivel: !!f.perec };
            state.encomendas.unshift(e); state.encForm = null; state.encFilter = 'aguardando';
            goRoot('port/encomendas'); snack('Encomenda registrada · ' + uni + ' avisado (código ' + cod + ')');
        },
        checkin: () => snack('Check-in de convidados aberto'),
        chave: () => snack('Controle de chave registrado no livro'),
        lancarLivro: () => sheet(`<h3>Lançar no livro</h3><p class="sub">O que você quer registrar?</p>${['Ronda', 'Ocorrência', 'Chave', 'Passagem de turno'].map(t => `<button class="li tap" data-act="livroTipo" style="width:100%"><span class="av" style="--c:#2563EB">${ic('history')}</span><div class="bd"><div class="t">${t}</div></div>${ic('chevron_right', 'chev')}</button>`).join('')}`),
        livroTipo: () => { closeOvl(); snack('Registro adicionado ao livro'); },
        lerGas: () => snack('Abrindo leitura da próxima unidade pendente…'),

        // app morador
        pixSheet: () => pixSheet(),
        copiarLinha: () => snack('Código de barras copiado'),
        verPdf: () => snack('Abrindo o boleto em PDF…'),
        autorizarTerceiro: () => sheet(`<h3>Autorizar retirada</h3><p class="sub">Quem pode retirar a encomenda por você?</p><div class="field"><span class="lab">Nome</span><input class="inp" placeholder="Nome de quem vai retirar"></div><div class="field"><span class="lab">Documento</span><input class="inp" placeholder="RG ou CPF"></div><button class="btn btn-primary btn-block mt20" data-act="confAutorizar">Autorizar</button>`),
        confAutorizar: () => { closeOvl(); snack('Pessoa autorizada · a portaria foi avisada'); },
        autorizarSheet: () => sheet(`<h3>Autorizar visita</h3><p class="sub">Pré-autorize e compartilhe o QR com o visitante.</p><div class="qr mt10"></div><div class="center mt14"><div class="b ink">Convite da unidade ${esc(D.morador.unidade)}</div><div class="sm muted">Válido hoje · 08h às 22h</div></div><button class="btn btn-primary btn-block mt20" data-act="confAutorizar2">${ic('content_copy')}Compartilhar convite</button>`),
        confAutorizar2: () => { closeOvl(); snack('Convite compartilhado'); },

        // reserva (wizard)
        rsvArea: (el) => { state.rsv.area = el.dataset.id; state.rsv.item = null; refreshRsv(); },
        rsvItem: (el) => { state.rsv.item = +el.dataset.id; refreshRsv(); },
        rsvDia: (el) => { state.rsv.dia = +el.dataset.id; refreshRsv(); },
        rsvTurno: (el) => { state.rsv.turno = el.dataset.id; refreshRsv(); },
        rsvBack: () => { state.rsv.step--; rerender(); },
        rsvNext: () => {
            const r = state.rsv;
            if (r.step < 3) { r.step++; rerender(); }
            else { const area = D.areas.find(a => a.id === r.area); const aprova = area && area.aprova; goRoot('app/reservas'); snack(aprova ? 'Reserva enviada para aprovação' : 'Reserva confirmada! Taxa no próximo boleto'); }
        }
    };

    function refreshRsv() { const box = $('#rsv-step'); if (box) { box.innerHTML = rsvStep(); } const def = S[cur().id]; const v = $('.view', views); if (v && def.cta) { const old = $('.cta', v); if (old) old.outerHTML = def.cta(cur().params); } }

    function entregarSheet(id) {
        const e = state.encomendas.find(x => x.id === id);
        sheet(`<h3>Registrar retirada</h3><p class="sub">${esc(e.unidade)} · ${esc(e.transp)} · código ${esc(e.codigo)}</p>
            <div class="field"><span class="lab">Quem está retirando?</span>
                <div class="chips"><button class="chip on" data-act="pickRet" data-id="morador">${esc(e.dest)} (morador)</button><button class="chip" data-act="pickRet" data-id="outro">Outra pessoa</button></div></div>
            <div class="field"><span class="lab">Assinatura de quem retirou</span><div class="cam-ph" style="height:90px" data-act="assinar">${ic('edit')}<span class="sm">Tocar para assinar</span></div></div>
            <button class="btn btn-primary btn-block mt20" data-act="confEntrega" data-id="${id}">${ic('done_all')}Confirmar entrega</button>`);
    }
    actions.pickRet = (el) => { $$('.sheet .chip').forEach(c => c.classList.remove('on')); el.classList.add('on'); };
    actions.assinar = (el) => { el.classList.add('done'); el.innerHTML = ic('check') + '<span class="sm">Assinado</span>'; };
    actions.confEntrega = (el) => { const e = state.encomendas.find(x => x.id === +el.dataset.id); e.status = 'retirada'; e.retiradaEm = 'hoje ' + clock(); e.retiradoPor = e.dest; closeOvl(); back(); snack('Entrega registrada · ' + e.unidade); };

    function pixSheet() {
        const b = D.boletoAtual;
        sheet(`<h3>Pagar com Pix</h3><p class="sub">${esc(b.competencia)} · ${money(b.valor)}</p>
            <div class="qr mt10"></div>
            <div class="center mt14"><div class="b ink">${money(b.valor)}</div><div class="sm muted">Baixa automática na hora</div></div>
            <button class="btn btn-primary btn-block mt20" data-act="copiarPix">${ic('content_copy')}Copiar Pix copia e cola</button>
            <button class="btn btn-ghost btn-block mt10" data-close>Fechar</button>`);
    }
    actions.copiarPix = () => { closeOvl(); snack('Pix copia e cola copiado'); };

    /* ---- MC namespace p/ handlers inline ---- */
    window.MC = {
        search(q) { searchResults(q); },
        focusSearch() { const i = $('#pq'); if (i) i.focus(); },
        setEnc(k, v) { state.encForm = state.encForm || {}; state.encForm[k] = v; },
        rsvTermo(v) { state.rsv.termo = v; const def = S[cur().id]; const view = $('.view', views); if (view && def.cta) { const old = $('.cta', view); if (old) old.outerHTML = def.cta(cur().params); } }
    };

    function searchResults(q) {
        const box = $('#pres'); if (!box) return;
        const n = es0(q);
        if (!n) { box.innerHTML = ''; return; }
        const vis = state.visitantes.filter(v => es0(v.nome).includes(n) || es0(v.unidade).includes(n) || es0(v.placa).includes(n));
        const vec = D.veiculos.filter(v => es0(v.placa).includes(n) || es0(v.unidade).includes(n));
        const uni = D.unidades.filter(u => es0(u.id).includes(n) || es0(u.responsavel).includes(n));
        const blk = D.bloqueados.filter(b => es0(b.nome).includes(n));
        let html = '';
        if (blk.length) html += `<div class="note" style="background:var(--expense-soft);color:var(--expense);margin-bottom:10px">${ic('warning')}<span><b>Atenção:</b> ${esc(blk[0].nome)} está na lista de bloqueados. ${esc(blk[0].motivo)}</span></div>`;
        const rows = [];
        vis.slice(0, 4).forEach(v => rows.push(visRow(v)));
        uni.slice(0, 3).forEach(u => rows.push(`<div class="li tap" data-act="noop"><span class="av" style="--c:#1F4FE0">${ic('apartment')}</span><div class="bd"><div class="t">${esc(u.id)} · ${esc(u.responsavel)}</div><div class="s">Unidade</div></div></div>`));
        vec.slice(0, 3).forEach(v => rows.push(`<div class="li">${plate(v.placa)}<div class="bd" style="margin-left:4px"><div class="t">${esc(v.modelo)}</div><div class="s">${esc(v.unidade)} · ${esc(v.acesso)}</div></div>${v.status === 'bloqueado' ? badge('Bloqueado', 'danger') : ''}</div>`));
        box.innerHTML = html + (rows.length ? `<div class="card" style="margin-bottom:6px"><div class="list">${rows.join('')}</div></div>` : emptyBox('search', 'Nada encontrado', 'Tente outro nome, unidade ou placa.'));
    }
    actions.noop = () => {};

    /* ---- overlays ---- */
    function sheet(html) {
        ovl.innerHTML = `<div class="scrim" data-close></div><div class="sheet"><div class="grab"></div>${html}</div>`;
        ovl.style.pointerEvents = 'auto';
        requestAnimationFrame(() => { $('.scrim', ovl).classList.add('show'); $('.sheet', ovl).classList.add('show'); });
    }
    function dialog(icon, color, title, text, acts) {
        ovl.innerHTML = `<div class="scrim" data-close></div><div class="dialog"><span class="dic" style="background:color-mix(in srgb,${color} 15%,var(--surface));color:${color}">${ic(icon)}</span><h3>${esc(title)}</h3><p>${esc(text)}</p><div class="acts">${acts || '<button class="btn btn-primary btn-block" data-close>Entendi</button>'}</div></div>`;
        ovl.style.pointerEvents = 'auto';
        requestAnimationFrame(() => { $('.scrim', ovl).classList.add('show'); $('.dialog', ovl).classList.add('show'); });
    }
    function closeOvl() {
        const sc = $('.scrim', ovl); if (!sc) return;
        sc.classList.remove('show'); const s = $('.sheet', ovl), d = $('.dialog', ovl);
        if (s) s.classList.remove('show'); if (d) d.classList.remove('show');
        setTimeout(() => { ovl.innerHTML = ''; ovl.style.pointerEvents = 'none'; }, 280);
    }

    let snackT;
    function snack(msg, action, fn) {
        clearTimeout(snackT);
        const hasNav = $('.view.has-nav', views);
        snackEl.className = 'snack' + (hasNav ? ' nav-up' : '');
        snackEl.innerHTML = `<span class="msg">${esc(msg)}</span>${action ? `<button class="act" data-snack>${esc(action)}</button>` : ''}`;
        requestAnimationFrame(() => snackEl.classList.add('show'));
        if (action && fn) $('[data-snack]', snackEl).addEventListener('click', () => { fn(); rerender(); hideSnack(); });
        snackT = setTimeout(hideSnack, action ? 4200 : 2600);
    }
    function hideSnack() { snackEl.classList.remove('show'); }

    function rerender() { const c = cur(); render('none'); }

    /* ============================== clock ============================== */
    const t0 = Date.now();
    function clock() {
        const base = 9 * 60 + 41 + Math.floor((Date.now() - t0) / 60000);
        return `${String(Math.floor(base / 60) % 24).padStart(2, '0')}:${String(base % 60).padStart(2, '0')}`;
    }
    function tickClock() { $('#clock').textContent = clock(); }
    setInterval(tickClock, 10000); tickClock();

    /* ============================== tema ============================== */
    function applyAppTheme() {
        const sys = matchMedia('(prefers-color-scheme: dark)').matches;
        const dark = state.theme === 'dark' || (state.theme === 'system' && sys);
        appEl.classList.toggle('dark', dark);
    }
    function setTheme(v) {
        state.theme = v;
        $$('#ptheme button').forEach(b => b.setAttribute('aria-pressed', String(b.dataset.v === v)));
        document.documentElement.setAttribute('data-theme', v === 'system' ? '' : v);
        if (v === 'system') document.documentElement.removeAttribute('data-theme');
        applyAppTheme();
    }

    /* ============================== índice (lateral) ============================== */
    const INDEX = [
        { label: 'Entrada', cls: '', items: [['splash', 'Abertura', 'bolt'], ['login', 'Entrar', 'login'], ['perfil', 'Escolher perfil', 'group']] },
        { label: 'Portaria · modo rápido', cls: 'port', items: [['port/painel', 'Painel do turno', 'grid_view'], ['port/visitantes', 'Visitantes e acesso', 'id_card'], ['port/visitante:1', 'Detalhe do visitante', 'badge'], ['port/nova-visita', 'Nova visita', 'person_add'], ['port/encomendas', 'Encomendas', 'inventory_2'], ['port/nova-encomenda', 'Registrar encomenda', 'add_a_photo'], ['port/reservas', 'Reservas de hoje', 'event'], ['port/livro', 'Livro da portaria', 'history'], ['port/gas', 'Leitura de gás', 'water_drop'], ['port/mais', 'Mais (portaria)', 'menu']] },
        { label: 'App do morador · completo', cls: 'app', items: [['app/inicio', 'Início', 'home'], ['app/reservas', 'Reservas', 'calendar_month'], ['app/nova-reserva', 'Nova reserva', 'event_available'], ['app/visitantes', 'Meus visitantes', 'id_card'], ['app/encomendas', 'Encomendas', 'inventory_2'], ['app/boletos', 'Boletos e pagamentos', 'account_balance_wallet'], ['app/avisos', 'Avisos', 'campaign'], ['app/mais', 'Mais', 'menu']] }
    ];
    function buildIndex() {
        $('#pindex').innerHTML = INDEX.map(g => `<div class="pgroup ${g.cls}"><span>${esc(g.label)}</span>${g.items.map(([id, l, i]) => `<button data-idx="${id}">${ic(i)}${esc(l)}</button>`).join('')}</div>`).join('');
    }
    function updateIndex() {
        const id = cur().id;
        $$('#pindex button').forEach(b => { const bid = b.dataset.idx.split(':')[0]; b.classList.toggle('on', bid === id); });
    }
    function gotoIndex(raw) {
        const [id, arg] = raw.split(':');
        const def = S[id]; if (!def) return;
        if (def.bare) { state.stack = [{ id, params: {} }]; render('none'); return; }
        // monta a pilha para a tela ter um "voltar" coerente
        const mode = def.mode;
        const root = mode === 'port' ? 'port/painel' : mode === 'app' ? 'app/inicio' : null;
        const params = arg ? (id.includes('visitante') ? { id: +arg } : id.includes('encomenda') ? { id: +arg } : {}) : (id === 'app/nova-reserva' ? { area: 'churrasqueiras' } : {});
        if (root && id !== root) state.stack = [{ id: root, params: {} }, { id, params }];
        else state.stack = [{ id, params }];
        state.rsv = null;
        render('none');
    }

    /* ---- notas (direita) ---- */
    function renderNotes(def) {
        const n = def.notes;
        if (!n) { $('#pnotes').innerHTML = ''; $('#pmini').innerHTML = ''; return; }
        const bullets = n.bullets ? `<div class="pnote"><ul>${n.bullets.map(b => `<li>${b}</li>`).join('')}</ul></div>` : '';
        const tip = n.tip ? `<div class="ptip">${ic('touch_app')}<span>${n.tip}</span></div>` : '';
        $('#pnotes').innerHTML = `<div class="pcrumb ${n.port ? 'port' : ''}">${esc(n.crumb || '')}</div><h2>${esc(n.title || '')}</h2><p>${n.text || ''}</p>${bullets}${tip}`;
        $('#pmini').innerHTML = `<b>${esc(n.title || '')}</b><p>${n.text || ''}</p>`;
    }

    /* ============================== eventos ============================== */
    appEl.addEventListener('click', e => {
        const go_ = e.target.closest('[data-go]');
        const root_ = e.target.closest('[data-root]');
        const back_ = e.target.closest('[data-back]');
        const act_ = e.target.closest('[data-act]');
        const close_ = e.target.closest('[data-close]');
        if (close_) { closeOvl(); return; }
        if (back_) { back(); return; }
        if (root_) { goRoot(root_.dataset.root); return; }
        if (go_) { let p = {}; try { p = go_.dataset.p ? JSON.parse(go_.dataset.p) : {}; } catch (_) {} navTo(go_.dataset.go, p); return; }
        if (act_) { const fn = actions[act_.dataset.act]; if (fn) fn(act_); return; }
        // tap para avançar no splash
        const d = S[cur().id]; if (d && d.tap && !e.target.closest('button,a,input')) d.tap();
    });
    ovl.addEventListener('click', e => {
        const act_ = e.target.closest('[data-act]'); const close_ = e.target.closest('[data-close]');
        if (act_) { const fn = actions[act_.dataset.act]; if (fn) fn(act_); return; }
        if (close_) closeOvl();
    });

    // índice
    $('#pindex').addEventListener('click', e => { const b = e.target.closest('button'); if (b) { gotoIndex(b.dataset.idx); $('#stage').classList.remove('menu'); } });
    // tema
    $$('#ptheme button').forEach(b => b.addEventListener('click', () => setTheme(b.dataset.v)));
    $('#preset').addEventListener('click', () => { state.mode = null; appEl.classList.remove('port'); state.stack = [{ id: 'splash', params: {} }]; render('none'); });
    $('#pfab').addEventListener('click', () => $('#stage').classList.add('menu'));
    $('#pclose').addEventListener('click', () => $('#stage').classList.remove('menu'));
    $('#stage').addEventListener('click', e => { if (e.target === $('#stage')) $('#stage').classList.remove('menu'); });
    matchMedia('(prefers-color-scheme: dark)').addEventListener('change', applyAppTheme);

    /* ============================== fit (escala do aparelho) ============================== */
    function fit() {
        const wrap = $('#dwrap'); if (!wrap || innerWidth <= 760) { document.documentElement.style.setProperty('--k', 1); return; }
        const center = $('.pcenter'); const availH = center.clientHeight - 40, availW = center.clientWidth - 40;
        const k = Math.min(availH / 884, availW / 420, 1);
        document.documentElement.style.setProperty('--k', k.toFixed(3));
    }
    addEventListener('resize', fit);

    /* ============================== start ============================== */
    buildIndex();
    setTheme('system');
    state.stack = [{ id: 'splash', params: {} }];
    render('none');
    fit();
})();
