# VENDEPASS: Venda de Passagens
## Introdução
**(contexto)**, **(problema)**, **(contribuição)**, **(metodologia)**, **(resultados)**.

## Como a arquitetura foi desenvolvida? Quais os componentes e seus papeis nessa arquitetura?
  A arquitetura foi desenvolvida com base no modelo de comunicação TCP/IP, utilizando a linguagem de programação Java. Para implementar essa comunicação, foi utilizada a API de sockets, que é responsável por criar conexões entre o cliente e o servidor, permitindo o envio e recebimento de dados. A comunicação é estruturada sobre o protocolo TCP/IP, que garante uma transmissão confiável e sequencial dos dados, atendendo aos requisitos do projeto. 
  O protocolo TCP (Transmission Control Protocol) assegura que os dados sejam entregues de forma íntegra, ordenada e sem duplicação, graças à sua funcionalidade de controle de erros e retransmissão em caso de falhas. O IP (Internet Protocol) é responsável por direcionar os pacotes de dados ao seu destino correto, utilizando endereços IP para a identificação única de cada dispositivo na rede. Essa combinação oferece uma boa solução para sistemas que precisam realizar a troca de informações de forma confiável. Como o sistema de reserva de passagens desenvolvido neste projeto, tendo em vista a necessidade de controlar as vendas que são realizadas.

Na questão de modularização, foram implementadas duas classes principais, Cliente e Servidor, cada uma com sua funcionalidade específica para atender os processamentos do sistema. Abaixo, segue um detalhamento das classes e suas funções:

- Cliente:
	- Main: É feita a maior parte da comunicação com o servidor. Na main é iniciado o canal e feita a inserção do CPF por parte do cliente. A partir da validação, que é realizada pelo servidor verificando a integridade do CPF, o CPF é enviado para ativar a conexão e processar os outros módulos, como o Menu, que é chamado após a confirmação para exibir as opções ao cliente.
	- Menu: Descreve um menu que será visualizado pelo cliente do sistema. O menu contém as opções de realizar compra e sair do sistema.
	- iniciarCompra: Após a seleção de compra, é enviada uma mensagem ao servidor solicitando uma lista de cidades disponíveis para viagem. O cliente seleciona a origem e o destino e envia ao servidor, então o servidor devolve as rotas disponíveis. O cliente escolhe a rota, e a compra é realizada.
	- Enviar_mensagem e Receber_mensagem: Módulos que fazem a comunicação com o servidor. São chamados sempre que for necessário trocar mensagens, realizando a transformação de mensagens em bits para envio ao servidor e a leitura dos bits, transformando-os em strings para tratamento no cliente.
	- Erro_comunicação: Encerra a comunicação com o servidor, caso ocorra alguma interrupção por parte do cliente.




## Que paradigma de serviço foi usado (stateless ou statefull)? Qual(is) o(s) motivo(s) dessa escolha?

## Que protocolo de comunicação foi desenvolvido entre os componentes do sistema? Quais as mensagens e a ordem das mensagens trocadas.

## Que tipo de formatação de dados foi usada para transmitir os dados, permitindo que nós de diferentes sistemas e implementadas em diferentes linguagens compreendam as mensagens trocadas.

## O sistema permite a realização de compras de passagens de forma paralela ou simultânea? Como é possivel otimizar o paralelismo do sistema.

## Há problemas de concorrência decorrentes do uso de conexões simultâneas? Se sim, como estas questões foram tratadas?

## Tirando e recolocando o cabo de algum dos nós, o sistema continua funcionando? Ele continua podendo fazer a compra que iniciou anteriormente?

## O sistema utiliza algum mecanismo para melhorar o tempo de resposta? Como você avaliou o desempenho do seu sistema? Fez algum teste de desempenho?

## Docker adicionado ao sistema e como executar o sistema.

## Conclusão

## Referências
