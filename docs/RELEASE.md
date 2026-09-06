# Desperta 1.1.0

Atualização gratuita, sem anúncios e sem login. Instale o APK sobre a versão anterior: o pacote e a chave de assinatura são os mesmos.

## Mudanças solicitadas

- QR/código de barras: a conclusão aceita pelo scanner é enviada diretamente ao serviço do alarme, com proteção contra callbacks duplicados ou atrasados. O scanner também permite exibição sobre a tela bloqueada.
- Respostas atrasadas após encerrar o alarme são descartadas sem reiniciar o serviço.
- Leitor vertical como a referência: câmera em tela cheia, retângulo central, área externa escurecida, instrução em português e lanterna.
- Horário: colunas de horas e minutos com rolagem circular, em vez do relógio em disco.
- Biblioteca de códigos: adicionar pela câmera, cadastrar vários, selecionar os aceitos, revisar o valor completo, excluir e testar a leitura antes de concluir. Qualquer código selecionado conclui a missão; códigos diferentes não encerram o alarme.
- Compatibilidade com os códigos e alarmes da versão 1.0.0.

## Validação

Os resultados e os cenários executados estão no [relatório da versão 1.1.0](https://github.com/g0dswer/Desperta/blob/main/docs/TESTES-1.1.0.md). A validação usa Android 15 em emulador e câmera alimentada com uma imagem de QR, passando pelo driver de câmera e pelo ZXing.

O aparelho Samsung do relato não esteve disponível para reproduzir sua falha específica. Sensores físicos, voz, vibração, Bluetooth e reconhecimento visual em cenas reais continuam com a cobertura limitada descrita na [matriz de funcionalidades](https://github.com/g0dswer/Desperta/blob/main/docs/FUNCIONALIDADES.md). Mantida a classificação de pré-release.

Baixe `Desperta-1.1.0.apk`. A integridade pode ser conferida com `SHA256SUMS.txt`.
