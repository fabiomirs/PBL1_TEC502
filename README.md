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

- Servidor:
	- Main: Inicia o servidor e configura o ambiente de rede para aceitar conexões de clientes, utilizando ServerSocketChannel e Selector. O servidor opera de forma não bloqueante, aguardando eventos como novas conexões e leituras de dados dos clientes. Também controla a remoção de clientes desconectados ou com erro.
	- Validar e registrar CPF: Valida se o CPF enviado pelo cliente já está em uso. Caso esteja, solicita um novo CPF. Se o CPF for válido, associa o CPF à conexão específica e envia uma confirmação ao cliente.
	- Processar requisição: Faz a troca de mensagens com o cliente durante o processo de compra. Envia as cidades disponíveis, trata a resposta do cliente com as rotas disponíveis e processa a compra final.
	- Iniciar_compra: Exibe uma lista de cidades disponíveis para a compra de passagens.
	- Escolher_cidades: Permite ao cliente selecionar a cidade de origem e destino, exibindo as rotas disponíveis usando o algoritmo BFS (Busca em Largura).
	- Escolher_rota: Processa a compra de passagens, verificando a disponibilidade e atualizando os dados.
	- EncontrarRotasBFS: Usa o algoritmo BFS para encontrar todas as rotas possíveis entre a origem e o destino. As rotas são retornadas como uma lista de listas, contendo o caminho das cidades.
	- ObterIndiceNo: Auxilia na busca de um índice baseado no nome da cidade (nó) dentro do mapa de trechos.
	- Ler_cidades: Lê os dados de cidades e trechos de um arquivo JSON, carregando-os em um ConcurrentHashMap.
	- Atualizar_arquivo_cidades: Atualiza o arquivo JSON com as informações atuais de trechos sempre que há mudanças nas rotas ou passagens.
	- Organiza_registro: Armazena a rota comprada e a data/hora da compra no registro do cliente.
	- Arquivar_registro_cliente: Salva o registro de compras em um arquivo JSON, armazenando o histórico de cada CPF.

	O projeto utiliza NIO (Non-blocking I/O) para otimizar o processo de leitura e escrita de dados, permitindo maior escalabilidade e eficiência. O NIO (Non-blocking I/O) é uma API que permite a leitura e escrita de dados de forma não bloqueante. Ou seja, o programa pode continuar executando outras tarefas enquanto aguarda as operações de I/O serem completadas, sem a necessidade de ficar esperando a conclusão de cada uma. Isso resulta em uma maior eficiência e escalabilidade, especialmente em sistemas que precisam lidar com múltiplas conexões simultâneas, como servidores de rede.

	No NIO, os principais componentes são os Channels, Buffers e o Selector. Um Channel é um caminho através do qual dados podem ser lidos ou escritos. Diferente de streams tradicionais, um canal pode ser tanto de entrada quanto de saída ao mesmo tempo, permitindo maior flexibilidade. Os dados que transitam por um canal são armazenados em Buffers, que são regiões da memória utilizadas para armazenar temporariamente as informações lidas ou a serem escritas. Isso possibilita o controle preciso sobre o que está sendo lido ou escrito a qualquer momento.

	O Selector é o componente chave da natureza não bloqueante do NIO. Ele permite que um único thread monitore múltiplos canais para eventos como operações de leitura, escrita ou novas conexões. O thread pode então responder a esses eventos à medida que ocorrem, sem ficar preso esperando por operações de I/O de um único canal, aumentando a eficiência do sistema. Ao trabalhar dessa forma, o NIO torna possível escalar o número de conexões e manipular um grande volume de dados de forma mais eficiente, usando menos recursos do sistema, o que é essencial em servidores de alto desempenho.


## Que paradigma de serviço foi usado (stateless ou statefull)? Qual(is) o(s) motivo(s) dessa escolha?

	O servidor foi implementado seguindo o paradigma de serviço stateful, o que significa que ele mantém o estado do cliente ao longo das interações. Por exemplo, quando o cliente quer comprar uma passagem, primeiro ele indica seu CPF, o servidor analisa o conteúdo, verifica se não está conectado por outro cliente e libera o acesso. Em seguida, é feita uma troca de mensagens, que funciona da seguinte forma:
 
 - o servidor envia uma lista de cidades disponíveis, o cliente seleciona a origem e o destino e envia ao servidor, o servidor devolve as rotas, e o cliente, por fim, seleciona qual deseja comprar. Esse ciclo de guardar estado entre interações é o que caracteriza um servidor stateful.

	A escolha por um servidor stateful foi feita para realizar um melhor gerenciamento das informações dos clientes. De modo que o servidor pode associar cada solicitação a um registro específico. Além disso, como o servidor possui interações que dependem do envio de dados por parte do cliente, essa abordagem permite um controle mais preciso sobre as operações realizadas.


## Que protocolo de comunicação foi desenvolvido entre os componentes do sistema? Quais as mensagens e a ordem das mensagens trocadas.

	As mensagens são transmitidas como strings, formatadas com um cabeçalho que indica a operação a ser realizada pelo servidor e um corpo que contém os dados necessários, como a cidade de origem ou destino. 
 
 Os principais protocolos desenvolvidos para a comunicação são:
	- "Iniciar_compra": Indica ao servidor que a compra foi iniciada e solicita as cidades disponíveis.
	- "Escolher_cidades": Enviado pelo cliente, contendo a cidade de origem e a cidade de destino.
	- "Escolher_rota": Contém a rota escolhida e a cidade de destino final.
