# MedRoutine

Aplicativo de apoio a cuidadores no processo de administração de medicações de idosos
(TCC – ADS/UCSAL). O repositório segue a arquitetura da Seção 5.2 do artigo:

```
┌──────────────────────────────┐        HTTPS/JSON        ┌───────────────────────────────────────┐
│ App Android nativo (Java/XML)│ ───────────────────────▶ │ Back-end (API REST – Spring Boot)     │
│  • interface do idoso        │                          │  • módulo de autenticação/autorização │
│  • interface do cuidador     │ ◀─── push (FCM) ──────── │  • módulo de regras de negócio        │
└──────────────────────────────┘                          │  • processamento agendado (1/min)     │
                                                          └──────────────────┬────────────────────┘
                                                                             │ JPA
                                                                    ┌────────▼────────┐
                                                                    │ PostgreSQL      │
                                                                    └─────────────────┘
```

| Pasta | Conteúdo |
|---|---|
| `android/` | Projeto do Android Studio (Java 17, Views em XML, minSdk 26 / targetSdk 34) |
| `backend/` | API em Java 17 + Spring Boot 3.3, PostgreSQL com Flyway, JWT, bcrypt e Firebase Admin |
| `docker-compose.exemplo.yml` | Modelo do `docker-compose.yml`, que sobe o PostgreSQL e a API com um comando (o arquivo real não vai para o repositório) |

As regras de negócio centrais (geração de doses, tolerância e transição de estados) ficam no pacote
`br.ucsal.medroutine.domain`, em Java puro, **com o mesmo código no back-end e no app**. O servidor é a
referência (RNF01). O app usa a mesma cópia para validar formulários e para executar o modo demonstração.
Os testes unitários desse pacote (RNF18) estão em `backend/src/test` e `android/app/src/test`.

---

## 1. Como executar

### 1.1 Back-end

Com Docker:

```bash
cp docker-compose.exemplo.yml docker-compose.yml
cp backend/src/main/resources/application.yml.exemplo backend/src/main/resources/application.yml
# edite os dois arquivos e troque os valores TROQUE_... (use a mesma senha do banco nos dois)
docker compose up --build
```

A API fica em `http://localhost:8080`, e `GET /actuator/health` responde `UP`.

Sem Docker: instale o PostgreSQL 16, crie o banco `medroutine` (usuário e senha `medroutine`) e rode:

```bash
cd backend
mvn spring-boot:run
```

Variáveis de ambiente opcionais:

| Variável | Uso |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexão com o banco |
| `JWT_SECRET` | Segredo dos tokens (mínimo de 32 caracteres) |
| `FIREBASE_CREDENTIALS` | Caminho do JSON da conta de serviço do Firebase. Sem ele, os pushes só aparecem no log |
| `spring.mail.*` | SMTP do e-mail de recuperação de senha. Sem ele, o código aparece no log |

As tabelas são criadas pelo Flyway (`backend/src/main/resources/db/migration/V1__esquema_inicial.sql`).

### 1.2 App Android

1. No Android Studio (Koala ou mais recente), use **File ▸ Open** e escolha a pasta `android/`.
2. Aguarde a sincronização do Gradle (AGP 8.5.2, Gradle 8.7, JDK 17).
3. Rode no emulador. O endereço padrão da API é `http://10.0.2.2:8080/`, que é o computador vendo pelo emulador.
   Em um celular físico, toque em **Configurar servidor** na tela inicial e informe o IP do computador,
   por exemplo `http://192.168.0.10:8080/`.

