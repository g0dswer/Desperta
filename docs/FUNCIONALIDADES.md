# Inventário das oito imagens e validação

O escopo abaixo corresponde às cinco primeiras imagens e às três adicionais. Sono, Manhã e Relatório foram removidos por solicitação posterior. Conta, Pro, propaganda e banners promocionais foram substituídos por todas as funções gratuitas, sem login e sem anúncios.

**Legenda:** “automatizado” significa teste executável de lógica ou integração; “emulador” significa fluxo Android executado; “aparelho pendente” significa que não existe evidência de validação em hardware físico. Não equivale a garantia em todos os fabricantes.

| Função identificada | Implementação | Evidência / limite |
|---|---|---|
| Lista de alarmes, horário e dias | MainActivity + Store | CRUD Android e persistência |
| Criar e editar alarme | Editor nativo, colunas de horas/minutos com rolagem circular, nome | Fluxo de interface; rolagem e persistência |
| Nome/rótulo do alarme | Editor e tela de disparo | Salvar e reabrir |
| Todos os dias / dias específicos / uma vez | Máscara semanal, horário local | SchedulerTest, incluindo mudança de dia e fuso |
| Contagem até o próximo disparo | Próxima ocorrência calculada | SchedulerTest |
| Ativar/desativar | Switch e cancelamento no AlarmManager | Persistência e fluxo Android |
| Excluir | Confirmação e remoção do agendamento | Fluxo Android |
| Prévia do alarme | Sessão separada e saída própria | Não deve consumir recorrência nem histórico real |
| Pular uma vez | skipUntil da próxima ocorrência | Teste de agenda e interface |
| Duplicar | Cópia independente, novo identificador | Fluxo Android |
| Até cinco missões | Sequência, remoção individual | Limite no editor e avanço controlado |
| Cadastrar QR/código de barras | ZXing pela câmera, biblioteca local reutilizável | Cadastro pelo driver de câmera do emulador |
| Leitor vertical com enquadramento e lanterna | Preview integral com máscara externa, retângulo central e controle de flash | QR e EAN-13 via câmera; orientação e lanterna no emulador |
| Adicionar vários códigos e selecionar os aceitos | Uma missão aceita qualquer código selecionado | Seleção múltipla e persistência |
| Revisar e excluir código | Menu individual, valor completo e confirmação de exclusão | Exclusão da biblioteca não altera outros alarmes salvos |
| Testar código antes de salvar | Testar leitura, sem iniciar alarme | Leitura pela câmera e confirmação visual |
| Encerrar somente com código cadastrado | Leitura real e validação contra referência | Código incorreto e cancelamento não concluem missão |
| Matemática | Perguntas e validação de resposta | Lógica e interface |
| Digitação | Conferência exata da frase | Lógica e interface |
| Encontrar cor | Seleção visual de cor correspondente | Lógica e interface |
| Passos | Sensor de passos do Android | Lógica; caminhada física pendente; informa sensor ausente |
| Sacudir | Acelerômetro com limiar e intervalo mínimo | Lógica; movimento físico pendente |
| Foto | Cadastro de referência e comparação visual local | Hash e rejeição de imagem plana testados; cadastro/comparação pela câmera pendentes |
| Agachamento | Pose ML Kit e ciclo de flexão/extensão do joelho | Ciclo lógico testado; detecção por imagem e execução corporal pendentes |
| Encontrar objeto doméstico | Reconhecimento local de copo, livro, garrafa, cadeira ou planta com ML Kit | Implementado; reconhecimento do objeto ainda não validado com cena real |
| Falar no ritmo | Reconhecimento de voz + janela temporal | Normalização da palavra testada; ritmo e microfone ainda pendentes |
| Confirmação de despertar | Novo chamado após intervalo | Disparo da confirmação e regressão contra repetição infinita testados |
| Escolher áudio personalizado | Seletor de documento com acesso persistente | WAV selecionado pelo seletor Android; acesso persistiu após atualizar o APK; reprodução verificada |
| Ouvir áudio antes de salvar | Prévia com duração limitada | Botão de amostra acionado; MediaPlayer iniciou e encerrou o WAV de 5 segundos |
| Volume | Volume por alarme | Configuração e serviço; percepção sonora física pendente |
| Vibração | Vibrator | Configuração testada; vibração física pendente |
| Despertar gradual | Rampa de volume | Serviço e configuração |
| Falar a hora | TextToSpeech | Configuração testada; reprodução audível pendente |
| Falar o clima | Open-Meteo + TextToSpeech | Consulta e erro testados com rede real; voz física pendente |
| Falar rótulo | TextToSpeech | Configuração testada; reprodução audível pendente |
| Amostras de voz | Botão de exemplo de hora/clima/nome | Implementado; teste audível de TTS pendente |
| Efeito extra alto | Volume máximo do fluxo de alarme | Limitado ao volume físico do aparelho, sem promessa de amplificação além do hardware |
| Soneca: intervalo e quantidade | Limite por sessão | Estado e agenda Android |
| Papel de parede | Aurora/Oceano/Noite ou documento escolhido | Oceano testado; PNG selecionado, persistido e exibido na prévia com contraste verificado |
| Salvar / cancelar | Persistência ou descarte explícito | Fluxo Android |
| Otimização de alarme | Status e atalhos de permissões exatas, notificações, tela cheia e bateria | Permissões controladas pelo Android |
| Ajustes avançados / prevenção de fuga | Sessão ativa, missões e informações de proteção | Forçar parada e revogação de permissão continuam possíveis |
| Impedir desligamento | Limite explicitado na tela | **Não implementável como bloqueio garantido em APK comum** |
| Proteção contra desinstalação | Administrador opcional, ativação/revogação pelo Android | Ativação no consentimento Android e remoção verificadas no emulador (dumpsys device_policy) |
| Tema | Claro, escuro, sistema | Persistência e interface |
| Saída de som | Aparelho atual / alto-falante | Preferência de rota Android; Bluetooth físico pendente |
| Conta/Pro/anúncios | Excluídos conforme decisão do usuário | Nenhuma cobrança/login implementados |
| Sono/Manhã/Relatório | Excluídos conforme decisão do usuário | Teste verifica ausência das abas |

## Restrições de confiabilidade

O APK não toca com o telefone desligado. O Android e o fabricante podem interromper aplicativos, revogar permissões e restringir execução em segundo plano. Após instalar, conceda os acessos indicados e experimente um alarme curto no próprio aparelho. A leitura de códigos depende de foco, iluminação e permissão de câmera. Foto e objeto usam reconhecimento aproximado, sujeito a falsos resultados; não são mecanismos de identidade ou segurança.

## Execução dos testes

O resumo final de execução e as capturas ficam em `docs/TESTES.md`. Os testes automatizados estão em `app/src/test` e `app/src/androidTest`.
