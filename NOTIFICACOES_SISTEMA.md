# Sistema de Notificações — Tô Contando

Este documento descreve a arquitetura, o comportamento e as limitações do sistema de notificações locais do aplicativo **Tô Contando**.

## 1. Notificações locais e notificações push

O aplicativo não depende de notificações push tradicionais enviadas por um servidor, como Firebase Cloud Messaging.

Os lembretes de eventos são **notificações locais agendadas**. O horário, o conteúdo e o processamento do lembrete são controlados diretamente no aparelho do usuário.

Isso permite que os lembretes funcionem:

* Sem conexão com a internet.
* Sem envio dos eventos pessoais para um servidor.
* Mesmo quando o aplicativo não está aberto.
* Com maior privacidade para o usuário.

As notificações locais aparecem normalmente na central de notificações do Android, assim como outras notificações do sistema. A diferença está na origem: notificações push chegam de um servidor, enquanto notificações locais são geradas pelo próprio aplicativo.

## 2. Fluxo de funcionamento

O fluxo básico de uma notificação é:

1. O usuário cria ou edita um evento.
2. O aplicativo salva os dados do evento e seus lembretes no Room.
3. O `NotificationScheduler` calcula os horários de disparo.
4. O lembrete é registrado no `AlarmManager`.
5. Quando chega o horário, o Android executa o receiver responsável.
6. O aplicativo valida novamente o estado do evento no banco.
7. Caso o evento ainda exista e esteja ativo, a notificação é exibida.
8. O estado do agendamento é atualizado no banco.

A validação no momento do disparo evita notificações de eventos que foram apagados, concluídos, desativados ou alterados depois do agendamento original.

## 3. Componentes principais

### AlarmManager

Responsável por registrar os horários dos lembretes no Android.

Dependendo das permissões disponíveis, o sistema utiliza:

* `setExactAndAllowWhileIdle()` para lembretes exatos.
* `setAndAllowWhileIdle()` como fallback aproximado.

### Room Database

O sistema utiliza as tabelas:

* `event_reminders`
* `scheduled_notifications`

Essas tabelas armazenam as configurações dos lembretes e o estado operacional dos agendamentos.

Entre os estados que podem ser registrados estão:

* Agendado.
* Exibido.
* Adiado.
* Cancelado.
* Ignorado.
* Com falha.

O banco permite reconstruir, auditar e reconciliar os alarmes, mas a prevenção de duplicações também depende da geração de identificadores estáveis e do cancelamento correto dos agendamentos antigos.

### NotificationScheduler

Centraliza a criação, substituição e cancelamento dos alarmes.

Antes de utilizar um alarme exato, ele verifica:

```kotlin
alarmManager.canScheduleExactAlarms()
```

Caso o acesso não esteja disponível, o sistema utiliza automaticamente um agendamento aproximado.

### NotificationDispatcher

É executado quando um alarme dispara.

Antes de mostrar a notificação, ele verifica se:

* O evento ainda existe.
* O evento não foi concluído.
* O lembrete continua habilitado.
* O horário ainda corresponde ao evento atual.
* O agendamento ainda está pendente.
* A notificação não foi exibida anteriormente.

### NotificationCompat

Responsável por construir a notificação exibida pelo Android, incluindo:

* Ícone.
* Título.
* Mensagem.
* Canal.
* Som.
* Vibração.
* Ações.
* Destino aberto ao tocar na notificação.

### NotificationBootReceiver e WorkManager

Os registros do `AlarmManager` não sobrevivem à reinicialização do aparelho.

Depois que o Android termina de inicializar, o `NotificationBootReceiver` agenda um trabalho pelo `WorkManager`. Esse trabalho consulta os eventos e lembretes persistidos no Room e reconstrói os agendamentos necessários.

O processamento pesado não é executado diretamente dentro do receiver.

## 4. Alarmes exatos e fallback aproximado

### Modo exato

Quando o aplicativo possui acesso a alarmes exatos, utiliza:

```kotlin
setExactAndAllowWhileIdle()
```

Esse modo deve ser reservado aos lembretes em que o horário definido pelo usuário é importante.

### Modo aproximado

Quando o acesso a alarmes exatos não está disponível, utiliza:

```kotlin
setAndAllowWhileIdle()
```

Esse fallback mantém o lembrete agendado e permite que ele seja processado durante o modo de economia de energia.

Como se trata de um alarme inexato, o Android pode atrasar sua execução conforme:

* Estado da bateria.
* Modo Doze.
* Políticas de economia de energia.
* Restrições do fabricante.
* Estado geral do aparelho.

Por isso, o horário de um lembrete aproximado não é garantido.

## 5. Permissões

### POST_NOTIFICATIONS

Necessária no Android 13 ou superior para que o aplicativo possa exibir notificações.

Essa permissão deve ser solicitada de forma contextual, preferencialmente quando o usuário ativar ou criar seu primeiro lembrete.

Caso seja negada, o evento continua salvo, mas o aplicativo deve informar que o alerta não poderá ser mostrado.

### SCHEDULE_EXACT_ALARM

Permite que o aplicativo solicite alarmes precisos.

Esse é um acesso especial controlado pelo usuário nas configurações do Android. O aplicativo deve verificar `canScheduleExactAlarms()` antes de cada agendamento exato.

