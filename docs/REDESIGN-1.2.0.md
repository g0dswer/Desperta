# Desperta 1.2.0 — escopo de redesign

Registro do escopo aprovado. Os resultados executados e os limites de validação estão no [relatório da versão](TESTES-1.2.0.md).

## Decisões de escopo

- Itens **1–4 e 6–10** da proposta de redesign foram aprovados.
- O **item 5 foi rejeitado**. A biblioteca de QR/código de barras mantém os
  valores originais cadastrados, a seleção múltipla, a revisão e a exclusão. Não
  serão introduzidos aliases, nomes inventados ou migração que altere o valor
  lido.
- Sono, Manhã e Relatório continuam fora do produto, conforme decisão anterior.
- Conta, Pro, anúncios e login continuam fora do produto. O app permanece
  gratuito e sem anúncios.
- O verificador de atualizações públicas do GitHub foi autorizado como parte
  desta etapa. A fonte configurada é `g0dswer/Desperta`; a instalação sempre
  depende da confirmação do instalador do Android.

## Matriz de implementação e validação

| Item | Escopo decidido | Estado no código atual | Evidência e limites 1.2.0 |
|---|---|---|---|
| 1. Identidade Amanhecer | Paleta clara/escura inspirada no amanhecer, sol como marca, nome Desperta e hierarquia mais acolhedora. | **Presente.** A identidade, cores, cabeçalho e tema são montados em `MainActivity`. | Capturas em claro/escuro, edição com fonte 130% e persistência passaram; TalkBack completo não auditado. |
| 2. Tela inicial objetiva | Próximo alarme em destaque, cartões compactos e ações separadas para testar, duplicar, pular, editar e excluir. | **Presente.** A tela inicial usa cartão de próxima ocorrência, menus de ação e restauração por desfazer. | Fluxos automatizados de criar/editar/duplicar/pular/excluir/desfazer passaram. |
| 3. Editor mais curto | Horário, dias, nome e missão visíveis; som, soneca, voz e aparência agrupados em seções que abrem sob demanda. | **Presente.** `MainActivity` mantém seções expansíveis e preserva a rolagem do editor. | Controles, salvar/cancelar e reabrir seções passaram em automação; estudo com usuários não realizado. |
| 4. Horário por rolagem | Colunas circulares de hora/minuto, feedback tátil e presets de dias úteis, fim de semana, todos os dias e uma vez. | **Presente.** O editor usa `NumberPicker` circular e controles de dias; não é o disco de horário das imagens. | Gestos reais de hora/minuto, presets e persistência passaram; cálculos de agenda/fuso cobertos em lógica. |
| 5. Item rejeitado | Não renomear códigos nem criar aliases para a biblioteca. Preservar o valor completo lido e a compatibilidade dos dados existentes. | **Preservado.** O modelo e a UI continuam tratando o valor cadastrado como referência; nenhuma mudança de alias foi incluída. | Cinco testes de biblioteca passaram: cadastro, seleção, revisão, exclusão e prévia. |
| 6. Leitor com feedback claro | Leitor vertical em tela cheia, retângulo central, área externa escurecida, instrução e lanterna; confirmação só para código selecionado. | **Presente.** O fluxo de scanner e a entrega da conclusão ao serviço existem; os valores da biblioteca permanecem originais conforme o item 5. | QR real no emulador, código errado, seleção múltipla e enquadramento passaram; limites físicos no relatório. |
| 7. Tela de alarme acionável | Uma ação principal contextual, sonecas restantes, progresso de várias etapas e confirmação de encerramento. | **Presente.** `RingActivity` exibe o estado contextual e o serviço controla a sessão real sem depender apenas do retorno da Activity. | Disparo real, código correto/incorreto, soneca e retorno à inicial passaram em testes Android. |
| 8. Missões organizadas | Seleção por grupos, descrições curtas, ordem de até cinco missões, prévia e configuração sem esconder a missão ativa. | **Presente.** O seletor de missões e a prévia estão integrados ao editor; a ordem salva acompanha o alarme. | Limite, reordenação, candidato experimentado antes de adicionar e prévia sem efeitos colaterais passaram. |
| 9. Pequenas conveniências | Pular uma ocorrência, duplicar, salvar modelo, desfazer exclusão e alterar somente a próxima ocorrência (`Só na próxima vez`). | **Presente.** A tela inicial expõe as ações; `Scheduler` persiste o override, consome-o uma vez em disparo real e suprime a ocorrência semanal substituída. | Lógica de exceção anterior/posterior/reinício/substituição passou; entrega real, prévia e desfazer passaram no Android. |
| 10. Conforto e acessibilidade | Áreas de toque confortáveis, descrições de conteúdo, feedback de foco, temas e textos que explicam o efeito de cada ação. | **Presente em parte.** Há descrições, controles ampliados e opções de tema no código; a cobertura precisa ser conferida no dispositivo. | Fonte 130% e temas inspecionados; teste verifica botão Salvar com ao menos 48 dp. TalkBack completo e hardware físico pendentes. |
| Atualizações GitHub (autorizado) | Verificar releases públicas, permitir checagem diária opcional, incluir/excluir pré-lançamentos, conferir SHA-256 e abrir a instalação com confirmação do Android. | **Presente no código.** `UpdateChecker`, `UpdateActivity`, Settings e o aviso na inicial estão ligados ao repositório público `g0dswer/Desperta`. | Consulta real ao GitHub, comparação de versões, filtro e cache passaram. Ensaio de download/instalação consta no relatório. |

## Validação

Foram aprovados 31 testes de lógica e 48 testes Android na execução final do APK assinado. O [relatório](TESTES-1.2.0.md) contém os logs, as capturas e os ensaios extras. Os testes cobrem tarefas concretas de uso; não equivalem a um estudo com participantes nem a uma garantia em todos os fabricantes.

A detecção física de movimentos, voz, fotos e objetos mantém os limites da matriz original. A inspeção de fonte e controles não substitui uma auditoria completa com TalkBack.

## Referências

- [Inventário de funcionalidades e limites](FUNCIONALIDADES.md)
- [Relatório histórico da versão 1.1.0](TESTES-1.1.0.md)
- [Matriz geral de testes](TESTES.md)
- Código principal: `app/src/main/java/com/desperta/MainActivity.java`,
  `Scheduler.java`, `AlarmService.java`, `UpdateChecker.java` e
  `UpdateActivity.java`.
