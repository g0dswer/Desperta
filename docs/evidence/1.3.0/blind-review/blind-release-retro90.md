# Auditoria visual cega — `release-final`

Comparação somente entre as quatro imagens, sem consultar código ou histórico. Os horários do status Android, a barra de gesto e o texto variável do próximo alarme não foram usados como falhas de identidade.

## Falhas grandes de composição ainda presentes

### Retro

- A moldura da captura não tem os cantos pretos arredondados da referência e termina com a área de navegação Android. O conteúdo começa praticamente na mesma altura, mas o painel do herói termina alguns pixels antes e a moldura inferior não coincide.
- Os ícones circulares dos cartões continuam deslocados para a esquerda em relação à referência (aproximadamente 15–20 px); o restante do bloco textual já está muito próximo.
- A linha “Usar um modelo” permanece mais larga na captura, com o ícone/texto um pouco à esquerda e a seta mais à direita. O botão “+ Alarme” também ocupa uma largura maior que a referência e seu rótulo é ligeiramente mais estreito/à esquerda.

### Nineties

- O cabeçalho ainda emenda diretamente no painel cinza: na referência existe uma faixa teal visível entre os dois blocos. A captura tem o cabeçalho mais alto e o painel principal começa cerca de 30–35 px antes.
- O relógio despertador do cabeçalho continua menor/mais estreito e o título pixelado fica alto demais dentro da faixa azul.
- O display LCD agora tem fundo azul-marinho, mas sua moldura/área interna continua muito mais larga e alta que a referência; os dígitos ciano também permanecem grandes e espalhados. Esta é a principal divergência interna da tela.

## Diferenças finas de renderização/layout

- Retro: título e regras ornamentais do painel principal ainda têm recuos ligeiramente diferentes; toggles ficaram próximos, mas são um pouco mais estreitos. Persistem diferenças pequenas de largura externa, sombras, bordas e antialiasing.
- Nineties: o painel principal continua alguns pixels mais estreito; a ilustração solar e os textos dos cartões estão próximos, com pequenos deslocamentos horizontais. Toggles, menus, linha “Usar um modelo” e botão inferior agora estão essencialmente alinhados, restando poucos pixels de margem e bevel.
- Os valores exibidos no subtítulo do próximo alarme diferem entre referência e captura, mas foram tratados como conteúdo dinâmico.