Se o acesso não estiver disponível, o lembrete será agendado de forma aproximada.

Quando o usuário concede novamente o acesso, o aplicativo deve executar uma reconciliação e recriar como exatos os lembretes que ainda estiverem pendentes.

### VIBRATE

Permite que os canais de notificação utilizem vibração quando configurados pelo usuário e permitidos pelo sistema.

### RECEIVE_BOOT_COMPLETED

Permite detectar a reinicialização do aparelho e iniciar a reconstrução dos agendamentos persistidos.

## 6. Snooze — Adiar lembrete

A notificação possui a ação **Adiar**.

Ao selecionar essa ação, o aplicativo:

1. Remove a notificação atualmente visível.
2. Verifica se o evento ainda está ativo.
3. Calcula um novo horário de disparo.
4. Atualiza o contador de adiamentos.
5. Substitui qualquer snooze pendente para a mesma ocorrência.
6. Registra o novo agendamento no banco.
7. Agenda o novo disparo como exato ou aproximado, dependendo da permissão disponível.

O tempo padrão atual é de 30 minutos.

Antes de exibir o lembrete adiado, o dispatcher realiza novamente todas as validações do evento. Caso ele tenha sido concluído, apagado ou desativado durante o período de adiamento, a nova notificação não será mostrada.

## 7. Idempotência e prevenção de duplicações

Cada agendamento deve possuir uma identidade lógica estável, formada por informações como:

```text
eventId + reminderId + occurrenceId
```

Ao editar um evento, o sistema:

1. Localiza seus agendamentos pendentes.
2. Cancela os `PendingIntent`s antigos.
3. Atualiza os registros correspondentes.
4. Calcula os novos horários.
5. Cria apenas os agendamentos válidos.

O sistema também precisa impedir:

* Dois snoozes simultâneos para a mesma ocorrência.
* Alarmes de eventos apagados.
* Alarmes antigos após uma edição.
* Mais de uma exibição para a mesma ocorrência.
* Registros órfãos no banco.
* Alarmes do sistema sem registro correspondente.

## 8. Reconciliação dos agendamentos

O aplicativo executa uma rotina de reconciliação para comparar os dados do Room com os agendamentos esperados.

Essa rotina é acionada em situações como:

* Inicialização do aplicativo.
* Reinicialização do aparelho.
* Alteração de data ou fuso horário.
* Atualização do aplicativo.
* Retorno das configurações de alarmes exatos.
* Concessão do acesso a alarmes exatos.
* Recuperação após uma falha de agendamento.

A reconciliação:

* Remove registros órfãos.
* Cancela lembretes inválidos.
* Elimina duplicações.
* Recria agendamentos ausentes.
* Atualiza o modo exato ou aproximado.
* Mantém somente ocorrências futuras válidas.

## 9. Conteúdo das notificações

O `NotificationFactory` define a mensagem conforme o tipo e a proximidade do evento.

### No horário

```text
Chegou a hora. “[Evento]” começa agora.
```

### Um dia antes

```text
É amanhã! Sua contagem para “[Evento]” termina amanhã.
```

### Evento privado

```text
Você tem um evento privado se aproximando. Abra o aplicativo para ver.
```

Eventos privados não exibem o título real no conteúdo público da notificação.

Ao tocar na notificação, o aplicativo deve abrir diretamente o evento correspondente, e não apenas a tela inicial.

## 10. Canais de notificação

As notificações devem ser organizadas em canais separados, como:

### Lembretes de eventos

Usado para notificações diretamente relacionadas aos eventos do usuário.

### Resumos e informações

Usado para notificações menos urgentes, como resumos ou informações gerais do aplicativo.

O usuário pode controlar som, vibração e importância de cada canal pelas configurações do Android.

## 11. Limitações do sistema

Nenhum sistema de notificações locais pode garantir pontualidade absoluta em todos os aparelhos.

A entrega pode ser afetada por:

* Permissão de notificações negada.
* Acesso a alarmes exatos desativado.
* Aplicativo colocado em suspensão profunda.
* Restrições agressivas de bateria do fabricante.
* Aplicativo interrompido manualmente pelo usuário.
* Alterações de data, hora ou fuso.
* Celular desligado no momento do lembrete.

O aplicativo deve detectar os estados que podem ser verificados e apresentar orientações claras, sem prometer uma precisão que o sistema operacional não pode garantir.

## 12. Diagnóstico

A tela de diagnóstico deve apresentar, de forma simples:

```text
Notificações: permitidas
Alarmes precisos: permitidos
Modo atual: exato
Próximo lembrete: 18 de junho, 08:00
Agendamentos ativos: 4
Última reconstrução: hoje, 07:12
```

Quando houver algum bloqueio, a tela deve explicar a consequência e oferecer acesso direto à configuração correspondente.

## 13. Privacidade

Todo o processamento dos lembretes de eventos acontece localmente.

Os títulos, datas e configurações dos eventos não precisam ser enviados a um servidor para que as notificações funcionem.

Quando um evento estiver marcado como privado, seu título e conteúdo sensível não serão exibidos na tela bloqueada ou no conteúdo público da notificação.
