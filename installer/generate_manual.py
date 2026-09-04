from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import (
    Image,
    KeepTogether,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "Manual-do-Usuario-Judicial-Pipeline.pdf"
ASSETS = ROOT / "output" / "pdf" / "assets"
ICON = ROOT / "installer" / "assets" / "judicial-pipeline-icon.png"

NAVY = colors.HexColor("#071326")
PANEL = colors.HexColor("#121F35")
BLUE = colors.HexColor("#3B82F6")
GREEN = colors.HexColor("#16A36A")
GOLD = colors.HexColor("#E5A93D")
RED = colors.HexColor("#C73545")
INK = colors.HexColor("#172033")
MUTED = colors.HexColor("#56657A")
LIGHT = colors.HexColor("#F3F6FA")
BORDER = colors.HexColor("#D7DFEA")


def build_styles():
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "ManualTitle",
            parent=base["Title"],
            fontName="Helvetica-Bold",
            fontSize=27,
            leading=32,
            textColor=colors.white,
            alignment=TA_CENTER,
            spaceAfter=12,
        ),
        "subtitle": ParagraphStyle(
            "ManualSubtitle",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=12,
            leading=18,
            textColor=colors.HexColor("#C8D7EA"),
            alignment=TA_CENTER,
        ),
        "h1": ParagraphStyle(
            "ManualH1",
            parent=base["Heading1"],
            fontName="Helvetica-Bold",
            fontSize=20,
            leading=24,
            textColor=NAVY,
            spaceAfter=10,
        ),
        "h2": ParagraphStyle(
            "ManualH2",
            parent=base["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=13,
            leading=17,
            textColor=NAVY,
            spaceBefore=7,
            spaceAfter=5,
        ),
        "body": ParagraphStyle(
            "ManualBody",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=10.2,
            leading=15,
            textColor=INK,
            spaceAfter=7,
        ),
        "small": ParagraphStyle(
            "ManualSmall",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=8.4,
            leading=12,
            textColor=MUTED,
        ),
        "callout": ParagraphStyle(
            "ManualCallout",
            parent=base["BodyText"],
            fontName="Helvetica-Bold",
            fontSize=10.5,
            leading=15,
            textColor=NAVY,
        ),
        "step": ParagraphStyle(
            "ManualStep",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=10.5,
            leading=15,
            textColor=INK,
        ),
        "table_header": ParagraphStyle(
            "ManualTableHeader",
            parent=base["BodyText"],
            fontName="Helvetica-Bold",
            fontSize=9,
            leading=12,
            textColor=colors.white,
        ),
        "table_body": ParagraphStyle(
            "ManualTableBody",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=8.5,
            leading=12,
            textColor=INK,
        ),
    }


STYLES = build_styles()


def p(text, style="body"):
    return Paragraph(text, STYLES[style])


def callout(text, color=BLUE):
    table = Table([[p(text, "callout")]], colWidths=[16.8 * cm])
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), LIGHT),
                ("BOX", (0, 0), (-1, -1), 0.8, BORDER),
                ("LINEBEFORE", (0, 0), (0, -1), 4, color),
                ("LEFTPADDING", (0, 0), (-1, -1), 12),
                ("RIGHTPADDING", (0, 0), (-1, -1), 12),
                ("TOPPADDING", (0, 0), (-1, -1), 10),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 10),
            ]
        )
    )
    return table


def step(number, title, description):
    badge = Table(
        [[Paragraph(str(number), ParagraphStyle(
            "Badge",
            fontName="Helvetica-Bold",
            fontSize=11,
            textColor=colors.white,
            alignment=TA_CENTER,
        ))]],
        colWidths=[0.8 * cm],
        rowHeights=[0.8 * cm],
    )
    badge.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), BLUE),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("BOX", (0, 0), (-1, -1), 0, BLUE),
            ]
        )
    )
    content = p(f"<b>{title}</b><br/>{description}", "step")
    table = Table([[badge, content]], colWidths=[1.1 * cm, 15.7 * cm])
    table.setStyle(
        TableStyle(
            [
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
                ("RIGHTPADDING", (0, 0), (-1, -1), 7),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
            ]
        )
    )
    return table


