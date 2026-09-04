from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
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
BLUE = colors.HexColor("#2D6CDF")
GREEN = colors.HexColor("#168A62")
GOLD = colors.HexColor("#D89B32")
RED = colors.HexColor("#B83A4B")
INK = colors.HexColor("#172033")
MUTED = colors.HexColor("#5A687B")
LIGHT = colors.HexColor("#F4F6F9")
LIGHT_BLUE = colors.HexColor("#EEF4FF")
BORDER = colors.HexColor("#D8DFE8")
WHITE = colors.white


def build_styles():
    base = getSampleStyleSheet()
    return {
        "cover_kicker": ParagraphStyle(
            "CoverKicker",
            parent=base["Normal"],
            fontName="Helvetica-Bold",
            fontSize=9,
            leading=12,
            tracking=1.4,
            textColor=GOLD,
            alignment=TA_CENTER,
        ),
        "cover_title": ParagraphStyle(
            "CoverTitle",
            parent=base["Title"],
            fontName="Helvetica-Bold",
            fontSize=28,
            leading=33,
            textColor=WHITE,
            alignment=TA_CENTER,
        ),
        "cover_subtitle": ParagraphStyle(
            "CoverSubtitle",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=11.5,
            leading=17,
            textColor=colors.HexColor("#CAD5E5"),
            alignment=TA_CENTER,
        ),
        "section_number": ParagraphStyle(
            "SectionNumber",
            parent=base["Normal"],
            fontName="Helvetica-Bold",
            fontSize=8.5,
            leading=11,
            tracking=1.2,
            textColor=BLUE,
            spaceAfter=4,
        ),
        "h1": ParagraphStyle(
            "ManualH1",
            parent=base["Heading1"],
            fontName="Helvetica-Bold",
            fontSize=20,
            leading=24,
            textColor=NAVY,
            spaceAfter=7,
        ),
        "h2": ParagraphStyle(
            "ManualH2",
            parent=base["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=12.5,
            leading=16,
            textColor=NAVY,
            spaceBefore=7,
            spaceAfter=4,
        ),
        "lead": ParagraphStyle(
            "ManualLead",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=11,
            leading=16.5,
            textColor=MUTED,
            spaceAfter=12,
        ),
        "body": ParagraphStyle(
            "ManualBody",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=9.8,
            leading=14.5,
            textColor=INK,
            spaceAfter=6,
        ),
        "small": ParagraphStyle(
            "ManualSmall",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=8.2,
            leading=11.5,
            textColor=MUTED,
        ),
        "caption": ParagraphStyle(
            "ManualCaption",
            parent=base["BodyText"],
            fontName="Helvetica-Oblique",
            fontSize=8,
            leading=11,
            textColor=MUTED,
            spaceBefore=3,
        ),
        "card_title": ParagraphStyle(
            "CardTitle",
            parent=base["BodyText"],
            fontName="Helvetica-Bold",
            fontSize=10,
            leading=13,
            textColor=NAVY,
            spaceAfter=2,
        ),
        "card_body": ParagraphStyle(
            "CardBody",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=9,
            leading=13,
            textColor=INK,
        ),
        "note": ParagraphStyle(
            "ManualNote",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=9.5,
            leading=14,
            textColor=INK,
        ),
        "table_header": ParagraphStyle(
            "TableHeader",
            parent=base["BodyText"],
            fontName="Helvetica-Bold",
            fontSize=8.5,
            leading=11.5,
            textColor=WHITE,
        ),
        "table_body": ParagraphStyle(
            "TableBody",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=8.2,
            leading=11.5,
            textColor=INK,
        ),
    }


STYLES = build_styles()


def p(text, style="body"):
    return Paragraph(text, STYLES[style])


def section(number, title, introduction):
    return [
        p(f"SEÇÃO {number}", "section_number"),
        p(title, "h1"),
        p(introduction, "lead"),
    ]


def note(text, color=BLUE, background=LIGHT_BLUE):
    box = Table([[p(text, "note")]], colWidths=[16.8 * cm])
    box.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), background),
                ("BOX", (0, 0), (-1, -1), 0.6, BORDER),
                ("LINEBEFORE", (0, 0), (0, -1), 3.5, color),
                ("LEFTPADDING", (0, 0), (-1, -1), 11),
                ("RIGHTPADDING", (0, 0), (-1, -1), 11),
                ("TOPPADDING", (0, 0), (-1, -1), 9),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 9),
            ]
        )
    )
    return box


