# Auditoria visual cega — identidade Matrix e Terminal

Inspeção feita somente por visualização das imagens, sem leitura de código ou histórico. A origem das coordenadas é o canto superior esquerdo; os valores abaixo são aproximados em pixels e referem-se às imagens abertas no início desta rodada.

Dimensões observadas:

- Matrix: referência e captura, `853 × 1844`.
- Terminal: referência e captura, `852 × 1846`.

As diferenças de hora do relógio do sistema, ícones da barra de status e valores de contagem regressiva foram tratadas como conteúdo dinâmico. A barra de gesto Android inferior aparece nas capturas e é registrada separadamente como chrome do sistema.

## Matrix

### Cabeçalho

- **Logo — posição e escala:** na referência, “Desperta” ocupa aproximadamente `x=282–570, y=154–195`; na captura, `x=288–565, y=161–208`. A captura está cerca de `6–7 px` mais à direita e `7–13 px` mais baixa, com largura aproximadamente `10–12 px` menor e altura visual um pouco maior.
- **Logo — tipografia:** as duas versões usam o mesmo conceito monoespaçado segmentado, mas a captura tem desenho mais largo/cheio nos traços verticais e aparência menos arejada; a referência tem o contorno mais fino e espaçamento mais aberto.
- **Engrenagem:** o centro da referência fica perto de `(790,173)`; o da captura perto de `(775,194)`. Portanto, a captura desloca o ornamento aproximadamente `15 px` para a esquerda e `21 px` para baixo. O desenho é semelhante, mas o da captura parece ligeiramente menor e mais fino.
- O horário e o ícone extra junto a ele na captura são barra de status Android e foram excluídos da avaliação de identidade.

### Cartão “PRÓXIMO ALARME”

- **Quadro — posição/tamanho:** referência aproximadamente `x=63–790, y=288–680`; captura `x=62–791, y=273–671`. A captura começa `15 px` mais acima, termina `9 px` mais acima, fica cerca de `2 px` mais larga e resulta aproximadamente `6 px` mais alta.
- **Cantos e borda:** a referência tem cantos arredondados mais discretos, uma borda única verde acinzentada e pouca emissão uniforme. A captura tem cantos visualmente mais redondos, borda mais saturada/brilhante em verde e contorno mais regular; perde o realce localizado no centro da borda inferior que aparece na referência, perto de `x=390–465, y≈680`.
- **Preenchimento:** o interior da referência é quase preto com textura Matrix muito discreta; o interior da captura fica mais visivelmente verde-escuro e com faixas/gradientes horizontais mais perceptíveis.
- **Título:** referência aproximadamente `x=299–554, y=346–370`; captura `x=295–559, y=313–337`. A captura sobe cerca de `33 px`, fica um pouco mais larga e tem traço mais pesado/luminoso.
- **Relógio grande:** referência aproximadamente `x=197–655, y=418–539`; captura `x=207–644, y=409–530`. A captura está cerca de `9 px` mais alta e é aproximadamente `20–22 px` mais estreita no total; a referência tem halo verde mais amplo e macio ao redor dos segmentos.
- **Linha auxiliar:** ignorando o texto dinâmico, a referência fica por volta de `y=588–611`; a captura, `y=606–629`. Assim, a captura põe a linha cerca de `18 px` mais abaixo, apesar de o relógio grande estar mais acima. A captura também a desenha toda em verde; na referência, o trecho final da linha (`12 min`) aparece branco, criando contraste de cor.

### Cartões de alarmes