def screenshot(filename, caption):
    path = ASSETS / filename
    image = Image(str(path), width=16.8 * cm, height=9.45 * cm)
    return KeepTogether([image, Spacer(1, 0.12 * cm), p(caption, "small")])


def status_table():
    rows = [
        [p("Status", "table_header"), p("Quando usar", "table_header")],
        [p("Monitorando leilão", "table_body"), p("O leilão ainda está agendado ou em andamento.", "table_body")],
        [p("Aguardando resultado", "table_body"), p("A praça encerrou, mas o resultado ainda não foi confirmado.", "table_body")],
        [p("Monitorando processo", "table_body"), p("Não houve arrematação e o processo judicial continua sendo acompanhado.", "table_body")],
        [p("Em análise", "table_body"), p("O imóvel está sendo conferido por uma pessoa.", "table_body")],
        [p("Oportunidade", "table_body"), p("Somente após aprovação humana. Nunca é marcada automaticamente.", "table_body")],
        [p("Descartado", "table_body"), p("Não atende aos critérios ou não interessa comercialmente.", "table_body")],
        [p("Encerrado", "table_body"), p("O acompanhamento terminou.", "table_body")],
    ]
    table = Table(rows, colWidths=[4.8 * cm, 12 * cm], repeatRows=1)
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), NAVY),
                ("GRID", (0, 0), (-1, -1), 0.5, BORDER),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("TOPPADDING", (0, 0), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
                ("BACKGROUND", (0, 1), (-1, -1), colors.white),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, LIGHT]),
            ]
        )
    )
    return table


def draw_page(canvas, doc):
    canvas.saveState()
    width, height = A4
    if doc.page > 1:
        canvas.setStrokeColor(BORDER)
        canvas.setLineWidth(0.5)
        canvas.line(2 * cm, 1.35 * cm, width - 2 * cm, 1.35 * cm)
        canvas.setFont("Helvetica", 8)
        canvas.setFillColor(MUTED)
        canvas.drawString(2 * cm, 0.85 * cm, "Judicial Pipeline - Manual do usuário")
        canvas.drawRightString(width - 2 * cm, 0.85 * cm, f"Página {doc.page}")
    canvas.restoreState()


