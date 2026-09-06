# Testes executados

Validação em emulador Android 15 (API 35), Pixel 7 ARM64. Não houve acesso a um telefone físico.

**Resultado: 15 testes locais e 26 testes Android aprovados, zero falhas e zero ignorados.** `lintDebug` e `assembleRelease` também passaram.

O teste de câmera usa a câmera do emulador alimentada por uma imagem de QR. A imagem atravessa o driver de câmera e o leitor ZXing do aplicativo; o teste não injeta o resultado da leitura. Os três cenários validam cadastro, código diferente rejeitado e encerramento de uma sessão real com o código correto. QR, EAN-13 e Code 128 também passam pelo decodificador de pixels em testes locais.

## Casos executados

| Grupo | Caso | Resultado |
|---|---|---|
| unit | `zeroMaskIsAOneShotClockOccurrence` | passed |
| unit | `springDstGapNormalizesToTheFirstValidLocalTime` | passed |
| unit | `skipUntilSuppressesExactlyOneMatchingOccurrence` | passed |
| unit | `dailyAlarmUsesTheNextFutureClockOccurrence` | passed |
| unit | `fallDstOccurrenceIsStrictlyAfterTheInput` | passed |
| unit | `sundayBitZeroSelectsSundayAndSkipsOtherDays` | passed |
| unit | `photoSignatureNeedsDetailAndMatchesSimilarHashes` | passed |
| unit | `typingMathSpeechAndLabelsAreVerified` | passed |
| unit | `colorAndShakeChecksRejectWrongInputAndDebounce` | passed |
| unit | `squatRequiresDownThenUp` | passed |
| unit | `barcodeRequiresExactPayload` | passed |
| unit | `progressIsBoundedAndRequiresAllRepetitions` | passed |
| unit | `code128CameraPixelDecoderPreservesLeadingZeros` | passed |
| unit | `qrCameraPixelDecoderPreservesExactPayload` | passed |
| unit | `ean13CameraPixelDecoderPreservesProductCode` | passed |
| android | `createEditPersistDuplicateSkipDelete` | passed |
| android | `tabsExcludedAndSettingsOpen` | passed |
| android | `allAlarmSwitchesSnoozeWallpaperAndMissionLimitPersist` | passed |
| android | `discardDoesNotCreateAlarm` | passed |
| android | `editorMissionAndOptionsPersist` | passed |
| android | `enableSwitchPersistsBothDirections` | passed |
| android | `actualBarcodeMissionStopsRealAlarmOnlyAfterCameraScan` | passed |
| android | `registerFromCameraAndMatchRegisteredCode` | passed |
| android | `wrongCameraCodeDoesNotCompleteMission` | passed |
| android | `overlappingScheduledDeliveriesAreQueuedAndThenDrained` | passed |
| android | `previewServiceDoesNotWriteHistoryOrDisableAlarm` | passed |
| android | `wakeCheckPendingIntentIsDeliveredAsASeparateRealTrigger` | passed |
| android | `snoozedSessionsRemainKeyedWhenActiveSessionChanges` | passed |
| android | `scheduledDeliveryStartsARealForegroundSessionAndDismissLogsIt` | passed |
| android | `snoozeLimitGuardPreventsClosingTheRingUi` | passed |
| android | `previewUsesAlarmAttributesAndRestoresExtraLoudVolumeAndRoute` | passed |
| android | `alarmPendingIntentsKeepNormalSnoozeWakeAndPreviewSeparate` | passed |
| android | `exactScheduleCanBeInstalledAndCancelledWhenPermissionIsAvailable` | passed |
| android | `wrongMissionResultLeavesCursorAndRingActive` | passed |
| android | `snoozePersistsCountAndMissionCursor` | passed |
| android | `colorMissionRequiresRequestedTile` | passed |
| android | `mathRejectsWrongThenAcceptsCorrect` | passed |
| android | `typingRejectsWrongInputAndCompletesExactText` | passed |
| android | `settingsPersistThemeAndOutputChoices` | passed |
| android | `weatherRefreshUsesRealGeocodingAndForecast` | passed |
| android | `weatherRefreshReportsInvalidCityAndClearsTimestamp` | passed |

## Reproduzir

```sh
python3 -m pip install 'qrcode[pil]'
python3 scripts/camera-fixture.py /tmp/desperta-correct.png
emulator -avd Desperta_API35 -camera-back imagefile:/tmp/desperta-correct.png -no-snapshot
./gradlew :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest :app:assembleRelease
```

O AVD usado é Pixel 7, imagem `system-images;android-35;google_apis;arm64-v8a`. A posição do QR no arquivo foi calibrada para o recorte dessa câmera. Em outro aparelho, execute os testes sem o fixture com `-Pandroid.testInstrumentationRunnerArguments.notClass=com.desperta.CameraPipelineTest` e faça a leitura de um código físico separadamente.

## Limites da evidência

- **Não validados em aparelho físico:** foco/iluminação da câmera; caminhada, agitação e agachamentos; reconhecimento de objetos/fotos em cenários reais; microfone/ritmo e voz audível; vibração, Bluetooth e volume percebido.
- Foto usa comparação visual aproximada. Agachamento usa capturas sucessivas de corpo inteiro, verificando flexão e extensão; não equivale a acompanhamento contínuo por vídeo.
- Seletor de áudio/imagem personalizada têm implementação, mas o percurso completo com documentos reais ainda requer teste manual.
- O disparo agendado, soneca, fila, confirmação de despertar, atributos de áudio, restauração de volume, persistência e missões descritas nos casos acima foram exercitados pelo Android do emulador. Isso não prova o comportamento de economia de bateria de cada fabricante.

Consulte a [matriz completa das funções](FUNCIONALIDADES.md) para separar implementação de cobertura de teste.

## Verificação manual do administrador

No emulador, abri Ativar proteção, aceitei a tela de administrador do Android, confirmei `com.desperta/.ProtectionAdmin` em `dumpsys device_policy`, usei Remover e confirmei que o administrador desapareceu. O recurso é opcional e reversível.