- **Primeiro cartão — quadro:** referência aproximadamente `x=41–811, y=734–917`; captura `x=42–811, y=721–906`. A captura sobe cerca de `13 px` no topo e `11 px` na base, mantendo quase a mesma largura e altura.
- **Segundo cartão — quadro:** referência aproximadamente `x=41–811, y=941–1122`; captura `x=42–811, y=928–1113`. A captura sobe cerca de `13 px` no topo e `9 px` na base. O intervalo entre os cartões também fica ligeiramente menor na captura (`~22 px` contra `~24 px`).
- **Bordas/cantos:** a referência usa bordas únicas, mais finas e verde acinzentadas; a captura usa bordas mais vivas/saturadas, com cantos visualmente mais arredondados e maior contraste contra o fundo.
- **Conteúdo esquerdo:** nos dois cartões, a captura coloca hora, título e subtítulo cerca de `8–10 px` acima em termos absolutos. As horas têm largura muito parecida, mas a captura parece um pouco mais compacta; os textos “Trabalho”, “Fim de semana” e as linhas auxiliares ficam ligeiramente menores/mais estreitos.
- **Toggle ativo/inativo:** referência aproximadamente `x=609–702` no primeiro e `x=608–703` no segundo; captura aproximadamente `x=606–704` nos dois. A captura alarga o controle em cerca de `4–6 px`, sobe cerca de `8–10 px` e usa uma borda/track mais marcada.
- **Divisor vertical:** referência perto de `x=725, y=775–878` no primeiro cartão e `x=725, y=981–1084` no segundo. Captura perto de `x=714, y=768–857` e `x=714, y=976–1064`. Na captura o divisor desloca-se aproximadamente `11 px` para a esquerda e fica visivelmente mais curto.
- **Menu de três pontos:** captura fica aproximadamente `7 px` mais à esquerda e `8–12 px` mais acima. Os pontos da referência têm presença mais regular; os da captura ficam mais comprimidos verticalmente.

### Botões inferiores

- **“Usar um modelo” — quadro:** referência aproximadamente `x=207–645, y=1428–1545`; captura `x=205–648, y=1436–1549`. A captura é cerca de `5 px` mais larga, começa `8 px` mais abaixo e termina `4 px` mais abaixo; a altura fica ligeiramente menor.
- **“Usar um modelo” — conteúdo:** na referência, o ícone quadriculado fica perto de `x=282–318` e o texto começa em `x≈347`. Na captura, o ícone fica perto de `x=235–271` e o texto começa em `x≈334`. Ou seja, o ícone da captura desloca-se aproximadamente `47 px` para a esquerda e o texto cerca de `13 px` para a esquerda; o grupo deixa de ficar centralizado como na referência. A captura também usa ícone e contorno mais brilhantes.
- **Botão “+ Alarme” — quadro:** referência aproximadamente `x=112–740, y=1590–1740` (incluindo o brilho externo; borda principal perto de `y=1597–1738`); captura aproximadamente `x=119–734, y=1602–1744`. A captura fica cerca de `12–14 px` mais estreita, deslocada para dentro em ambos os lados e aproximadamente `8–12 px` mais baixa no conjunto.
- **Botão “+ Alarme” — glow e preenchimento:** a referência tem halo largo, forte e localizado acima/abaixo da borda, além de textura Matrix visível no preenchimento. A captura tem borda mais uniforme e saturada, halo mais estreito e menos textura interna.
- **Símbolo e texto do botão:** o “+” da referência mede aproximadamente `52 px` de largura e começa perto de `x=283`; na captura mede cerca de `40 px` e começa perto de `x=309`, portanto fica menor e aproximadamente `25 px` mais à direita. O texto da captura é um pouco mais estreito (`fim perto de x=551` contra `x≈560` na referência).
- **Chrome Android:** a captura contém a barra branca de gesto aproximadamente em `x=314–539, y=1816–1825`; a referência não contém essa barra. Isso é overlay do sistema, não elemento da identidade.

## Terminal

### Cabeçalho

