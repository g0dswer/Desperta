# Auditoria visual cega — rodada `round9-final`

Comparação direta das capturas fechadas com as referências originais, somente por inspeção das imagens. Coordenadas aproximadas em pixels, a partir do canto superior esquerdo. Valores de relógio/status Android e a barra de gesto foram desconsiderados como conteúdo dinâmico.

## Retro — diferenças que ainda permanecem

- **Moldura/recorte:** a referência conserva cantos pretos arredondados nos quatro cantos; a captura ocupa esses cantos com o fundo creme e mostra a navegação Android no rodapé. O conteúdo principal já está quase na mesma altura: o herói começa perto de `y=92` nos dois, mas a captura termina alguns pixels antes (`y≈557` contra `y≈568`) e deixa a borda inferior diferente.

- **“PRÓXIMO ALARME”:** o título da captura continua menor/mais estreito (`x≈280–575`, contra `x≈267–591`). As linhas decorativas continuam longas demais: captura aproximadamente `x=50–250` e `x=603–803`; referência `x=87–236` e `x=622–767`.

- **Relógio grande:** o `07:30` continua menor e mais estreito na captura (aprox. `x=105–749`) que na referência (`x=84–770`). A linha fina inferior também vai quase de borda a borda na captura (`x≈50–803`), em vez do recuo da referência (`x≈86–768`).

- **Cards:** o grupo circular de ícone e pontos da esquerda permanece deslocado cerca de 15–20 px para a esquerda na captura. O bloco textual começa alguns pixels antes e o subtítulo fica mais baixo, perto da borda inferior do cartão. Os toggles continuam menores/estreitos no conjunto visual e deslocados para a direita: o laranja fica em torno de `x=610–721`, contra `x=580–699` na referência; o menu de três pontos permanece junto à borda direita.

- **“Usar um modelo”:** a captura ainda é mais larga (`x≈17–836`, contra `x≈24–830`), com o ícone/texto deslocados para a esquerda e a seta deslocada para a direita. A borda inferior agora está visível, mas os recuos laterais continuam diferentes.

- **“+ Alarme”:** tamanho e altura ficaram próximos da referência, porém o botão continua mais largo (`x≈18–835`, contra `x≈26–829`). A diferença de conteúdo é explícita: a referência tem o rótulo branco `+ Alarme`, enquanto a captura mostra somente `Alarme` no texto grande; o “+” do círculo à esquerda não substitui o “+” do rótulo. A barra de gesto e a ausência dos cantos pretos inferiores permanecem diferenças de recorte.

- **Subtítulo:** o valor (`12 h 21 min` contra `8 h 12 min`) é conteúdo variável, mas o separador continua com cor diferente: laranja na referência e pálido/azulado na captura.

## Nineties — diferenças que ainda permanecem

- **Cabeçalho e intervalo vertical:** a captura tem o cabeçalho aproximadamente em `x=16–846`, contra `x=10–852` na referência. O cabeçalho capturado termina perto de `y=232` e emenda diretamente no painel cinza; na referência termina perto de `y=219` e há uma faixa teal de aproximadamente 45 px antes do painel principal (`y≈265`). Esse desaparecimento do intervalo e o cabeçalho mais alto são a maior diferença estrutural restante.

- **Elementos do cabeçalho:** o despertador capturado ainda é mais estreito/menor que o da referência; a engrenagem está alguns pixels mais acima. O título pixelado fica mais alto dentro do cabeçalho capturado (aprox. 15–20 px acima da referência), apesar de manter o mesmo estilo e largura aproximada.

- **Painel principal:** por causa do cabeçalho, a captura começa cerca de 30–35 px antes (`y≈232`, contra `y≈265`) e fica mais alta até alcançar praticamente o mesmo fim (`y≈1016–1020`). Continua mais estreita (`x≈46–816`, contra `x≈40–822`). O título começa cerca de 10 px mais à direita e a linha branca é mais curta (`x≈86–777`, contra `x≈69–794`).

- **Display LCD (alta):** a moldura capturada continua mais larga (`x≈148–713`, contra `x≈157–706`) e os algarismos são maiores/largos. Além da geometria, o preenchimento está errado: na referência a área interna do display é azul-marinho quase preta; na captura ela aparece cinza, próxima ao painel, deixando os segmentos ciano sem o fundo escuro da referência. A borda/bevel interna também fica diferente.

- **Toggles dos cards:** o tamanho agora está mais próximo, mas ambos continuam deslocados para a direita cerca de 30–35 px: captura aproximadamente `x=630–744`, referência `x=596–713`. Os menus de três pontos ficam apenas alguns pixels mais à direita que na referência.

- **Cards e linha de modelo:** as caixas e ícones estão próximos em escala, mas a captura deixa o texto dos cartões alguns pixels mais à direita/baixo. A linha “Usar um modelo” permanece ligeiramente mais larga e mais alta na captura (`x≈26–836` contra `x≈29–833`), com o ícone e o título deslocados alguns pixels para a direita.

- **Botão “+ Alarme”:** a caixa capturada continua muito mais larga (`x≈16–846`, contra `x≈28–834`), embora a altura e a posição vertical estejam próximas. O rótulo pixelado está deslocado fortemente para a direita: começa aproximadamente em `x≈366` na captura, contra `x≈306` na referência. A borda bevel é semelhante, mas as margens teal laterais não são.

- **Cores e tipografia:** teal, azul do cabeçalho/botão e cinza geral permanecem na mesma família. A diferença de cor crítica é o interior do LCD. A linguagem pixelada dos textos está próxima; a maior divergência tipográfica restante é o tamanho/espalhamento dos dígitos LCD e o posicionamento do rótulo inferior.

