# Auditoria visual cega — Retro e Nineties

Comparação feita somente olhando as quatro imagens fornecidas, em resolução original. Não foi consultado código nem histórico. As imagens têm o mesmo tamanho por par: `retro` 853×1844 px e `nineties` 862×1825 px. As coordenadas abaixo são aproximadas, com origem no canto superior esquerdo de cada imagem.

Esta auditoria usa a versão das capturas que foi aberta na primeira inspeção. As capturas foram posteriormente substituídas no diretório de origem; essa substituição não foi reaberta nem misturada às observações abaixo.

## Retro

### Diferenças de maior impacto

- **Enquadramento e deslocamento vertical (alta):** a referência tem a moldura arredondada com cantos pretos visíveis desde os quatro cantos da imagem e o primeiro painel começa aproximadamente em `y=91`. A captura mostra a faixa de status Android clara no alto, o primeiro painel começa em torno de `y=120` e aparece a barra de gesto Android no rodapé. Ignorando o conteúdo variável do status, o conteúdo da identidade está deslocado para baixo cerca de 20–30 px e perde os cantos pretos da moldura da referência.

- **Painel do herói (alta):** a largura e o alinhamento horizontal do logo, engrenagem e arcos são próximos, mas na captura todo o conjunto começa mais abaixo (`x≈17–836, y≈120`) que na referência (`x≈18–835, y≈91`). O painel capturado termina perto de `y≈575`; o da referência perto de `y≈568`. Isso deixa a captura com menos altura útil e comprime o espaçamento vertical entre logo, arcos, sol e faixas inferiores. As cores creme, azul-marinho e laranja e os ornamentos do cenário permanecem visualmente próximos.

- **Painel “PRÓXIMO ALARME” (alta):** a captura começa cerca de 20–30 px abaixo (`y≈598`, contra `y≈573` na referência). O título laranja está mais estreito e menor na captura (aprox. `x=280–575`, contra `x=267–591`), embora continue centralizado. As linhas ornamentais horizontais são muito mais longas: na captura chegam aproximadamente de `x=50` a `250` e de `x=603` a `803`; na referência ficam aproximadamente entre `x=87–236` e `x=622–767`. Os parafusos e a linha inferior acompanham o deslocamento vertical.

- **Relógio grande (alta):** o `07:30` da captura ocupa uma caixa menor e mais baixa, aproximadamente `x=105–749, y=718–870`, contra `x=84–770, y=686–854` na referência. Os algarismos e os dois pontos laranja estão visivelmente menores/mais estreitos. A linha fina abaixo também se estende quase até as bordas na captura (`x≈50–803`), enquanto na referência fica recuada (`x≈86–768`), além de estar cerca de 30 px mais abaixo.

- **Cartões de alarme (alta):** os cartões começam cerca de 20 px mais abaixo na captura. O círculo/ícone e a grade de pontos do lado esquerdo estão deslocados para a esquerda em torno de 15–20 px em relação à referência, enquanto o bloco textual fica apenas alguns pixels mais à esquerda e ligeiramente menor. O toggle laranja da primeira linha é mais estreito e deslocado para a direita: aproximadamente `x=610–721` na captura, contra `x=580–699` na referência. O divisor vertical e os três pontos continuam próximos da borda direita, por isso o espaço entre toggle e divisor fica diferente. O mesmo padrão aparece no toggle desligado da segunda linha.

- **Linha “Usar um modelo” (alta):** a captura usa quase toda a largura (`x≈17–836`), enquanto a referência mantém recuo maior (`x≈24–830`). O ícone/texto ficam deslocados para a esquerda e o espaçamento até as laterais muda; a seta fica mais à direita na captura. A linha inferior arredondada da captura fica parcialmente encoberta pelo botão laranja seguinte. Na referência a linha é inteira e existe um intervalo visível antes do botão.

- **Botão “+ Alarme” e corte inferior (alta):** na referência o botão ocupa aproximadamente `x=26–829, y=1645–1810`, com altura perto de 160 px. Na captura ele é mais largo (`x≈20–833`) e muito mais baixo, terminando perto de `y≈1768`; a altura fica perto de 125 px. O círculo do “+”, o texto branco e as ranhuras da direita ficam verticalmente comprimidos e o texto começa alguns pixels mais à esquerda. A captura termina com área de navegação Android e não mostra a moldura preta arredondada inferior da referência.

### Outras diferenças observáveis

- A captura coloca o subtítulo do próximo alarme cerca de 25–35 px mais abaixo. O texto exibido também tem valor diferente (`12 h 35 min` na captura contra `8 h 12 min` na referência); tratei o valor como conteúdo, mas a posição e a cor do separador são comparáveis: a referência usa um ponto laranja evidente, enquanto a captura mostra um separador pálido/azulado.

- O logo `Desperta`, a engrenagem, os arcos, os planetas, o sol e as faixas do herói não mostram troca de desenho ou de paleta; a discrepância dominante é posição vertical e a geometria dos painéis abaixo.