Com base nesses protocolos, o servidor interpreta as mensagens e executa ações correspondentes, como listar cidades ou rotas.
A ordem de troca de mensagens entre cliente e servidor segue o fluxo apresentado na imagem abaixo:

<p align="center"><strong>Componentes da De1-Soc. </strong></p>
<p align="center">
  <img src="Arquivos das sessões/Diagrama de sequência cliente-servidor.png" width = "400" />
</p>
<p align="center"><strong>
</strong></p>


## Que tipo de formatação de dados foi usada para transmitir os dados, permitindo que nós de diferentes sistemas e implementadas em diferentes linguagens compreendam as mensagens trocadas.

O tipo de formatação utilizado para a troca de dados é String, estruturada em duas partes: um cabeçalho, que contém o protocolo indicando a operação a ser realizada, e um corpo, que contém os dados em si. Essa abordagem garante que as mensagens possam ser compreendidas por sistemas implementados em diferentes linguagens, já que as strings são uma estrutura de dados amplamente suportada.

## O sistema permite a realização de compras de passagens de forma paralela ou simultânea? Como é possivel otimizar o paralelismo do sistema.

	O sistema permite seu funcionamento de forma simultânea, ou seja, vários usuários podem realizar compras e acessar o sistema ao mesmo tempo. O que possibilita esse tratamento é o uso da API NIO (Non-blocking I/O), que faz parte da plataforma Java e foi projetada para suportar operações assíncronas e não bloqueantes, o que facilita a execução de tarefas simultâneas.
 
	O NIO moderno utiliza canais e buffers para lidar com dados de maneira eficiente. Diferente de operações bloqueantes, onde o sistema precisa esperar a conclusão de uma tarefa antes de iniciar outra, no NIO, as operações de leitura e escrita podem ser realizadas de forma assíncrona, permitindo que várias operações ocorram ao mesmo tempo. Isso é fundamental para cenários onde múltiplos usuários acessam o sistema simultaneamente, como na venda de passagens.
 
	Os canais (Channels) são os principais responsáveis por estabelecer conexões para leitura e escrita de dados. Eles operam de forma não bloqueante, ou seja, o sistema pode continuar executando outras tarefas enquanto os dados estão sendo processados. Isso permite que várias requisições de diferentes usuários sejam atendidas ao mesmo tempo, sem que uma operação precise esperar pela conclusão de outra.
 
	Os buffers atuam como áreas temporárias de armazenamento de dados enquanto eles são manipulados pelos canais. Eles otimizam o fluxo de dados, permitindo que o sistema lide com grandes volumes de informações de forma eficiente e não bloqueante.
 
	Esses componentes do NIO permitem que o sistema funcione de maneira altamente paralela, suportando múltiplos acessos simultâneos e garantindo que as operações de venda de passagens possam ser realizadas por vários usuários ao mesmo tempo, sem causar lentidão ou interrupções no sistema.


## Há problemas de concorrência decorrentes do uso de conexões simultâneas? Se sim, como estas questões foram tratadas?

Como o NIO possibilita que as operações de leitura e escrita sejam realizadas de forma assíncrona e permite que várias operações ocorram ao mesmo tempo, não houve problemas de concorrência no nível de I/O. Isso ocorre porque os próprios canais tratam essas operações de forma isolada, sem a necessidade de um controle centralizado para gerenciar o fluxo de dados. Com isso, o acesso simultâneo foi possibilitado de forma eficiente. Entretanto, pode ocorrer um problema com dados compartilhados, como as listas referentes à venda de passagens, porque, caso a atualização da venda seja feita em outro momento, pode gerar uma inconsistência. Por isso, foi implementado de forma que haja um controle no mesmo instante da realização da compra: se um cliente comprar uma passagem enquanto outro está com a aba de passagens disponíveis aberta, esse valor não será refletido imediatamente. Entretanto, se for a última passagem, o cliente será informado de que a venda não foi possível devido à indisponibilidade de passagens.

## Tirando e recolocando o cabo de algum dos nós, o sistema continua funcionando? Ele continua podendo fazer a compra que iniciou anteriormente?

Caso um cliente seja desconectado durante uma compra, sua conexão com o servidor é desfeita, e todo o seu histórico momentâneo no servidor é perdido. Dessa forma, apenas as transações confirmadas são computadas e salvas no servidor. Ou seja, se o cliente já tiver feito uma compra e quiser realizar outra, mas durante esse intervalo sua conexão for perdida, a compra anterior não será perdida, pois já foi computada. No entanto, independentemente de onde ele parou na compra posterior, se essa compra não tiver sido confirmada, ela não será atualizada no servidor. Com isso, caso ocorra disputa por uma última passagem, dentre um cliente A que já realizou uma compra antes e um cliente B que está fazendo sua primeira compra, e no meio da compra da passagem, a conexão do cliente A é perdida, o cliente B consegue comprar, pois a prioridade é de quem confirmar a comprar primeiro.

## O sistema utiliza algum mecanismo para melhorar o tempo de resposta? Como você avaliou o desempenho do seu sistema? Fez algum teste de desempenho?

## Docker adicionado ao sistema e como executar o sistema.

## Conclusão

## Referências
