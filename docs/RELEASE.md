# Desperta 1.0.0

APK Android assinado, gratuito, sem anúncios e sem login. Android 8 ou superior.

Inclui alarmes recorrentes, até cinco missões, QR/código de barras cadastrado pela câmera, matemática, digitação, cores, passos, sacudir, foto, agachamento por capturas de pose, reconhecimento de objetos e fala no ritmo. Som, voz, soneca, tema e papel de parede configuráveis.

Sono, Manhã e Relatório não fazem parte desta versão, conforme o escopo solicitado.

## Evidência

- 15 testes de lógica aprovados.
- 26 testes Android aprovados, sem falhas ou ignorados.
- O APK assinado foi instalado e os testes Android foram repetidos sobre ele.
- Cadastro e comparação de QR testados pelo driver de câmera do emulador e ZXing, incluindo rejeição do código diferente e encerramento do alarme real com o código correto.
- Build de release, Android Lint e assinatura v2/v3 verificados.

## Limites

Esta é uma pré-release: sensores físicos, microfone/ritmo, reconhecimento visual em cenas reais, vibração e Bluetooth ainda precisam de validação em telefone. Não há garantia de impedir desligamento ou parada forçada pelo Android. As permissões necessárias são apresentadas em Ajustes.

A [matriz de funcionalidades](https://github.com/g0dswer/Desperta/blob/main/docs/FUNCIONALIDADES.md) informa implementação e cobertura de teste individual. O [relatório de testes](https://github.com/g0dswer/Desperta/blob/main/docs/TESTES.md) lista os casos executados e as lacunas.

Baixe `Desperta-1.0.0.apk`. O arquivo `SHA256SUMS.txt` permite conferir a integridade do download.