- Os parafusos, as bordas duplas e os cantos arredondados permanecem do mesmo tipo visual, mas a captura altera os recuos das linhas e a moldura do rodapé. Não há uma mudança global forte de cor; a paleta creme/azul/laranja está próxima.

## Nineties

### Diferenças de maior impacto

- **Enquadramento e deslocamento vertical (alta):** a referência começa o cabeçalho aproximadamente em `x=10–852, y=77`; a captura começa em `x=16–846, y=120`. Portanto o cabeçalho capturado está cerca de 40 px mais baixo e é aproximadamente 12–14 px mais estreito. A referência termina em fundo teal sem barra de gesto; a captura inclui o status Android no alto e a barra de gesto branca no rodapé. O conteúdo do status foi desconsiderado, mas o recorte/inset do layout é uma diferença visível.

- **Cabeçalho (alta):** o fundo azul e o título pixelado permanecem reconhecíveis, porém o relógio despertador à esquerda da captura é menor e fica muito mais próximo da borda esquerda do cabeçalho; na referência ele tem mais margem esquerda e ocupa uma caixa aproximadamente 60×75 px, enquanto na captura aparenta cerca de 50×68 px. O botão de engrenagem fica na mesma região direita, mas todo o cabeçalho está deslocado para baixo e com menos margem lateral.

- **Painel principal (alta):** a referência ocupa aproximadamente `x=40–822, y=265–1020`; a captura ocupa `x=46–816, y=294–1025`. A captura é mais estreita, começa cerca de 30 px abaixo e termina quase na mesma altura, portanto fica sensivelmente mais baixa. O recuo do título e da linha branca também muda: o título passa de `x≈68` para `x≈79`, e a linha fica mais curta na captura (aprox. `x=86–777`, contra `x=69–794`).

- **Ilustração do nascer do sol (média/alta):** na referência a imagem fica aproximadamente em `x=338–522, y=393–553`; na captura fica perto de `x=342–517, y=423–573`. Ela está deslocada para baixo cerca de 30 px e um pouco menor, sobretudo na largura.

- **Display LCD central (alta):** a moldura da captura é mais larga e mais alta. A referência fica aproximadamente em `x=157–706, y=591–856`; a captura em `x=148–713, y=608–886`. A área preta interna também se expande, e os segmentos ciano do `07:30` ficam maiores e mais largos na captura (`x≈218–644`) que na referência (`x≈236–626`). Isso altera simultaneamente tamanho, espaçamento e peso percebido da tipografia digital.

- **Toggles dos cartões (alta):** na referência o toggle ocupa aproximadamente `x=596–714`, com uma cápsula larga. Na captura ele é uma cápsula bem mais estreita, aproximadamente `x=664–745`, deslocada para a direita; o mesmo ocorre no estado desligado da segunda linha. O botão de três pontos permanece perto da borda direita, de modo que o intervalo entre toggle e menu fica muito menor na captura.

- **Botão “+ Alarme” (alta):** a referência mantém o botão em aproximadamente `x=28–834, y=1647–1784`; a captura começa mais cedo (`x≈16–846, y≈1618`) e é mais larga, com o conteúdo mais próximo das bordas laterais. O texto pixelado `+ Alarme` aparece deslocado para a direita na captura, e o botão termina perto de `y=1750`, deixando uma faixa teal maior antes da barra de gesto. A referência mostra o botão mais abaixo e sem a barra Android.

### Outras diferenças observáveis

- Os cartões têm bordas e fundo cinza do mesmo estilo, mas a captura usa quase toda a largura disponível; as margens laterais são menores que as da referência. O bloco textual dos cartões fica alguns pixels mais à direita e os títulos/subtítulos ficam ligeiramente mais baixos dentro das caixas.

- A linha “Usar um modelo” da captura é mais larga (`x≈16–846`) que a referência (`x≈29–833`). O ícone e o texto mantêm a mesma leitura e estilo pixelado, mas o recuo lateral e a altura útil da linha mudam.

- O título `Desperta`, “PRÓXIMO ALARME” e os textos dos cartões conservam a linguagem tipográfica pixelada. A alteração tipográfica mais forte está no display LCD, cujos algarismos ficaram maiores na captura; a alteração de largura do cabeçalho e dos painéis também muda o espaçamento ao redor dos textos.

- O teal externo, o azul do cabeçalho/botão e o ciano do display continuam na mesma família de cores. O cinza dos painéis capturados parece levemente mais escuro/neutro, mas essa diferença é sutil diante das diferenças de geometria. As bordas bevel brancas/pretas continuam presentes, porém os recuos e a espessura aparente mudam junto com a largura dos painéis.

## Conteúdo dinâmico separado

Não foram usados como falha de identidade os valores variáveis do relógio/status Android nem a barra de navegação do sistema. Os valores textuais da área “próximo alarme” foram registrados apenas para deixar a comparação verificável; a conclusão visual principal vem de posição, escala, tipografia, controles, ornamentos, bordas e cortes.

