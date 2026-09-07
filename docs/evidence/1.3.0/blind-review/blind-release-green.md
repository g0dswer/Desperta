# Auditoria visual cega — captura final

Comparação somente visual entre as referências originais e `/tmp/desperta-identity-review/release-final/matrix.png` e `terminal.png`. As variações do relógio/status Android e os valores de contagem regressiva foram ignorados; a barra de gesto inferior também é chrome do sistema.

## Falhas de composição ainda visíveis

### Matrix

- O relógio grande não está centrado como na referência: o grupo `07:30` da captura começa aproximadamente `20–25 px` mais à esquerda, termina alguns pixels antes e fica mais largo para a esquerda. O painel e o título estão próximos, portanto o erro é específico do bloco do relógio.
- Nos cartões, os divisores verticais continuam cerca de `10–11 px` à esquerda e os menus de três pontos cerca de `5–7 px` à esquerda. Os cartões terminam alguns pixels abaixo da referência e têm cantos mais arredondados.
- O botão final está praticamente na posição correta, mas o símbolo `+` permanece menor e aproximadamente `20–25 px` mais à direita; o texto “Alarme” fica um pouco mais estreito.

### Terminal

- O cabeçalho ainda está verticalmente fora da referência: o logo fica aproximadamente `16 px` mais abaixo, a linha horizontal cerca de `9 px` acima, e a linha é mais curta/inserida nas laterais. `[AJUSTES]` também fica mais abaixo e apresenta largura menor. Esse é um desalinhamento estrutural da faixa superior.
- O menu `[...]` dos dois cartões de alarme quebra em duas linhas na captura (`[...]` aparece como uma linha superior e um `]` abaixo). Na referência ele é uma unidade horizontal única. Esta é a falha funcional/visual mais evidente da rodada.
- Os cartões de alarme começam cerca de `5–7 px` acima e terminam `9–13 px` acima, ficando mais baixos; a hora fica aproximadamente `20–25 px` acima da posição equivalente, enquanto título e controles também não mantêm a mesma distribuição vertical. O divisor inicia muito perto do topo do cartão.
- O botão “Usar um modelo” está aproximadamente `18 px` abaixo da referência e seu texto é cerca de `30 px` mais estreito. O quadro final de “Novo alarme” está próximo verticalmente, mas seu texto continua mais estreito.

## Diferenças finas de renderização

- **Matrix:** o título principal e as linhas auxiliares estão próximos; permanecem diferenças de saturação/glow (captura mais verde uniforme), o trecho final da linha auxiliar não fica branco como na referência e o fundo Matrix da captura tem menor contraste. O logo está apenas alguns pixels à direita/baixo e o botão “Usar um modelo” está próximo em posição.
- **Terminal:** o cartão principal está próximo em geometria e os relógios grandes têm altura semelhante, mas o título é mais estreito e deslocado à direita, a linha auxiliar fica cerca de `9 px` abaixo e o preenchimento da captura é verde-escuro mais uniforme que o fundo preto da referência. As molduras e marcas de canto estão próximas; ainda há pequeno recuo horizontal adicional nas molduras da captura.
- A tipografia terminal da captura continua mais compacta em vários textos e o rastro/scanline fica mais regular e brilhante em alguns segmentos.

As barras brancas de gesto inferiores presentes nas capturas foram mantidas fora do julgamento da identidade visual.