def build_manual():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    document = SimpleDocTemplate(
        str(OUTPUT),
        pagesize=A4,
        rightMargin=2 * cm,
        leftMargin=2 * cm,
        topMargin=1.8 * cm,
        bottomMargin=1.7 * cm,
        title="Manual do Usuário - Judicial Pipeline",
        author="Judicial Pipeline",
        subject="Guia de instalação e uso diário",
    )

    story = []

    cover_icon = Image(str(ICON), width=4.2 * cm, height=4.2 * cm)
    cover = Table(
        [[
            Spacer(1, 1.2 * cm),
        ], [
            cover_icon,
        ], [
            p("JUDICIAL PIPELINE", "title"),
        ], [
            p("Manual do usuário", "subtitle"),
        ], [
            Spacer(1, 0.35 * cm),
        ], [
            p("Descoberta e acompanhamento de imóveis judiciais em Sorocaba e Votorantim", "subtitle"),
        ], [
            Spacer(1, 2.8 * cm),
        ], [
            p("Versão 1.3.0 - Setembro de 2026", "subtitle"),
        ]],
        colWidths=[17 * cm],
        rowHeights=[1.2 * cm, 4.4 * cm, 1.2 * cm, 0.8 * cm, 0.5 * cm, 1.4 * cm, 3 * cm, 0.8 * cm],
    )
    cover.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), NAVY),
                ("ALIGN", (0, 0), (-1, -1), "CENTER"),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("BOX", (0, 0), (-1, -1), 0, NAVY),
            ]
        )
    )
    story.extend([Spacer(1, 1.3 * cm), cover, PageBreak()])

    story.extend(
        [
            p("1. Comece por aqui", "h1"),
            callout(
                "O cliente não precisa instalar Docker manualmente, digitar comandos, editar arquivos, criar usuário ou informar senha. O instalador prepara tudo.",
                GREEN,
            ),
            Spacer(1, 0.35 * cm),
            p("Primeira instalação", "h2"),
            step(1, "Abra o instalador", "Dê dois cliques em <b>Judicial-Pipeline-Instalador.exe</b>."),
            step(2, "Autorize o Windows", "Quando aparecer a pergunta de segurança, escolha <b>Sim</b>."),
            step(3, "Aguarde", "O programa prepara os componentes necessários. Na primeira instalação, alguns downloads podem demorar vários minutos. Se a internet oscilar, o instalador tenta novamente sozinho. O reparo oficial do Windows já acompanha o instalador."),
            step(4, "Reinicie, se solicitado", "Depois da reinicialização, clique no atalho <b>Judicial Pipeline</b> na área de trabalho."),
            p("Uso diário", "h2"),
            step(1, "Clique no atalho", "Uma pequena janela mostrará que o sistema está sendo preparado."),
            step(2, "Espere o navegador abrir", "O painel abre automaticamente em <b>http://localhost:8080</b>."),
            step(3, "Conheça o sistema", "Na primeira abertura, leia a apresentação e clique em <b>Entrar no painel</b>. Para revê-la depois, use <b>Como funciona</b> no topo."),
            step(4, "Use normalmente", "Não há login ou configuração no computador do cliente."),
            Spacer(1, 0.2 * cm),
            callout(
                "Se aparecer uma tela mencionando WSL, não digite comandos. O instalador leva o reparo oficial e corrige esse componente sozinho.",
                GOLD,
            ),
            PageBreak(),
        ]
    )

    story.extend(
        [
            p("2. Painel principal", "h1"),
            p(
                "O painel reúne somente os imóveis elegíveis que passaram pela triagem. Itens duplicados, descartados ou com falha ficam registrados na Central de Auditoria.",
            ),
            p(
                "A busca automática consulta Sublime Leilões, Mega Leilões, SPY Leilões, Portal Zuk, GL Leilões, PublicJud e o DJEN/CNJ. Casas, apartamentos, galpões e terrenos podem ser acompanhados quando atendem ao escopo.",
            ),
            screenshot("painel.png", "Painel principal com indicadores, filtros e imóveis monitorados."),
            Spacer(1, 0.25 * cm),
            p("Indicadores superiores", "h2"),
            p(
                "<b>Imóveis cadastrados:</b> total acompanhado. <b>Em acompanhamento:</b> itens ainda ativos. <b>Sem lances:</b> leilões sem arrematação confirmada. <b>Oportunidades aprovadas:</b> imóveis aprovados manualmente.",
            ),
            callout("Sem lances não significa oportunidade. A oportunidade depende de análise humana.", RED),
            p("Arquivados / inativos", "h2"),
            p(
                "Quando a última praça publicada já terminou, o imóvel sai automaticamente do painel principal. Use <b>Arquivados / inativos</b> para consultar o registro, as fontes, as observações e todo o histórico preservado.",
            ),
            PageBreak(),
        ]
    )

    story.extend(
        [
            p("3. Pesquisar e abrir um imóvel", "h1"),
            p("Use os filtros na parte superior do painel para reduzir a lista:"),
            step(1, "Processo", "Digite o número completo ou parte do número do processo."),
            step(2, "Cidade e bairro", "Escolha Sorocaba, Votorantim ou um bairro específico."),
            step(3, "Status e resultado", "Localize imóveis conforme a etapa de acompanhamento."),
            step(4, "Limpar filtros", "Use o botão quando quiser voltar à lista completa."),
            p("Em cada cartão", "h2"),
            p(
                "Confira localização, processo, avaliação, valores das praças e situação. Use <b>Ver detalhes</b> para analisar. Use <b>Abrir fonte</b> para conferir o anúncio original do leiloeiro.",
            ),
            callout(
                "Os valores são apresentados conforme cada fonte. Se duas fontes divergirem, o sistema preserva ambas para conferência.",
                BLUE,
            ),
            p("O que o sistema filtra automaticamente", "h2"),
            p(
                "A descoberta considera imóveis de Sorocaba e Votorantim, elimina duplicidades por URL e processo e rejeita itens incompatíveis com o escopo. Processos de despejo não são oportunidades imobiliárias válidas para este produto.",
            ),
            PageBreak(),
        ]
    )

    story.extend(
        [
            p("4. Detalhes do imóvel", "h1"),
            p(
                "A tela de detalhes reúne localização, processo judicial, avaliação, lance mínimo, datas das praças, histórico e fontes.",
            ),
            screenshot("detalhe.png", "Início da tela de detalhes com dados do imóvel e do processo."),
            Spacer(1, 0.3 * cm),
            p("Atualizar dados agora", "h2"),
            p(
                "Esse botão consulta novamente a fonte cadastrada. Use quando precisar conferir uma mudança antes da próxima atualização automática.",
            ),
            p("Datas e valores", "h2"),
            p(
                "Confira separadamente a primeira e a segunda praça. A avaliação não é necessariamente o lance atual; observe o rótulo de cada valor.",
            ),
            PageBreak(),
        ]
    )

    story.extend(
        [
            p("5. Registrar a análise", "h1"),
            p(
                "Na lateral da tela de detalhes, escolha o status e escreva uma observação de até 2.000 caracteres. Depois clique em <b>Salvar acompanhamento</b>.",
            ),
            screenshot("detalhe-decisao.png", "Praças, status, observação e evidências preservadas por fonte."),
            Spacer(1, 0.25 * cm),
            callout(
                "Exemplos de observação: verificar matrícula, ocupação, débitos, andamento processual ou dúvida a confirmar.",
                GREEN,
            ),
            p("Significado dos status", "h2"),
            status_table(),
            PageBreak(),
        ]
    )

    story.extend(
        [
            p("6. Central de Auditoria", "h1"),
            p(
                "A Central de Auditoria é a trilha de controle do sistema. Ela explica o que aconteceu em cada busca e permite conferir decisões e falhas.",
            ),
            screenshot("auditoria.png", "Central de Auditoria com última execução e totais históricos."),
            Spacer(1, 0.25 * cm),
            p("Como interpretar", "h2"),
            p(
                "<b>Encontrados:</b> lotes retornados pelas fontes. <b>Elegíveis:</b> passaram pela triagem. <b>Importados:</b> entraram pela primeira vez. <b>Já cadastrados/atualizados:</b> foram reconhecidos e atualizados sem duplicação. <b>Descartados:</b> não atendem aos critérios. <b>Falhas:</b> uma fonte ou etapa não respondeu corretamente.",
            ),
            callout(
                "A auditoria não é a lista de oportunidades. Ela é o histórico técnico que comprova o trabalho realizado pelo sistema.",
                BLUE,
            ),
            PageBreak(),
        ]
    )

    story.extend(
        [
            p("7. Regras de uso seguro", "h1"),
            p("O sistema ajuda a organizar e acompanhar. A decisão comercial e jurídica continua sendo humana."),
            Table(
                [
                    [p("FAÇA", "table_header"), p("NÃO FAÇA", "table_header")],
                    [p("Confira endereço, processo, datas, valores e fontes.", "table_body"), p("Não trate sem lances como oportunidade automática.", "table_body")],
                    [p("Registre dúvidas e decisões nas observações.", "table_body"), p("Não apague C:\\ProgramData\\JudicialPipeline.", "table_body")],
                    [p("Mantenha o computador conectado à internet durante as consultas.", "table_body"), p("Não remova volumes ou dados no Docker Desktop.", "table_body")],
                    [p("Abra a fonte original antes de uma decisão importante.", "table_body"), p("Não use o sistema como substituto de análise jurídica, registral ou financeira.", "table_body")],
                ],
                colWidths=[8.4 * cm, 8.4 * cm],
                style=TableStyle(
                    [
                        ("BACKGROUND", (0, 0), (0, 0), GREEN),
                        ("BACKGROUND", (1, 0), (1, 0), RED),
                        ("GRID", (0, 0), (-1, -1), 0.5, BORDER),
                        ("VALIGN", (0, 0), (-1, -1), "TOP"),
                        ("LEFTPADDING", (0, 0), (-1, -1), 9),
                        ("RIGHTPADDING", (0, 0), (-1, -1), 9),
                        ("TOPPADDING", (0, 0), (-1, -1), 8),
                        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
                        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, LIGHT]),
                    ]
                ),
            ),
            Spacer(1, 0.45 * cm),
            p("Funcionamento automático", "h2"),
            p(
                "Enquanto o computador e o mecanismo local estiverem ligados, as rotinas programadas podem consultar as fontes. Quando o computador está desligado, nenhuma busca é executada. Ao ligar novamente, abra o programa pelo atalho.",
            ),
            p("Atualizações das fontes", "h2"),
            p(
                "Sites de leilão podem mudar a estrutura das páginas. Se uma fonte parar de responder, a falha aparecerá na auditoria e poderá exigir atualização técnica do programa.",
            ),
            p("Liberar memória ao terminar", "h2"),
            p(
                "Fechar a aba do navegador não encerra o mecanismo local. Se não for usar mais o sistema e quiser liberar a memória, clique com o botão direito no ícone do Docker perto do relógio do Windows e escolha <b>Quit Docker Desktop</b>. No próximo uso, o atalho Judicial Pipeline iniciará tudo novamente.",
            ),
            PageBreak(),
        ]
    )

    story.extend(
        [
            p("8. Ajuda rápida", "h1"),
            p("O painel não abriu", "h2"),
            step(1, "Aguarde", "Na primeira abertura, o preparo pode levar alguns minutos."),
            step(2, "Reinicie", "Reinicie o computador e clique novamente no atalho Judicial Pipeline."),
            step(3, "Reinstale por cima", "Execute o instalador atualizado. Os dados existentes são preservados."),
            p("O Docker mostrou wsl --update", "h2"),
            callout(
                "Não abra terminal e não digite comandos. Feche o aviso e execute o instalador atualizado; o reparo oficial já está dentro dele.",
                GOLD,
            ),
            p("Arquivos para o suporte", "h2"),
            p(
                "Se o problema continuar, envie os arquivos abaixo ao responsável técnico. Eles não devem ser publicados em redes sociais ou repositórios públicos.",
            ),
            p(
                "<b>C:\\ProgramData\\JudicialPipeline</b><br/>instalacao.log<br/>preparacao-docker.log",
            ),
            p(
                "<b>%LOCALAPPDATA%\\JudicialPipeline</b><br/>preparacao-windows.log<br/>reparo-wsl-msi.log<br/>inicializacao.log<br/>inicio.log",
            ),
            Spacer(1, 0.4 * cm),
            callout(
                "Uso normal: ligar o computador, clicar no atalho Judicial Pipeline e aguardar o navegador abrir.",
                GREEN,
            ),
        ]
    )

    document.build(story, onFirstPage=draw_page, onLaterPages=draw_page)
    print(OUTPUT)


if __name__ == "__main__":
    build_manual()