def routine_card(number, title, description):
    badge_style = ParagraphStyle(
        f"RoutineBadge{number}",
        fontName="Helvetica-Bold",
        fontSize=11,
        leading=14,
        textColor=WHITE,
        alignment=TA_CENTER,
    )
    badge = Table(
        [[Paragraph(str(number), badge_style)]],
        colWidths=[0.72 * cm],
        rowHeights=[0.72 * cm],
    )
    badge.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), BLUE),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
            ]
        )
    )
    body = [p(title, "card_title"), p(description, "card_body")]
    card = Table([[badge, body]], colWidths=[1.05 * cm, 15.35 * cm])
    card.setStyle(
        TableStyle(
            [
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
            ]
        )
    )
    return card


def two_cards(cards):
    cells = []
    for title, text in cards:
        cells.append([p(title, "card_title"), p(text, "card_body")])
    table = Table([cells], colWidths=[8.1 * cm, 8.1 * cm], hAlign="LEFT")
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), LIGHT),
                ("BOX", (0, 0), (-1, -1), 0.6, BORDER),
                ("INNERGRID", (0, 0), (-1, -1), 0.6, BORDER),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 10),
                ("RIGHTPADDING", (0, 0), (-1, -1), 10),
                ("TOPPADDING", (0, 0), (-1, -1), 9),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 9),
            ]
        )
    )
    return table


def screenshot(filename, caption, height=8.7):
    path = ASSETS / filename
    image = Image(str(path), width=15.47 * cm, height=height * cm)
    frame = Table([[image]], colWidths=[16.0 * cm])
    frame.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), WHITE),
                ("BOX", (0, 0), (-1, -1), 0.7, BORDER),
                ("LEFTPADDING", (0, 0), (-1, -1), 7),
                ("RIGHTPADDING", (0, 0), (-1, -1), 7),
                ("TOPPADDING", (0, 0), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
            ]
        )
    )
    return KeepTogether([frame, p(caption, "caption")])


def status_table():
    rows = [
        [p("Status", "table_header"), p("Uso recomendado", "table_header")],
        [p("Monitorando leilão", "table_body"), p("A praça ainda está agendada ou acontecendo.", "table_body")],
        [p("Aguardando resultado", "table_body"), p("A praça terminou e a fonte ainda não confirmou o desfecho.", "table_body")],
        [p("Monitorando processo", "table_body"), p("Não houve arrematação e o processo continuará sendo acompanhado.", "table_body")],
        [p("Em análise", "table_body"), p("Matrícula, ocupação, débitos ou viabilidade estão sendo conferidos.", "table_body")],
        [p("Oportunidade", "table_body"), p("O imóvel foi aprovado por uma pessoa após a análise.", "table_body")],
        [p("Descartado", "table_body"), p("O caso não atende aos critérios ou deixou de interessar.", "table_body")],
        [p("Encerrado", "table_body"), p("O acompanhamento chegou ao fim.", "table_body")],
    ]
    table = Table(rows, colWidths=[4.7 * cm, 12.1 * cm], repeatRows=1)
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), NAVY),
                ("GRID", (0, 0), (-1, -1), 0.45, BORDER),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, LIGHT]),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("TOPPADDING", (0, 0), (-1, -1), 6.5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6.5),
            ]
        )
    )
    return table


def situations_table():
    rows = [
        [p("Situação", "table_header"), p("O que fazer", "table_header")],
        [p("O imóvel apareceu como sem lances", "table_body"), p("Confira a fonte e aguarde o resultado. Isso, sozinho, não transforma o imóvel em oportunidade.", "table_body")],
        [p("Os valores das fontes são diferentes", "table_body"), p("Não escolha um valor por aproximação. Abra as fontes e registre a divergência na observação.", "table_body")],
        [p("O imóvel sumiu do painel", "table_body"), p("Procure em Arquivados / inativos. O sistema retira da tela inicial os leilões cuja última praça terminou.", "table_body")],
        [p("Preciso conferir uma mudança agora", "table_body"), p("Abra o imóvel e use Atualizar dados agora. A atualização automática continuará funcionando.", "table_body")],
        [p("Uma fonte falhou", "table_body"), p("Veja a execução na Central de Auditoria. As outras fontes continuam sendo processadas.", "table_body")],
    ]
    table = Table(rows, colWidths=[5.2 * cm, 11.6 * cm], repeatRows=1)
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), NAVY),
                ("GRID", (0, 0), (-1, -1), 0.45, BORDER),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, LIGHT]),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("TOPPADDING", (0, 0), (-1, -1), 8),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
            ]
        )
    )
    return table