- **Divisor horizontal:** referência aproximadamente `x=33–819, y=210–213`; captura `x=41–811, y=202–205`. A captura desloca a linha `8 px` para dentro em cada lado e cerca de `8 px` para cima, reduzindo o comprimento em aproximadamente `16 px`.
- **Logo:** referência aproximadamente `x=42–310, y=126–170`; captura `x=50–318, y=142–188`. A captura desloca o logo `8 px` à direita e `16–18 px` para baixo, mantendo largura parecida, mas com traço visual um pouco mais cheio e scanlines mais evidentes.
- **“[AJUSTES]”:** referência aproximadamente `x=665–810, y=134–163`; captura `x=640–778, y=153–181`. A captura desloca o bloco cerca de `25 px` para a esquerda, encurta a largura em cerca de `7–10 px` e desce `18–20 px`. O desalinhamento horizontal entre logo e ajustes fica mais evidente na captura.

### Cartão principal

- **Quadro — posição/tamanho:** referência aproximadamente `x=44–805, y=279–673`; captura `x=43–809, y=276–721`. O topo da captura fica só `3 px` acima, mas a base fica cerca de `48 px` abaixo; a captura aumenta a altura em aproximadamente `51 px` e fica cerca de `5 px` mais larga.
- **Borda e ornamentos:** a referência tem uma linha única fina com marcas “+” salientes nos quatro cantos. A captura substitui isso por um quadro de duas linhas paralelas/duplo contorno, sem as cruzes salientes; esse mesmo padrão reaparece nos cartões e botões inferiores.
- **Título:** referência aproximadamente `x=285–557, y=321–348`; captura `x=304–550, y=326–352`. A captura desce cerca de `5 px`, desloca o início `19 px` para a direita e reduz a largura em aproximadamente `25–30 px`; a fonte fica mais compacta.
- **Relógio grande:** referência aproximadamente `x=178–656, y=400–535`; captura `x=204–648, y=428–558`. A captura desce cerca de `28–30 px`, estreita cerca de `35 px` no total e fica ligeiramente menor em altura. O desenho segmentado e as scanlines continuam presentes, mas os segmentos da captura têm aparência mais grossa/compacta.
- **Linha auxiliar:** referência aproximadamente `x=206–634, y=592–619`; captura `x=219–634, y=652–678`. A captura desce cerca de `60 px`, começa `13–14 px` mais à direita e fica um pouco mais estreita. O valor de horas/minutos é dinâmico e foi desconsiderado; posição e tipografia foram comparadas.
- **Faixas internas:** a captura mostra faixas retangulares verde-escuras atrás do título e da linha auxiliar (aproximadamente `x=74–778`), enquanto a referência mantém o cartão quase uniforme, com apenas a textura horizontal fina de terminal.

### Cartões de alarmes

- **Primeiro cartão — quadro:** referência aproximadamente `x=44–805, y=743–962`; captura `x=43–809, y=783–1002`. A captura desloca o cartão cerca de `40 px` para baixo, mantém altura próxima e aumenta a largura em aproximadamente `5 px`.
- **Segundo cartão — quadro:** referência aproximadamente `x=44–805, y=1021–1241`; captura `x=43–809, y=1065–1284`. A captura desloca-o cerca de `43–44 px` para baixo e aumenta ligeiramente a largura; o espaçamento entre os cartões fica um pouco maior.
- **Quadros dos cartões:** referência usa linha única e quatro “+” nos cantos; captura usa duplo contorno paralelo e cantos quadrados sem as marcas “+”.
- **Conteúdo esquerdo:** no primeiro cartão, a hora da referência fica perto de `x=84–300, y=778–837`; na captura, `x=85–293, y=804–861`. A captura fica mais estreita e, apesar do cartão estar mais abaixo, o conteúdo fica aproximadamente `12–14 px` mais próximo do topo do cartão. “Trabalho” e a linha auxiliar da captura começam perto de `x=80`, contra `x≈84` na referência, e têm aparência um pouco mais compacta.
- **Controles direitos:** no primeiro cartão, `[ATIVO]` fica perto de `x=552–665` na referência e `x=528–638` na captura; a captura desloca-o aproximadamente `24 px` à esquerda e estreita o texto. No segundo, a mesma tendência aparece em `[PAUSADO]`. Os menus `[...]` da captura têm espaçamento horizontal mais largo e ficam mais à esquerda/abaixo em relação ao cartão.
- **Divisores verticais:** referência perto de `x=694`, com altura aproximada de `145 px`; captura perto de `x=688`, com altura aproximada de `90 px`. A captura desloca o divisor `6 px` para a esquerda e reduz sua altura em cerca de `55 px`.
- **Preenchimento:** as faixas retangulares escuras/verdeadas atrás das linhas internas são muito mais visíveis na captura; na referência, o fundo é predominantemente preto com scanlines finas uniformes.

