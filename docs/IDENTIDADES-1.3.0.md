# Identidades do Desperta 1.3.0

Retrofuturista é a identidade padrão em instalações novas e na migração da antiga opção de tema. Ajustes → Identidade permite escolher Retrofuturista, Anos 90, Matrix ou Terminal antigo; a escolha é persistida. Não há login, anúncios ou recursos pagos.

## Implementação e revisão

| Identidade | Tratamento |
|---|---|
| Retrofuturista | Papel creme, azul petróleo, laranja, ornamentos orbitais aprovados, placas metálicas, relógio dinâmico e emblemas |
| Anos 90 | Desktop verde-petróleo, janelas cinza com relevos, cabeçalho azul, ícones pixelados, tipografia e mostrador digital |
| Matrix | Fundo de códigos, verde sobre preto, mostradores digitais, contornos luminosos e controles contrastantes |
| Terminal antigo | Tipografia monoespaçada pixelada, textura de fósforo, perímetros finos, controles entre colchetes |

A identidade é aplicada à inicial, editor e seções, seleção/configuração/execução de missões, biblioteca de códigos, leitor, alarme tocando, Ajustes, atualização e diálogos do aplicativo. A imagem da câmera permanece sem filtro para preservar a leitura. O instalador, permissões e barras do Android pertencem ao sistema.

Horários, nomes, recorrência, estados e ações são controles reais. Somente ornamentos estáticos e texturas são aproveitados das imagens de referência; a inicial não é uma captura usada como uma falsa interface. Há fontes abertas e uma derivação documentada da fonte do relógio em `app/src/main/assets/fonts/README.md`.

## Auditoria cega

Revisores independentes receberam pares de imagens, sem código, histórico de correções ou explicações sobre a implementação. Os relatórios descrevem diferenças concretas, incluindo posição, tamanho, recortes, tipografia, bordas e cores. Rodadas posteriores usaram também a resolução exata de cada referência para não confundir proporção do dispositivo com erro de layout.

Os relatórios originais, inclusive apontamentos negativos, estão em [evidências da revisão](evidence/1.3.0/blind-review/). Os apontamentos motivaram correções de sobreposição, corte do botão de modelos, borda dupla, relógio invisível, tipografia, ícones, espaçamentos e relevo. A aprovação funcional dos testes não prova igualdade pixel a pixel. As referências são imagens geradas; texto dinâmico, rasterização de fontes e componentes do Android exigem avaliação separada.

Veja [testes desta versão](TESTES-1.3.0.md) para evidências executadas e limites.