def draw_page(canvas, doc):
    canvas.saveState()
    width, height = A4
    if doc.page == 1:
        canvas.setFillColor(NAVY)
        canvas.rect(0, 0, width, height, fill=1, stroke=0)
    else:
        canvas.setStrokeColor(BORDER)
        canvas.setLineWidth(0.5)
        canvas.line(2 * cm, 1.28 * cm, width - 2 * cm, 1.28 * cm)
        canvas.setFont("Helvetica", 7.8)
        canvas.setFillColor(MUTED)
        canvas.drawString(2 * cm, 0.82 * cm, "Judicial Pipeline  |  Guia de uso")
        canvas.drawRightString(width - 2 * cm, 0.82 * cm, str(doc.page))
    canvas.restoreState()


def build_manual():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    document = SimpleDocTemplate(
        str(OUTPUT),
        pagesize=A4,
        rightMargin=2 * cm,
        leftMargin=2 * cm,
        topMargin=1.65 * cm,
        bottomMargin=1.65 * cm,
        title="Guia de uso - Judicial Pipeline",
        author="Judicial Pipeline",
        subject="Orientações para o uso diário do sistema",
    )

    story = []

    cover_icon = Image(str(ICON), width=4.0 * cm, height=4.0 * cm)
    cover = Table(
        [
            [Spacer(1, 1.0 * cm)],
            [cover_icon],
            [p("GUIA DE USO", "cover_kicker")],
            [p("JUDICIAL PIPELINE", "cover_title")],
            [p("Consulta e acompanhamento de imóveis judiciais", "cover_subtitle")],
            [Spacer(1, 2.2 * cm)],
            [p("Sorocaba e Votorantim", "cover_subtitle")],
            [p("Versão 1.3.0  |  Setembro de 2026", "cover_subtitle")],
        ],
        colWidths=[17 * cm],
        rowHeights=[1.2 * cm, 4.2 * cm, 0.7 * cm, 1.3 * cm, 1.0 * cm, 11.8 * cm, 0.65 * cm, 0.65 * cm],
    )
    cover.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), NAVY),
                ("ALIGN", (0, 0), (-1, -1), "CENTER"),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
            ]
        )
    )
    story.extend([Spacer(1, 1.35 * cm), cover, PageBreak()])

    story.extend(section("01", "O essencial", "Este guia começa com o programa já instalado. Para o uso normal, basta abrir o atalho Judicial Pipeline e aguardar o painel aparecer no navegador."))
    story.extend(
        [
            p("A rotina em quatro passos", "h2"),
            routine_card(1, "Abra o painel", "Clique no atalho Judicial Pipeline. Não é necessário abrir terminal nem configurar o banco."),
            routine_card(2, "Localize o imóvel", "Use a busca pelo processo ou os filtros de cidade, bairro, status e resultado."),
            routine_card(3, "Confira os detalhes", "Abra o imóvel, leia as datas, valores e fontes e consulte o anúncio original quando necessário."),
            routine_card(4, "Registre a decisão", "Escolha o status correto, escreva a observação e salve. O histórico ficará preservado."),
            Spacer(1, 0.25 * cm),
            note("Na primeira abertura, o sistema apresenta um resumo rápido. Depois, essa apresentação pode ser revista pelo botão <b>Como funciona</b>.", GREEN, colors.HexColor("#EEF8F4")),
            Spacer(1, 0.35 * cm),
            p("O que o sistema faz", "h2"),
            p("O Judicial Pipeline procura imóveis em fontes integradas, aplica a triagem de Sorocaba e Votorantim, evita duplicidades e organiza os dados para acompanhamento. A aprovação de uma oportunidade é sempre feita por uma pessoa."),
            p("As consultas incluem Sublime Leilões, Mega Leilões, SPY Leilões, Portal Zuk, GL Leilões, PublicJud e DJEN/CNJ. O DataJud complementa os dados do processo judicial."),
            note("Leilão sem lances não significa oportunidade aprovada.", RED, colors.HexColor("#FFF1F3")),
            PageBreak(),
        ]
    )

    story.extend(section("02", "Painel principal", "A tela inicial mostra os imóveis ativos e os números que ajudam a entender o volume de trabalho. Ela não exibe duplicados, descartados nem leilões já arquivados."))
    story.extend(
        [
            screenshot("painel.png", "Painel principal: indicadores, filtros e lista de imóveis monitorados.", 8.25),
            Spacer(1, 0.2 * cm),
            two_cards(
                [
                    ("Indicadores", "Mostram imóveis ativos, itens em acompanhamento, casos sem lances e oportunidades aprovadas."),
                    ("Filtros", "Podem ser combinados. Por exemplo: Votorantim + Em análise. Use Limpar filtros para voltar à lista completa."),
                ]
            ),
            Spacer(1, 0.25 * cm),
            p("Como ler o cartão do imóvel", "h2"),
            p("Cada cartão apresenta localização, processo, avaliação, valores do leilão e situação atual. <b>Ver detalhes</b> abre a ficha completa. <b>Abrir fonte</b> leva ao anúncio original."),
            note("A avaliação e o lance mínimo são campos diferentes. Antes de decidir, confira o rótulo do valor e a praça correspondente.", GOLD, colors.HexColor("#FFF8E9")),
            PageBreak(),
        ]
    )

    story.extend(section("03", "Analisar um imóvel", "A ficha do imóvel reúne o que foi encontrado nas fontes e no processo. Use essa tela como ponto de partida para a conferência, não como parecer jurídico ou financeiro."))
    story.extend(
        [
            screenshot("detalhe.png", "Ficha do imóvel com localização, processo, situação e valores.", 8.2),
            Spacer(1, 0.2 * cm),
            p("Ordem prática de conferência", "h2"),
            routine_card(1, "Localização", "Confirme cidade, endereço, bairro e tipo do imóvel."),
            routine_card(2, "Processo", "Confira o número e a vara. Quando necessário, consulte os andamentos apresentados na ficha."),
            routine_card(3, "Praças e valores", "Leia separadamente primeira praça, segunda praça, avaliação e lance atual ou mínimo."),
            routine_card(4, "Fontes", "Abra o anúncio original e compare as evidências. Se houver divergência, registre-a; não escolha um dado por aproximação."),
            note("O botão <b>Atualizar dados agora</b> faz uma nova consulta da fonte. Ele pode ser usado sem interromper a atualização automática.", BLUE),
            PageBreak(),
        ]
    )

    story.extend(section("04", "Registrar o acompanhamento", "A análise ganha valor quando o próximo passo fica registrado. Escreva de forma curta, indicando o que foi conferido, o que falta conferir e quem tomou a decisão."))
    story.extend(
        [
            screenshot("detalhe-decisao.png", "Área de acompanhamento: status, observação, praças e fontes.", 7.75),
            Spacer(1, 0.15 * cm),
            two_cards(
                [
                    ("Boa observação", "Ex.: Matrícula conferida. Falta confirmar ocupação e débitos condominiais. Rever após o resultado da segunda praça."),
                    ("Evite", "Anotações vagas como analisar depois ou parece bom. Elas não explicam o que já foi feito nem o próximo passo."),
                ]
            ),
            Spacer(1, 0.2 * cm),
            p("Escolha do status", "h2"),
            p("Altere o status somente quando houver motivo claro. Depois de escrever a observação, clique em <b>Salvar acompanhamento</b>."),
            note("O campo aceita até 2.000 caracteres. Informações sensíveis ou documentos pessoais não devem ser copiados para a observação.", GOLD, colors.HexColor("#FFF8E9")),
            PageBreak(),
        ]
    )

    story.extend(section("05", "Referência de status", "O status indica em que ponto o imóvel está. Ele não substitui a observação: use os dois juntos para que outra pessoa consiga entender o caso."))
    story.extend(
        [
            status_table(),
            Spacer(1, 0.4 * cm),
            note("<b>Oportunidade</b> é uma decisão manual. O sistema nunca deve promovê-la apenas por desconto, ausência de lances ou encerramento do leilão.", RED, colors.HexColor("#FFF1F3")),
            Spacer(1, 0.4 * cm),
            p("Quando o leilão termina", "h2"),
            p("Depois da última praça publicada, o imóvel deixa o painel principal e passa para <b>Arquivados / inativos</b>. O registro não é apagado: fontes, observações e histórico continuam disponíveis."),
            p("Se o resultado ainda não estiver confirmado, use <b>Aguardando resultado</b>. Se não houve arrematação e o processo continuar relevante, use <b>Monitorando processo</b>."),
            PageBreak(),
        ]
    )

    story.extend(section("06", "Auditoria e itens arquivados", "A Central de Auditoria mostra o trabalho feito em cada busca: o que foi encontrado, importado, atualizado, descartado ou não pôde ser consultado."))
    story.extend(
        [
            screenshot("auditoria.png", "Central de Auditoria: última execução, totais e histórico por lote.", 8.0),
            Spacer(1, 0.2 * cm),
            p("Leitura dos números", "h2"),
            two_cards(
                [
                    ("Encontrados e elegíveis", "Encontrados vieram das fontes. Elegíveis passaram pela triagem de imóvel e região."),
                    ("Importados e atualizados", "Importados são novos. Atualizados já existiam e foram reconhecidos sem gerar duplicidade."),
                ]
            ),
            Spacer(1, 0.2 * cm),
            two_cards(
                [
                    ("Descartados", "Não atenderam aos critérios. O motivo fica registrado para conferência."),
                    ("Falhas", "Uma fonte ou etapa não respondeu corretamente. As demais fontes continuam sendo processadas."),
                ]
            ),
            Spacer(1, 0.3 * cm),
            note("A auditoria é o histórico técnico da coleta. A lista de oportunidades continua no painel e depende da aprovação humana.", BLUE),
            PageBreak(),
        ]
    )

    story.extend(section("07", "Situações do dia a dia", "Estas são as dúvidas mais comuns durante o acompanhamento. Em caso de dúvida sobre um dado, a fonte original e o processo devem prevalecer sobre suposições."))
    story.extend(
        [
            situations_table(),
            Spacer(1, 0.4 * cm),
            p("Antes de encerrar uma análise", "h2"),
            p("Confirme endereço, processo, datas das praças, valor utilizado, situação da ocupação e eventuais débitos. Registre a conclusão e a fonte que sustentou a decisão."),
            note("Processos de despejo tratam de locação e não entram como oportunidade imobiliária deste produto.", RED, colors.HexColor("#FFF1F3")),
            Spacer(1, 0.3 * cm),
            p("Ao terminar o uso", "h2"),
            p("Fechar a aba do navegador não desliga o mecanismo local. Se quiser liberar memória, clique com o botão direito no ícone do Docker perto do relógio e escolha <b>Quit Docker Desktop</b>. No próximo uso, abra novamente pelo atalho Judicial Pipeline."),
            PageBreak(),
        ]
    )

    story.extend(section("08", "Se o painel não abrir", "Na maioria das vezes, basta aguardar alguns minutos: o banco e a aplicação podem estar iniciando. Não digite comandos em janelas do Windows."))
    story.extend(
        [
            routine_card(1, "Espere um pouco", "A primeira abertura do dia pode ser mais lenta, principalmente depois de uma atualização do Windows."),
            routine_card(2, "Tente novamente", "Feche a janela de aviso, reinicie o computador e abra o atalho Judicial Pipeline."),
            routine_card(3, "Reinstale por cima", "Se o problema continuar, execute o instalador mais recente. A reinstalação foi preparada para preservar os dados existentes."),
            Spacer(1, 0.35 * cm),
            note("Se aparecer uma mensagem sobre WSL, não execute <b>wsl --update</b> manualmente. Use o instalador atualizado, que já contém o reparo oficial.", GOLD, colors.HexColor("#FFF8E9")),
            Spacer(1, 0.4 * cm),
            p("O que enviar ao suporte", "h2"),
            p("Envie uma captura da mensagem e, se existirem, os arquivos <b>instalacao.log</b>, <b>preparacao-docker.log</b>, <b>preparacao-windows.log</b> e <b>inicializacao.log</b>. Eles ficam nas pastas de dados do Judicial Pipeline."),
            p("Esses registros servem para diagnóstico. Não publique os arquivos em redes sociais ou repositórios abertos."),
            Spacer(1, 0.45 * cm),
            note("Rotina normal: clique no atalho Judicial Pipeline, aguarde o navegador abrir e trabalhe pelo painel.", GREEN, colors.HexColor("#EEF8F4")),
        ]
    )

    document.build(story, onFirstPage=draw_page, onLaterPages=draw_page)
    print(OUTPUT)


if __name__ == "__main__":
    build_manual()
