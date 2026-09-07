# Auditoria visual cega — rodada 9

Comparação visual direta entre as referências anteriores e os arquivos fechados em `/tmp/desperta-identity-review/round9-final/`. Coordenadas aproximadas, origem no canto superior esquerdo. Horário/ícones do Android e valores de contagem regressiva foram ignorados como conteúdo dinâmico; a barra de gesto é registrada somente como chrome do sistema.

## Matrix — divergências que permanecem

- **Logo:** a captura ainda coloca “Desperta” cerca de `6 px` à direita e `7–10 px` abaixo da referência (`y≈161` contra `y≈154`), com largura visual um pouco menor. A engrenagem já está visualmente alinhada e não aparece como divergência nesta rodada.
- **Cartão principal:** o topo está praticamente coincidente (`y≈288`), mas a captura termina cerca de `6 px` abaixo (`y≈686` contra `y≈680`). Há uma linha horizontal interna escura perto de `y≈300` na captura que não existe na referência. O preenchimento da captura é mais verde/visível; a referência é mais preta e tem glow concentrado no centro da borda inferior.
- **Título do cartão:** “PRÓXIMO ALARME” permanece aproximadamente `19 px` acima na captura (`y≈327` contra `y≈346`), com largura ligeiramente maior e traço mais cheio.
- **Relógio grande:** a captura agora está próxima verticalmente, mas fica cerca de `5 px` abaixo e é aproximadamente `17–20 px` mais estreita no total. O halo/verde dos segmentos é mais uniforme e intenso na captura.
- **Linha auxiliar:** ainda está aproximadamente `31 px` abaixo (`y≈619` contra `y≈588`) e um pouco mais estreita. A captura colore toda a linha em verde; na referência o trecho final “12 min” é branco. O texto de horas/minutos em si é dinâmico.
- **Cartões de alarmes:** os quadros estão próximos, mas a captura deixa as bases cerca de `4–5 px` mais baixas e mantém cantos mais arredondados e borda mais saturada. Cada cartão ainda tem uma linha horizontal interna logo abaixo do topo, ausente na referência.
- **Controles dos cartões:** os divisores continuam cerca de `11 px` à esquerda (`x≈714` contra `x≈725`) e visualmente mais curtos. Os menus de três pontos continuam aproximadamente `7 px` à esquerda; os toggles da captura são alguns pixels mais largos.
- **“Usar um modelo”:** a captura está cerca de `18–25 px` acima da referência (`top≈1410` contra `≈1428`) e fica um pouco mais baixa/compacta. O ícone continua aproximadamente `47 px` à esquerda (`x≈235` contra `x≈282`); o texto começa cerca de `12 px` antes (`x≈335` contra `≈347`).
- **“+ Alarme”:** a captura ainda é muito maior: aproximadamente `x=95–757, y=1575–1755`, contra `x=112–740, y≈1591–1740` na referência. O “+” continua menor e cerca de `25 px` mais à direita; o texto termina antes na captura. O glow e o verde da borda permanecem mais uniformes/intensos, com menos textura interna que a referência.
- **Sistema:** a barra branca de gesto inferior permanece na captura (`x≈314–539, y≈1816`); não pertence à identidade.

## Terminal — divergências que permanecem

- **Cabeçalho:** “Desperta” continua cerca de `8 px` à direita e `16–18 px` abaixo da referência. A linha divisória continua aproximadamente `8 px` acima e `8 px` mais para dentro em cada lado (`x≈41–811` contra `≈33–819`). “[AJUSTES]” permanece cerca de `25 px` à esquerda e `18–20 px` abaixo, além de ligeiramente mais estreito.
- **Cartão principal:** o quadro e as marcas de canto agora estão próximos, mas a captura é cerca de `5 px` mais larga (`x≈43–809` contra `≈44–804`) e termina cerca de `4 px` acima. O conteúdo interno ainda diverge: o título fica aproximadamente `5 px` abaixo, começa `19 px` mais à direita e é `25–30 px` mais estreito; o relógio fica cerca de `4 px` abaixo e `35–40 px` mais estreito; a linha auxiliar fica cerca de `9 px` abaixo e começa `13–14 px` mais à direita. O interior da captura é verde-escuro, enquanto a referência é quase preta; faixas horizontais de preenchimento continuam mais visíveis na captura.
- **Cartões de alarmes:** os dois quadros da captura continuam aproximadamente `7–12 px` acima, `5–10 px` mais baixos em sua base e alguns pixels mais largos. A hora fica perto de `15–23 px` acima da posição equivalente dentro do cartão; títulos e controles direitos também ficam mais altos. `[ATIVO]`/`[PAUSADO]` permanecem cerca de `24–25 px` à esquerda e mais estreitos. Os divisores continuam cerca de `6 px` à esquerda, começam muito próximos do topo do cartão e terminam antes; os menus `[...]` são mais largos e mais à esquerda. O fundo interno segue mais verde e com faixas retangulares mais marcadas.
- **“Usar um modelo”:** as marcas de canto já coincidem em estilo, mas a captura ainda é aproximadamente `17–18 px` mais larga (`x≈172–680` contra `≈181–672`) e `11–14 px` mais alta. O texto da captura é cerca de `30–32 px` mais estreito e fica aproximadamente `10 px` abaixo da referência.
- **“Novo alarme”:** o quadro está praticamente na mesma posição vertical, mas permanece cerca de `7–8 px` mais largo. O conteúdo `[ + NOVO ALARME ]` ocupa aproximadamente `20 px` a mais de largura na captura (`x≈155–700` contra `≈164–689`), com espaçamento/traço mais pesado.
- **Sistema:** a barra branca de gesto Android permanece na parte inferior da captura e não existe na referência.

## Diferenças de aparência ainda comuns

- A captura Matrix mantém bordas e glow mais saturados, com preenchimentos verde-escuros mais visíveis e cantos mais arredondados.
- A captura Terminal mantém tipografia de relógios/títulos mais compacta e cartões com interior mais verde; a referência preserva maior contraste preto e scanlines finas uniformes.
