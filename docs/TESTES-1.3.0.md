# Desperta 1.3.0 — validação

Ambiente: emulador Pixel 7 ARM64, Android 15 / API 35. Aplicativo assinado com a mesma chave das versões anteriores. As verificações de câmera usam uma imagem de QR fornecida ao driver de câmera do emulador; o resultado não é injetado no callback do leitor.

## Execuções

- 31 testes de lógica: aprovados, sem falhas, erros ou ignorados.
- Build de release e lint: aprovados. Assinatura APK v2/v3 conferida.
- Execução integrada: **59 testes Android aprovados**, 411,962 segundos, sem falhas ou ignorados. [Registro completo](evidence/1.3.0/android-integrated-59.txt).
- Regressão de interface após ajustes visuais: **25 testes aprovados**, 306,733 segundos; inicial, editor, fonte ampliada, horário por rolagem e quatro identidades. [Registro](evidence/1.3.0/android-ui-25.txt).
- Conferência final do APK de distribuição: **12 testes de identidades e aparência do leitor aprovados**, 119,392 segundos, depois dos últimos ajustes no cabeçalho Anos 90, fundo das janelas e menu Terminal. [Registro](evidence/1.3.0/android-final-identities-12.txt).
- Quatro capturas adicionais na resolução de cada referência: aprovadas. [Capturas reais](screenshots/1.3.0/).

## Cobertura

| Área | Verificação |
|---|---|
| Padrão Retrofuturista | Preferências vazias e legado de tema; migração e seleção persistida |
| Quatro identidades | Seleção pelos Ajustes, recriação/reabertura, inicial e superfícies principais; cores/fontes distintas |
| Criar/editar alarmes | Criar a partir da ação inicial nas quatro identidades; editar e persistir; duplicar, pular e desfazer |
| Ativação | Switches das três identidades e controle textual do Terminal persistem corretamente |
| Editor | Horário por gestos de rolagem, dias, seções, som, soneca, voz, papel de parede e limite de missões |
| Relógio digital | Pixels do relógio Anos 90 presentes e alterados quando o horário real muda de 07:30 para 08:45 |
| Fonte ampliada | Fluxos da inicial e editor em fonte ampliada; salvar e abrir opções continuam acessíveis |
| Leitor/biblioteca | Registro, seleção, revisão, exclusão, códigos errados, leitura aceita e prévia; scanner nas quatro identidades |
| Alarme e QR | Alarme tocando + leitura pela câmera + encerramento nas quatro identidades; agendamento exato e recorrência na suíte do motor |
| Missões | Configuração, ordem, candidato/experimentação e execução dos fluxos automatizados existentes |
| Atualizador | Superfície nas quatro identidades, configuração persistida, consulta pública e validação dos dados do release |
| Revisão visual | Pares de referências/capturas para agentes cegos, com relatórios explícitos de diferenças e rodadas de correção |

O teste de QR repetido por identidade inicia o serviço de alarme e verifica seu encerramento real; a entrega pelo relógio/AlarmManager é validada separadamente na suíte EngineFlowTest. Isso evita confundir disparo imediato com agendamento.

## Falhas investigadas durante o desenvolvimento

- A moldura e o relógio Anos 90 inicialmente apareciam vazios: a translação interna de TextView era aplicada ao desenho customizado. O desenho agora compensa esse deslocamento; há teste dos pixels e da mudança de horário.
- O leitor provocava relayout contínuo, impedindo a automação de atingir estado ocioso. A margem só é atualizada quando muda.
- Testes antigos procuravam o texto literal + Alarme após o ícone e o texto serem separados. Os testes passaram a usar a descrição acessível Adicionar alarme, comum às quatro identidades; a verificação adicional cria e salva um alarme em cada uma.
- A consulta meteorológica falhou uma vez por resposta externa e passou na repetição; a execução integrada de 59 testes também passou.
- A inspeção visual encontrou um fundo cinza indevido dentro do relógio Anos 90; corrigido para azul-marinho antes do APK final.

## Limites

Testes no emulador não substituem validação física de câmera Samsung, voz, vibração, movimento, passos, agachamentos, Bluetooth e reconhecimento de cenas reais. Mantêm-se os limites da [matriz geral](FUNCIONALIDADES.md).

Os revisores não certificaram igualdade pixel a pixel. Foram corrigidas diferenças de composição e defeitos identificados; rasterização de fontes, relevos, texturas e componentes nativos ainda podem diferir das imagens geradas. As barras e diálogos de instalação/permissão pertencem ao Android. Não houve estudo de usabilidade com participantes.

A [validação no GitHub Actions](https://github.com/g0dswer/Desperta/actions/runs/34166682047) passou no commit de código `a995e80`.

## Atualização pública 1.2.0 → 1.3.0

Ensaio executado com o APK público original 1.2.0, sem cliente modificado. Foi criado pela interface o alarme diário `Atualizacao preservada v130`, às 07:30. O próprio app encontrou a 1.3.0 no GitHub, baixou e conferiu o APK, abriu a permissão de instalação e o instalador do Android. Após confirmar a atualização, o alarme continuou salvo e ativado e a identidade padrão passou a Retrofuturista. Nova consulta informou que a 1.3.0 já era a versão mais recente.

O Android marcou novamente a autorização de notificações em tela cheia como pendente após a instalação. O app mostrou **Revisar permissões do alarme**; o caminho pela interface abriu a configuração correta e, após a autorização, mostrou **Pronto**. Alarmes exatos e notificações permaneceram autorizados.

O APK efetivamente instalado pelo atualizador foi extraído e seu SHA-256 coincidiu com o APK de distribuição: `38e8222d996c0ac59d3cce4f17bacd74a8451fdd3ee3a51d68989f5b7b915d86`. [Registro](evidence/1.3.0/installed-apk-verification.txt).

Capturas: [alarme na 1.2.0](screenshots/1.3.0/upgrade-01-before.png), [oferta pública](screenshots/1.3.0/upgrade-02-available.png), [download conferido](screenshots/1.3.0/upgrade-03-verified.png), [instalador](screenshots/1.3.0/upgrade-04-installer.png), [alarme preservado](screenshots/1.3.0/upgrade-05-preserved.png), [1.3.0 atualizada](screenshots/1.3.0/upgrade-06-current.png).

A validação do commit de publicação também [passou no GitHub Actions](https://github.com/g0dswer/Desperta/actions/runs/34166856758).
