# production-execution-service

Responsável pelo diagnóstico e pela execução da OS, pelos catálogos próprios de labor/supply, estoque e monitoring. O `os-service` é o dono dos itens e dados da OS; esta aplicação encaminha essas alterações por REST. O `billing-service` decide aprovação/pagamento.

## Fluxo

### Organização da execução

- `ExecutionWork`: estado operacional local, datas/status, `correlationId` e itens necessários para manter uma reserva de estoque.
- `ServiceOrderExecutionService`: coordena diagnóstico, aprovação, estoque, execução e monitoring. Métodos como `receiveServiceOrderForDiagnosis`, `startDiagnosis` e `finishServiceOrderExecution` descrevem a ação realizada.
- `ExecutionWorkRepository`: interface de domínio usada pela service. `ExecutionWorkRepositoryGateway` acessa o Mongo e utiliza `ExecutionWorkDocumentMapper` para converter o snapshot sem disparar uma transição ao carregar o registro.
- `ExecutionPublisher`: envia payloads específicos por fila e o `correlationId` como atributo da mensagem SQS, após o commit da transação ativa.

Os nomes das classes persistidas, a collection `execution_work`, as rotas HTTP e os contratos JSON foram preservados nesta refatoração. Logs de duração, monitoring, exceções e configurações New Relic continuam no fluxo.

1. `eventForDiagnostics` (SQS) → persiste snapshot `RECEBIDA`. **Receber o evento não inicia diagnóstico.**
2. `POST /execution/{id}/diagnosis/start` → valida existência/estado local, grava `EM_DIAGNOSTICO`, mecânico e data, envia `eventDiagnosis` diretamente ao SQS.
3. O mecânico adiciona/remove labor e supply e altera informações da OS por chamadas REST encaminhadas ao `os-service`, com o mesmo Bearer JWT da requisição. Os catálogos continuam neste serviço.
4. `POST /execution/{id}/submit-for-approval` → reserva insumos, grava `AGUARDANDO_APROVACAO` e envia `eventDiagnosisCompleted` diretamente à fila SQS configurada. O monólito consultado não possui consumidor SQS desse evento; a esteira/serviço consumidor precisa estar disponível fora daquele repositório.
5. `topicAprovacao` (SNS) → `productionApproval` (SQS, RawMessageDelivery=true) → recebe no corpo somente `serviceOrderId` e `statusPayment`; `correlationId` vem no atributo SQS. Esta peça atualiza o snapshot local e mantém a reserva de estoque quando aprovada; o estado oficial da OS pertence ao `os-service`.
6. `POST /execution/{id}/start` ou início do primeiro labor → grava `EM_EXECUCAO`, consome reserva e emite `eventExecutionStarted` para manter o os-service atualizado.
7. Início/fim individual de labor é encaminhado ao `os-service`; a resposta desse serviço alimenta o histórico local de monitoring.
8. `POST /execution/{id}/finish` → consulta a OS no `os-service`, aplica a regra do monólito de não finalizar com labor não rejeitada sem início/fim, chama `update-status?status=FINALIZADA`, grava o snapshot e envia `eventForOsCompleted` ao SQS.

Não há acesso ao banco do `os-service`. O `correlationId` recebido no atributo da mensagem `eventForDiagnostics` é persistido até o mecânico iniciar o diagnóstico.

## Endpoints

Todos os endpoints de negócio exigem Bearer JWT. Swagger e `/actuator/health` são públicos.

| Método | Caminho | Corpo/efeito |
|---|---|---|
| GET | `/execution` | OSs ativas ordenadas por prioridade/data, como no monólito |
| GET | `/execution/{id}` | Snapshot de execução |
| POST | `/execution/{id}/diagnosis/start` | Mecânico recebido como principal autenticado; o filtro valida o JWT e lê `mechanicId`/`mechanicName` das claims (ou de `roles`); sem corpo |
| POST | `/execution/{id}/labors` | `{"laborId":"l1"}`; dados/preço vêm do catálogo |
| DELETE | `/execution/{id}/labors/{laborId}` | Encaminha remoção ao `os-service` |
| POST | `/execution/{id}/supplies` | `{"supplyId":"s1","quantity":2}`; preço vem do catálogo |
| DELETE | `/execution/{id}/supplies/{supplyId}` | Encaminha remoção ao `os-service` |
| PUT | `/execution/{id}/information` | `{"informationText":"Diagnóstico realizado..."}`; encaminha ao `os-service` |
| POST | `/execution/{id}/submit-for-approval` | Conclui diagnóstico e solicita aprovação |
| POST | `/execution/{id}/start` | Inicia OS; não inventa horários de início dos labors |
| POST | `/execution/{id}/labors/{laborId}/start` | Inicia labor; primeiro labor também inicia OS |
| POST | `/execution/{id}/labors/{laborId}/finish` | Finaliza labor e registra monitoring |
| POST | `/execution/{id}/finish` | Finaliza OS |
| GET | `/api/monitoring/labors` | Médias de duração |
| PUT | `/api/monitoring/force-recalc` | Recalcula médias usando histórico de execuções |