**Push (FCM):** crie um projeto no [console do Firebase](https://console.firebase.google.com), adicione um
app Android com o pacote `br.ucsal.medroutine` e baixe o `google-services.json` para `android/app/`. O plugin
só é aplicado quando esse arquivo existe. Para o back-end, gere uma chave da conta de serviço em
*Configurações do projeto ▸ Contas de serviço* e aponte `FIREBASE_CREDENTIALS` para ela.

### 1.3 Modo demonstração (sem servidor)

Na tela inicial, toque em **Conhecer o modo demonstração** e escolha *idosa (Maria)* ou *cuidadora (Ana)*.

- Os dados são fictícios e ficam só no aparelho. Uma faixa amarela os identifica em todas as telas.
- As regras são as mesmas do servidor.
- A dose **Vitamina D** é criada para tocar **2 minutos** depois de iniciar a demonstração. Se não for
  confirmada, gera o **alerta ao cuidador 5 minutos** após o horário. Assim, o escalonamento completo cabe
  na apresentação.
- Em *Minha conta ▸ Reiniciar dados da demonstração*, a demonstração recomeça do zero.

Com isso, a apresentação pode ser repetida mesmo que o servidor, a internet ou o Firebase estejam
indisponíveis (restrições do projeto, Seção 5.1).

---

## 2. Estrutura do app Android

```
br.ucsal.medroutine
├── domain/            regras de negócio (cópia idêntica à do back-end)
├── data/
│   ├── model/         modelos da API (Gson)
│   ├── remote/        Retrofit: ApiService, ApiClient, GsonFactory
│   ├── local/         Room: cópia da agenda (DoseLocal) e fila offline (ConfirmacaoPendente)
│   ├── repo/          MedRoutineRepository (interface), RemoteRepository, Repositorios
│   └── sync/          AgendaIdoso, FilaConfirmacoes e Workers (WorkManager)
├── demo/              DemoRepository: mesma interface, executada no aparelho
├── alarm/             AgendadorLembretes (AlarmManager), receivers do lembrete e do boot
├── notification/      canais, notificações do lembrete e do escalonamento, serviço FCM
├── session/           SessionManager (token, perfil, preferências)
└── ui/
    ├── inicio/        Splash (direciona por perfil) e Boas-vindas
    ├── auth/          Login, Cadastro e Recuperação de senha do cuidador
    ├── idoso/         Código de acesso, Doses de hoje e Lembrete em tela cheia
    ├── cuidador/      Painel, Medicamentos, Histórico, Notificações, Detalhe da dose, Idosos, Conta
    └── comum/         Formatos, permissões, utilitários de UI
```

Telas, nos mesmos layouts dos mockups:

| Tela | Activity / Fragment | Layout |
|---|---|---|
| Boas-vindas | `BoasVindasActivity` | `activity_boas_vindas.xml` |
| Idoso · Entrada por código | `IdosoCodigoActivity` | `activity_idoso_codigo.xml` |
| Idoso · Doses de hoje | `IdosoHomeActivity` | `activity_idoso_home.xml`, `item_dose_idoso.xml` |
| Idoso · Lembrete da dose | `LembreteActivity` | `activity_lembrete.xml` |
| Cuidador · Acompanhamento | `CuidadorMainActivity` + `PainelFragment` | `fragment_painel.xml` |
| Cuidador · Cadastro de medicamento | `MedicamentoFormActivity` | `activity_medicamento_form.xml` |
| Cuidador · Notificações | `NotificacoesActivity` | `activity_notificacoes.xml` |
| Cuidador · Push confirmada e alerta | `Notificador.mostrarParaCuidador` | notificação do sistema |
| Cuidador · Situação da dose | `DoseDetalheActivity` | `activity_dose_detalhe.xml` |
| Cuidador · Histórico | `HistoricoFragment` | `fragment_historico.xml` |
| Cuidador · Idosos e convites | `IdososActivity` | `activity_idosos.xml` |

O ícone adaptativo (`mipmap-anydpi-v26/ic_launcher.xml`) é o mesmo do canvas de identidade: seta circular com
o sinal de confirmação, em `#FFD166` sobre `#12355B`, com camada monocromática para os ícones temáticos.

## 3. API REST

| Método | Caminho | Perfil | Requisito |
|---|---|---|---|
| POST | `/api/auth/cadastro` · `/login` · `/recuperar-senha` · `/redefinir-senha` | público | RF01, RNF13 |
| POST | `/api/auth/codigo-idoso` | público | RF03 |
| GET, DELETE | `/api/conta` | ambos | RNF13 |
| PUT, POST | `/api/conta/token-aparelho` · `/api/conta/sair` | ambos | RF01, RF14 |
| GET, POST | `/api/idosos` | cuidador | RF03, RF04 |
| PUT, DELETE | `/api/idosos/{id}` | cuidador | RNF05, RNF13 |
| POST | `/api/idosos/{id}/codigo-acesso` · `/convites` | cuidador | RF03, RF04 |
| POST | `/api/vinculos/aceitar` | cuidador | RF04 |
| DELETE | `/api/idosos/{id}/vinculo` | cuidador | RF04 |
| GET, POST | `/api/idosos/{id}/medicamentos` | cuidador | RF05–RF08 |
| GET, PUT | `/api/medicamentos/{id}` | cuidador | RF09 |
| POST | `/api/medicamentos/{id}/desativar` | cuidador | RF09 |
| GET | `/api/idosos/{id}/doses?data=` | cuidador | RF16 |
| GET | `/api/idosos/{id}/historico?inicio=&fim=&medicamentoId=` | cuidador | RF17 |
| GET | `/api/notificacoes` | cuidador | RF14, RF15, RF20 |
| GET | `/api/doses/{id}` | ambos | RF16 |
| POST | `/api/doses/{id}/confirmacao` | ambos | RF11, RF12, RF13 |
| GET | `/api/eu/agenda` | idoso | RF10 |

Os erros seguem o formato `{ "mensagem": "...", "campo": "dataFim" }`, com o campo indicado para o RF18.

## 4. Rastreabilidade dos requisitos

| Requisito | Onde está implementado |
|---|---|
| RF01 Cadastro e acesso | `AuthService`, `LoginActivity`, `CadastroActivity`, `RecuperarSenhaActivity`, `ContaActivity` |
| RF02 Perfis | `SecurityConfig` (papéis IDOSO/CUIDADOR), `SplashActivity` (o idoso não tem telas de edição) |
| RF03 Código de uso único de 24 h | `CodigoService`, `AuthService.entrarComCodigo`, `IdosoCodigoActivity` |
| RF04 Vínculos N:N | `IdosoService` (convite, aceitar, desvincular), `AutorizacaoService`, índice `uk_vinculo_ativo` |
| RF05–RF07 Medicamento, frequência e tolerância | `MedicamentoService`, `ValidadorTratamento`, `MedicamentoFormActivity` |
| RF08 Horizonte de 7 dias | `GeradorDoses`, `DoseService.gerarDoses` e `renovarHorizonte` (diário) |
| RF09 Alteração preservando o histórico | `MedicamentoService.atualizar` / `desativar`, `DoseRepository.removerPendentesFuturas` |
| RF10 Lembrete em tela cheia, offline, adiar e boot | `AgendadorLembretes`, `LembreteReceiver`, `LembreteActivity`, `BootReceiver` |
| RF11 Confirmação 60 min antes, offline | `RegrasDose.podeConfirmar`, `FilaConfirmacoes`, `SincronizarConfirmacoesWorker` |
| RF12 Confirmação pelo cuidador | `ConfirmacaoDose.peloCuidador`, `DoseDetalheActivity` |
| RF13 Estados da dose | `EstadoDose`, `RegrasDose.estadoAposConfirmacao`, UNIQUE em `confirmacao_dose.dose_id` |
| RF14 Notificação de confirmação | `ConfirmacaoService`, `NotificacaoService`, `MensagensNotificacao` |
| RF15 Alerta único por dose | `VerificacaoDosesService` (a cada minuto), `marcarNaoConfirmada`, UNIQUE em `notificacao` |
| RF16 Estado sempre atualizado | `PainelFragment`, `DoseDetalheActivity` (aberta pela notificação) |
| RF17 Histórico | `DoseService.historico`, `HistoricoFragment` |
| RF18 Validação | `ValidadorTratamento` (servidor e app), `GlobalExceptionHandler` |
| RF19 Permissões | `PermissoesHelper`, `include_aviso_permissoes.xml` |
| RF20 Registro de envio | `Notificacao` (PENDENTE/ENVIADA/FALHA, tentativas, erro) |
| RNF01 UTC | colunas `timestamptz`, `Instant`, `Relogio` do servidor, `horario_aparelho` |
| RNF03 Um toque | botão "Já tomei" no lembrete, na notificação e na lista |
| RNF04 Acessibilidade | textos em sp, toques ≥ 48 dp, `contentDescription`, paleta com contraste ≥ 4,5:1 |
| RNF05 Áudio | `res/raw/lembrete.wav`, `TextToSpeech` quando `anuncioVoz` está ativo |
| RNF06 Alarmes exatos | `setExactAndAllowWhileIdle` ou, sem permissão, `setAndAllowWhileIdle` com aviso |
| RNF07 Fila persistente idempotente | Room (`confirmacao_pendente`) + WorkManager + confirmação idempotente no servidor |
| RNF08 Três tentativas | `NotificacaoService.TENTATIVAS_MAXIMAS` |
| RNF09 Integridade | restrições UNIQUE e transação na confirmação e no alerta |
| RNF10 Execução a cada minuto | `@Scheduled(fixedDelay = 60_000)` |
| RNF12 Segurança | bcrypt, JWT, HTTPS no build de release, vínculo verificado no servidor |
| RNF13 LGPD | consentimento no cadastro, exclusão da conta e do perfil do idoso |
| RNF14 Android 8+ e retrato | `minSdk 26`, `screenOrientation="portrait"` |
| RNF16 Serviços externos | Firebase Cloud Messaging (seção 5 abaixo) |
| RNF17 Modularidade | camadas `domain` / `data` / `ui` no app; `auth` / `service` / `repository` / `web` no servidor |
| RNF18 Testes com relógio injetável | `Relogio`, `GeradorDosesTest`, `RegrasDoseTest`, `ValidadorTratamentoTest` |

## 5. Serviços externos e limitações (RNF16)

- **Firebase Cloud Messaging:** entrega o push ao cuidador. A entrega depende da conectividade e do estado do
  aparelho, então não é garantida. Por isso o estado `ENVIADA` só significa que o FCM aceitou a mensagem, e a
  situação da dose é sempre consultada no servidor.
- **Fabricantes com economia de bateria agressiva** (Xiaomi, Samsung, entre outros) podem atrasar alarmes. Nesses
  aparelhos, oriente o usuário a remover o MedRoutine da otimização de bateria.
- **Tela cheia:** no Android 14, o lembrete em tela cheia pode exigir permissão concedida pelo usuário. O app
  mostra um aviso quando ela está negada.
- **Estado das confirmações:** o registro reflete a confirmação informada no aplicativo, e não a ingestão
  efetiva do medicamento.

## 6. Testes

```bash
cd backend && mvn test          # regras de domínio (JUnit)
cd android && ./gradlew test    # as mesmas regras, no módulo do app
```
