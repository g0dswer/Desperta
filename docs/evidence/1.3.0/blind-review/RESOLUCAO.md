# Tratamento dos apontamentos

Os relatórios são preservados como escritos pelos revisores e correspondem às respectivas rodadas, não à compilação posterior. A revisão foi feita sem acesso ao código ou às explicações das correções. Medidas descritas como aproximadas são estimativas visuais.

| Apontamento das rodadas | Correção |
|---|---|
| Retrofuturista: conteúdo baixo e modelo cortado | Recuo superior, altura do cabeçalho, espaçamento e rodapé ajustados; modelo novamente inteiro |
| Retrofuturista: molduras com emendas, linhas longas, emblemas e toggles deslocados | Cantos da moldura recompostos, recuos das linhas ajustados, emblemas e switches reposicionados |
| Retrofuturista: relógio estreito e zero/3 divergentes | Fonte numérica derivada com contornos arredondados, sete diagonal, dois-pontos circulares e dimensão de glifos ajustada |
| Retrofuturista: sinal de mais e separador | Rótulo + Alarme mantido e separador da contagem colorido em laranja |
| Anos 90: relógio vazio/cinza e moldura plana | Desenho do relógio independente da rolagem interna de TextView; fundo azul-marinho com moldura rebaixada em dois níveis |
| Anos 90: cabeçalho, ícones, texto e toggles | Espaçamentos e proporções ajustados, tipografia compactada, controles e ação principal reposicionados |
| Matrix: mostrador com números muito juntos e sete vertical | Desenho contínuo, zero cortado, sete diagonal e espaçamento entre algarismos |
| Matrix: linha interna extra, brilho/cor, engrenagem e modelo deslocados | Linha removida, preenchimento escurecido, borda suavizada e elementos reposicionados |
| Terminal: contorno duplo sem cruzes | Um único perímetro com cruzes nos quatro cantos |
| Terminal: painel alto, divisores curtos e faixas atrás do texto | Painel reduzido; divisores ajustados; textura aplicada aos glifos, sem retângulos escuros atrás dos textos |
| Terminal: modelo e ação final | Larguras, altura, fonte e rodapé ajustados |

A afirmação do relatório Anos 90 sobre uma faixa teal entre cabeçalho e painel não foi adotada: a imagem original mostra uma área cinza da janela externa nesse intervalo. Barras e cantos físicos do sistema Android não são desenhados como conteúdo falso do aplicativo.

As capturas finais permitem inspeção direta. Não há certificação de igualdade pixel a pixel: rasterização, conteúdo dinâmico, textura/relevo e controles nativos ainda podem diferir das imagens geradas. O relatório funcional é independente da auditoria visual.

A última revisão ainda encontrou quebra de linha no menu Terminal. A versão final força o menu a uma linha com ajuste de tamanho do texto. A inspeção das telas internas também encontrou o fundo de janela legado creme na tela Matrix e uma placa de feedback vazia no leitor: o fundo da janela agora acompanha a identidade e o feedback só aparece quando existe mensagem. A posição do cabeçalho e as dimensões do mostrador Anos 90 receberam mais um ajuste após esses relatórios.