### Botões inferiores

- **“Usar um modelo” — quadro:** referência aproximadamente `x=181–672, y=1354–1451`; captura `x=172–680, y=1356–1465`. A captura é cerca de `16–18 px` mais larga, começa quase na mesma altura e fica aproximadamente `14 px` mais alta na base. A referência tem borda única com cruzes; a captura tem duplo contorno sem cruzes.
- **Texto do modelo:** referência aproximadamente `x=256–599, y=1391–1421`; captura `x=270–583, y=1400–1434`. A captura estreita o texto em aproximadamente `30 px` e desce cerca de `9–13 px`; o grupo fica centralizado, mas visualmente menor.
- **Espaço até “Novo alarme”:** referência deixa cerca de `160 px` entre a base do botão de modelo e o topo do botão final; captura deixa cerca de `130 px`. A captura reduz esse vazio em aproximadamente `30 px`.
- **“[ + NOVO ALARME ]” — quadro:** referência aproximadamente `x=47–806, y=1614–1784`; captura `x=43–809, y=1597–1754`. A captura é cerca de `7–8 px` mais larga, começa `17 px` mais acima, termina `30 px` mais acima e fica aproximadamente `13 px` mais baixa em altura total.
- **“[ + NOVO ALARME ]” — conteúdo:** referência ocupa aproximadamente `x=164–689`; captura `x=155–700`. A captura aumenta a largura visual do conjunto em cerca de `30 px` e deixa o texto/colchetes mais espaçados ou pesados, mantendo o centro próximo de `x≈426`.
- **Chrome Android:** a barra branca de gesto aparece na captura em aproximadamente `x=315–538, y=1817–1825` e não existe na referência. É overlay do sistema e foi excluída da identidade.

## Diferenças recorrentes

1. As capturas Matrix usam verde mais saturado/brilhante nas bordas, cantos mais arredondados e composição vertical ligeiramente refluída; as referências usam contornos mais suaves, cinza-esverdeados, halos localizados e mais espaço negativo.
2. A captura Matrix desloca a engrenagem para a esquerda/baixo, desloca o ícone de “Usar um modelo” muito para a esquerda e reduz o “+” de “Alarme”.
3. As capturas Terminal têm quadros mais largos, vários cartões mais altos ou deslocados, e substituem sistematicamente a linha única com marcas “+” por duplo contorno sem ornamentos de canto.
4. A captura Terminal comprime a tipografia de títulos e relógios, mas amplia o conjunto textual do botão final; portanto, não é uma simples escala global.
5. As duas capturas apresentam faixas de preenchimento/gradientes verde-escuros mais visíveis e menor uniformidade de fundo que as referências.
6. As barras de gesto e as variações do relógio/ícones de status pertencem ao Android e não foram usadas como reprovação da identidade visual.

## Evidência de rodada

Este relatório corresponde especificamente às capturas que foram abertas em `/tmp/desperta-identity-review/reference-size/matrix.png` e `terminal.png` no início desta auditoria. Esses caminhos foram posteriormente atualizados por uma nova execução durante o trabalho; não foram reabertos nem misturados nesta comparação. Uma rodada posterior deve usar outro diretório fechado se a intenção for comparar novas capturas.