Controllers e processos administrativos existentes de labor, supply e estoque foram mantidos. Erros de OS inexistente retornam 404; regras de negócio, 400.

Configure `OS_SERVICE_BASE_URL` localmente no `.env`. No deploy, o workflow lê a URL atual do parâmetro SSM definido por `OS_SERVICE_BASE_URL_PARAMETER` (padrão `/officyna/os-service/base-url`) e injeta-a no ConfigMap. A esteira de infraestrutura deve atualizar esse parâmetro após criar/substituir o ELB.

## Contratos de eventos

Recebimento em `eventForDiagnostics`: o corpo contém somente `serviceOrderId` e `status`; o atributo SQS `correlationId` é obrigatório e prevalece sobre qualquer campo no corpo.

```json
{"serviceOrderId":"os1","status":"RECEBIDA"}
```

O registro local guarda `serviceOrderId`, status e `correlationId`; a aplicação não cria nem mantém cópias locais de labors/supplies da OS.

Aprovação integral mínima:

```json
{"serviceOrderId":"os1","status":"APROVADA"}
```

O status de aprovação não contém decisões de labor nem itens de supply; essas informações pertencem ao `os-service`.

Contratos de saída:

- `eventDiagnosis`: atributo SQS `correlationId`; corpo somente `serviceOrderId`, `status`, `mechanicId` e `mechanicName`.
- `eventDiagnosisCompleted`: atributo SQS `correlationId`; corpo somente `serviceOrderId`.
- `eventExecutionStarted` e `eventForOsCompleted`: atributo SQS `correlationId`; corpo `serviceOrderId` e `status`.

As ações do mecânico para iniciar/finalizar labor chamam os endpoints de `start-labor` e `finish-labor` no `os-service`, encaminhando o Bearer JWT. O início da primeira labor também muda a OS para `EM_EXECUCAO` no `os-service`; a conclusão geral chama `update-status` com `FINALIZADA`. O snapshot local acompanha essas transições. Ao finalizar labor, o monitoring local registra os horários devolvidos pelo `os-service`, em paralelo ao monitoring mantido pelo próprio `os-service`.

Os payloads não carregam identificador de evento, labors, supplies, orçamento ou informações de diagnóstico.

Mensagens repetidas de diagnóstico são identificadas pelo `serviceOrderId` já existente. SQS pode entregar duplicatas e fora de ordem; as transições de status permanecem protegidas.

## Persistência: três bancos

A persistência de labor e supply fica em `infrastructure/persistence`: documentos em `mongodb/model`, repositórios Mongo em `mongodb/repository`, gateways em `mongodb/gateway` e conversores em `mapper`. Os documentos e o repositório de movimentações de estoque seguem essa mesma organização. As interfaces de domínio permanecem nos respectivos módulos administrativos. `StockService` trabalha com `StockMovement` e a interface `StockMovementRepository`; o gateway usa `StockMovementMongoRepository` e converte os documentos Mongo. O histórico mantém os mesmos campos no JSON da API.

Três bancos lógicos no **mesmo cluster MongoDB e MongoClient**:

| Banco | Collections |
|---|---|
| `officyna_supply` | `supplies`, `stock_movements` |
| `officyna_labor` | `labors`, `labor_monitoring`, `labor_executions` |
| `officyna_snapshot` | `execution_work` |

`execution_work` guarda o ID da OS (também `_id`), status, mecânico, `correlationId`, datas de cada status, flags de reserva/consumo e os IDs/quantidades reservados para estoque. Não mantém uma cópia dos labors/supplies da OS. O Mongo `_id` evita snapshots duplicados.

As mensagens são montadas em memória. Durante uma transação, o publicador registra um callback em memória e envia ao SQS apenas em afterCommit, quando o Mongo já confirmou a gravação. Rollback ou falha de commit não enviam mensagens. Não há mensagens persistidas nem reenvio automático. Sem transação ativa, o envio é imediato. Se o SQS falhar após o commit, o erro é propagado ao chamador e os dados Mongo permanecem confirmados. Uma interrupção do processo entre o commit e o envio também pode impedir a entrega; Mongo e SQS não formam uma transação única.

