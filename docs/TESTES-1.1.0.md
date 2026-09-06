# Desperta 1.1.0 — correções solicitadas e testes

Ambiente: Android 15 / API 35, emulador Pixel 7 ARM64. APK de release assinado com a mesma chave da versão 1.0.0, instalado com atualização (`adb install -r`). O aparelho Samsung do usuário não esteve disponível; não afirmamos ter reproduzido a falha específica desse aparelho.

## Resultado

- 18 testes locais aprovados, sem falhas.
- 36 testes Android aprovados em uma execução integral final, sem falhas nem ignorados, no APK assinado (196,884 segundos).
- 4 testes de câmera com EAN-13 aprovados.
- Alarme agendado com PIN no emulador: aprovado no leitor vertical.
- `lintDebug`, build de release e assinatura v2/v3 aprovados.

## Pedidos e evidências

| Pedido | Mudança | Validação |
|---|---|---|
| Código reconhecido deve parar o alarme | MissionActivity entrega conclusão diretamente ao serviço; o retorno da tela não é o único canal | Câmera do emulador leu o QR e encerrou o alarme real; sessão removida, histórico gravado uma vez e alarme único desativado |
| Código errado não pode concluir | Comparação exata com a lista selecionada | Câmera leu código diferente, manteve a missão aberta e não concluiu |
| Vários códigos aceitos | Lista `targets`, mantendo compatibilidade com `target` antigo | Teste com primeiro código incorreto e segundo correto encerrou o alarme |
| Leitor como a referência enviada | Câmera vertical em tela cheia, área externa escurecida, retângulo central, instrução acima e lanterna abaixo | Teste de orientação e posição dos controles; alternância de estado da lanterna no emulador; QR e EAN-13 lidos pela câmera |
| Funcionar depois de a tela apagar | Scanner com suporte à tela bloqueada; tela do alarme acorda o aparelho | Alarme agendado tocou após a tela apagar e foi encerrado pela câmera; teste adicional com bloqueio por PIN também passou |
| Evitar conclusão duplicada ou atrasada | Índice esperado acompanha cada resultado; mensagens de controle não iniciam novo serviço de primeiro plano | Repetir o resultado da missão anterior não avançou a seguinte; repetir depois de encerrar não recriou sessão nem derrubou o processo |
| Horário por scrolling | NumberPicker de horas e minutos com rolagem circular | Gestos reais alteraram ambos os valores; salvar e reabrir preservou o horário |
| Adicionar códigos | Scanner cadastra na biblioteca local | Código lido pelo driver da câmera apareceu na lista selecionado |
| Selecionar e revisar | Seleção múltipla, menu individual com valor completo | Selecionar dois, desmarcar um e revisar código passaram |
| Excluir | Confirmação remove da biblioteca e da seleção atual | Código desapareceu; outro alarme salvo preservou seu próprio código |
| Testar antes de salvar | Botão Testar leitura | Leitura correta retornou confirmação, sem tocar um alarme |
| Preservar dados antigos | Leitura do formato anterior e migração da biblioteca | JSON da versão antiga, roundtrip, cópia independente e seleção no editor passaram |

## Falhas encontradas durante a validação

1. O teste de revisão procurava `Fechar` com comparação sensível a maiúsculas. O Android apresenta o botão do diálogo em caixa alta. O localizador foi corrigido para ignorar caixa; o fluxo passou.
2. Um cenário com callbacks sobrepostos revelou `ForegroundServiceDidNotStartInTimeException`: uma resposta tardia podia iniciar um serviço sem alarme ativo. Agora apenas o disparo cria um serviço de primeiro plano; controles atrasados sem sessão são encerrados. Foi adicionada regressão que espera além do prazo de erro do Android após repetir a conclusão. Referência: [ciclo de vida de serviços de primeiro plano](https://developer.android.com/develop/background-work/services/fgs/troubleshooting).

3. O cenário com PIN mostrou instâncias sobrepostas da tela do alarme. A atividade agora preserva a missão na mudança de orientação; quando o aparelho está bloqueado, somente o full-screen intent da notificação abre a tela, evitando a segunda abertura manual.
4. Um teste verificava a sessão entre a gravação do histórico e sua remoção. A espera agora acompanha ambas as condições de término antes das asserções.

5. O teste de alarme real abria manualmente outra RingActivity, embora o serviço já a abrisse. Isso provocava uma corrida no fechamento de ActivityScenario. O teste agora usa somente a abertura real do serviço e aguarda o retorno à tela principal; os quatro testes de câmera passaram novamente.

A leitura utiliza o fixture gerado por `scripts/camera-fixture.py`, transmitido pela câmera do emulador e decodificado pelo ZXing. Não é injeção de um resultado de leitura. Testes locais adicionais decodificam QR, EAN-13 e Code 128 a partir de pixels.

## Limites

Não houve teste no Samsung original nem em câmera, sensores, vibração, voz ou Bluetooth físicos. Um emulador não substitui a confirmação no aparelho em que o problema foi relatado. Consulte a [matriz geral](FUNCIONALIDADES.md) para a cobertura das funções que não foram alteradas nesta versão.


## Reprodução dos testes de câmera

O QR padrão é criado com `python scripts/camera-fixture.py`. Inicie o emulador com `-camera-back imagefile:/tmp/desperta-correct.png`.

Para EAN-13, `scripts/BarcodeCameraFixture.java` gera a imagem de teste usando o ZXing Core 3.4.1 (disponível nas dependências Gradle). Execute com Java 17 e o JAR no classpath, indicando um PNG de saída; copie-o para o caminho da câmera antes do teste. Rode `CameraPipelineTest` com o argumento de instrumentação `barcodeFixtureValue=7891035002427`. O valor esperado não é injetado no leitor; ele serve para comparar o resultado decodificado da imagem.

As evidências de execução ficam em `docs/evidence/`. A iluminação da lanterna e a câmera físicas não foram avaliadas.

Execução integral final: [log dos 36 testes](evidence/android-1.1.0-final.txt). O CI do código também [passou no GitHub](https://github.com/g0dswer/Desperta/actions/runs/34058243752).
