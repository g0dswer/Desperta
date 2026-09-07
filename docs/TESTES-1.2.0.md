# Desperta 1.2.0 — Amanhecer e atualizações

Ambiente: Android 15 / API 35, emulador Pixel 7 ARM64. APK de produção assinado com a mesma chave das versões anteriores. O relatório distingue testes executados no emulador de comportamento ainda não avaliado em aparelho físico.

## Resultado

- 31 testes locais de lógica aprovados.
- Primeira execução integrada: 43 testes Android aprovados, sem falhas ou ignorados.
- Execução final do APK assinado: **48 testes Android aprovados**, sem falhas nem ignorados (288,443 segundos).
- Repetição da suíte de câmera com EAN-13: 5 testes aprovados (20,374 segundos).
- Alarme agendado com tela protegida por PIN, leitura pela câmera e encerramento: 1 teste aprovado (9,275 segundos).
- Build de release, lint e assinatura v2/v3: aprovados.
- Download público e atualização pelo instalador: **aprovados**. O próprio app baixou o APK publicado, conferiu o arquivo, abriu a permissão de instalação e o instalador do Android; o alarme foi preservado.

## Escopo aprovado e evidências

| Item | Implementação | Evidência |
|---|---|---|
| 1 — Amanhecer | Azul profundo, branco quente, âmbar e ícone de sol no horizonte; retirada do selo gratuito | Capturas reais do APK; tema claro/escuro e fonte ampliada na validação final |
| 2 — Inicial | Próximo alarme, cartões compactos, toque para editar, menu separado, Ajustes no cabeçalho e criar alarme fixo | Criar, editar, duplicar, ativar/desativar e persistir via UI |
| 3 — Editor | Som, soneca, voz, aparência e missão organizados em seções; salvar fixo e resumo das configurações | Todos os controles de som/voz/soneca, papel de parede e limite de missões salvos e reabertos |
| 4 — Horário | Colunas circulares editáveis, feedback tátil, dias legíveis e atalhos de repetição | Gestos reais nas duas colunas, presets e persistência; vibração física pendente |
| 5 — Excluído | Biblioteca mantém códigos, seleção, revisão e exclusão; sem nomes de objetos ou aliases | Cinco testes de regressão da biblioteca |
| 6 — Leitor | Código incorreto mantém câmera aberta; código aceito retorna imediatamente; instrução contextual e lanterna rotulada | QR pela câmera do emulador, código errado, seleção múltipla, orientação, enquadramento e estado da lanterna |
| 7 — Alarme | Uma ação principal contextual, contador só para várias etapas, sonecas restantes e confirmação de encerramento | Alarme real encerrado pela câmera; conclusão única, prévia e soneca |
| 8 — Missões | Grupos por objetivo, descrição, configurar/experimentar antes de adicionar e reordenação | Candidato experimentado sem adição automática; ordem e conteúdo persistidos |
| 9 — Conveniências | Pular com data, desfazer, modelos pessoais, duplicar e alterar só a próxima ocorrência | UI e testes de entrega real/recorrência; horário antecipado suprime também o horário original |
| 10 — Conforto | Singular/plural, descrições acessíveis, botões principais amplos, tema claro/escuro e suporte a fonte ampliada | Interação e captura com fonte 130%; auditoria completa por TalkBack não realizada |
| Atualizações | Consulta diária opcional ao abrir, consulta manual, filtro de pré-lançamentos, aviso na inicial, download e confirmação no Android | Comparação de versões, respostas inválidas, cache, consulta pública real, download e instalação aprovados |

## Registros

Os registros desta versão ficam em `docs/evidence/`, com os resultados e o SHA-256 do APK. A consulta de atualizações usa exclusivamente a lista pública de releases de `g0dswer/Desperta`, sem autenticação. O arquivo baixado é conferido por pacote, versão superior e certificado de assinatura; o checksum publicado também é verificado quando disponível.

O ensaio de atualização usa um cliente de QA com versão anterior e o mesmo código do atualizador, assinado pela mesma chave, para permitir observar uma atualização real para o APK publicado. Esse cliente de QA não é distribuído no release.

## Atualização real pelo aplicativo

O cliente de QA 1.1.9 (versionCode 2), com o mesmo atualizador e assinatura, encontrou o release público 1.2.0. Pela interface, foi criado o alarme `Atualizacao preservada`, diário às 07:30. O botão **Baixar atualização** transferiu o APK do GitHub; o app confirmou a integridade, o pacote, a versão superior e a assinatura antes de oferecer **Instalar atualização**.

Após permitir instalações dessa fonte e confirmar **Update** no instalador Android, o PackageManager informou 1.2.0 (versionCode 3). O alarme permaneceu na inicial e foi encontrado como `RTC_WAKEUP` exato no agendador do Android. O APK instalado foi extraído e seu SHA-256 coincidiu com o APK publicado e testado: `f18fc06d5b4d72862ec95034ad5ab450585c69262a90651cf02ca4f6b72dc3ff`. Uma nova consulta informou que a versão instalada era a mais recente.

Capturas: [oferta](screenshots/1.2.0/06-atualizacao-disponivel.png), [download conferido](screenshots/1.2.0/07-download-conferido.png), [confirmação Android](screenshots/1.2.0/08-confirmacao-android.png), [alarme preservado](screenshots/1.2.0/09-alarme-preservado.png) e [versão instalada](screenshots/1.2.0/10-versao-atualizada.png).

A [compilação e os testes no GitHub Actions](https://github.com/g0dswer/Desperta/actions/runs/34158879826) também passaram no commit de código publicado.

## Limites

A câmera do emulador recebe imagens de QR/código de barras e passa pelo driver de câmera e pelo ZXing; os testes não injetam o resultado da leitura. Isso não substitui uma câmera Samsung física. Caminhada, movimentos reais, agachamento, fala, vibração, Bluetooth e reconhecimento de fotos/objetos em cenas reais continuam com os limites documentados na [matriz geral](FUNCIONALIDADES.md).

Os testes de interface são tarefas de uso executadas por automação e inspeção visual. Não houve estudo de usabilidade com participantes; não há uma medição comparativa de tempo ou taxa de erro de usuários.