Transações usam a mesma MongoDatabaseFactory/session entre os três templates. Requer **replica set** ou cluster com suporte às transações usadas. Não separar os bancos em três servidores independentes sem adaptar a coordenação. Collections/índices são criados antes do primeiro fluxo de negócio. Validar suporte/transações/índices na versão real do DocumentDB antes de produção.

## Regras preservadas e particularidades do monólito

Veja `docs/REGRAS_E_VALIDACAO.md` para rastreabilidade. Não foram inventados bloqueios de edição por status, execução sequencial ou exigência de labor aprovado no endpoint de início: essas restrições não existem no código fonte recebido. Elas podem ser requisitos desejáveis, mas mudariam a regra existente.

JWT preserva assinatura/chave, expiração e roles do token emitido pelo serviço de autenticação. O filtro extrai o usuário, carrega seus `UserDetails` pelo componente `AuthService` e valida o token contra esse usuário, seguindo o fluxo do monólito; o controller não decodifica o token. `AuthService` está temporariamente simulado usando as claims assinadas, pois o contrato do serviço de identidade ainda não foi disponibilizado. Quando esse serviço existir, a implementação deverá carregar/validar o usuário nele. O endpoint de diagnóstico valida `mechanicId` preenchido e os estados da OS, mas **não verifica o cadastro remoto do mecânico**.

## Execução

Java 21. Configurar `.env` usando `.env.example`, filas/tópicos e credenciais AWS ou role da aplicação. Executar:

```bash
./mvnw verify
docker compose up --build
```

Compose inicializa Mongo replica set e publica a aplicação em 8084. Para usar Mongo fora do Compose, configure replica set com hostname acessível à JVM; o hostname `mongodb` anunciado pelo Compose é destinado aos containers.

As filas, tópicos, assinaturas SNS→SQS e DLQs pertencem à infraestrutura compartilhada e não são criados pelo deploy do microsserviço. Os nomes e ARNs necessários são configurados por ambiente.

Permissões da role IRSA da aplicação: SQS ReceiveMessage/DeleteMessage/ChangeMessageVisibility/GetQueueAttributes/GetQueueUrl nas filas de entrada e SQS SendMessage/GetQueueUrl nas filas de saída. A role, o OIDC provider e a associação do principal de deploy ao EKS pertencem ao repositório de infraestrutura.

Esta peça não cria nem configura cluster ou nodes. O repositório de infraestrutura mantém EKS, node groups, rede, addons compartilhados, roles e políticas IAM. Este repositório constrói/publica a imagem e aplica somente os recursos Kubernetes da aplicação (`Deployment`, `Service` interno `ClusterIP`, `HPA`, `ServiceAccount`, `ConfigMap` e Secrets). O workflow conecta ao cluster existente, busca o endpoint DocumentDB no SSM e faz o rollout, seguindo o fluxo de build, imagem e deploy do `officyna-service`. O `Service` permanece `ClusterIP` para tráfego interno/gateway; a exposição externa e a rota para esta API devem ser providas pelo gateway da infraestrutura. Consulte [`docs/INFRA_EKS.md`](docs/INFRA_EKS.md) para a divisão de responsabilidades e as variáveis necessárias.

New Relic: agente/configuração do monólito restaurados, nome da aplicação ajustado, license key somente por ambiente. Logs JSON mantêm `message` e `correlationId`; transições e duração usam os mesmos identificadores SERVICE_ORDER_STATUS_CHANGED/SERVICE_ORDER_STATUS_DURATION. Média de labor usa dia de trabalho de **8 horas**, inicialização por estimativa e forceRecalc. Health, métricas HTTP do Actuator e filtros de correlação estão disponíveis.

## Migração e validação

Não inferir status a partir de documentos antigos `execution_work` sem status. Antes de atualizar um ambiente já usado, fazer backup e migrar esses documentos com o estado/timestamps autoritativos do os-service. Reenvio de eventForDiagnostics serve apenas para novas OSs RECEBIDA; não deve regredir ordens em andamento. No snapshot persistido, duplicatas de recebimento são ignoradas.

Foram adicionados testes unitários de transições, contratos, aprovação, execução paralela permitida, monitoring e duração. **Não foi possível compilar/executar a suíte neste ambiente**: Maven Central não resolveu no DNS; o runtime disponível também é Java 17, enquanto o projeto exige 21. Os checks executados são estruturais, não comprovam integração Mongo/AWS. Executar `./mvnw verify` em Java 21 e os testes de integração descritos no documento antes de deploy.
