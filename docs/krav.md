# Krav — rimfrost-regel-rtf-manuell-komplettering

Tjänsten implementerar kompletteringsregeln för RTF-ärenden (Rätt till försäkring)
och avgör om ett ärende kräver komplettering med personnummer och avsikt innan
handläggningen kan fortsätta. Om komplettering krävs skapas en uppgift i det
Operativa Uppgiftslagret (OUL) som en handläggare kan arbeta med. Kompletteringen
avslutas antingen manuellt av handläggaren eller automatiskt via externa händelser.

Krav som rör det ärvda ramverket (`rimfrost-framework-regel-komplettering`) upprepas
inte — se dess `docs/krav.md`. Krav för identitetsprenumeration finns i
`docs/krav-identitet.md` (RTFMK-FR-08, RTFMK-FR-09).

---

## 1. Intressenter och aktörer

| Aktör                              | Roll                                                                                                   |
|------------------------------------|--------------------------------------------------------------------------------------------------------|
| Handläggare                        | Hämtar kompletteringsdata via REST, registrerar svar och slutför komplettering                         |
| Operativt Uppgiftslager (OUL)      | Tar emot och hanterar kompletteringsuppgifter; skickar statushändelser när uppgifter avslutas          |
| Referensdata-tjänst                | Tillhandahåller erbjudandenamn som visas i den utökade uppgiftsbeskrivningen                           |
| Handläggning Adapter               | Persistenslager för ärendedata (läsning och uppdatering av `Handlaggning`)                             |
| Kafka                              | Asynkron kanal för regelanrop (inkommande) och regelsvar (utgående)                                    |
| Skyddad Identitet-tjänst (SID)     | Kontrollerar om handläggaren har behörighet att se ärenden med skyddad identitet                       |

---

## 2. Funktionella krav

### RTFMK-FR-01 — Regelanrop via Kafka

Tjänsten tar emot asynkrona regelanrop från Kafka och inleder kompletteringsflödet.

- **RTFMK-FR-01.1** Tjänsten ska konsumera meddelanden från Kafka-topic
  `rtf-manuell-komplettering-requests`.
- **RTFMK-FR-01.2** När ett regelanrop tas emot och komplettering inte krävs ska
  tjänsten omedelbart publicera ett regelsvar med `utfall = JA` på Kafka-topic
  `rtf-manuell-komplettering-responses`.
- **RTFMK-FR-01.3** När ett regelanrop tas emot och komplettering krävs ska tjänsten
  skapa en kompletteringsuppgift i OUL och invänta handläggarens åtgärd.
- **RTFMK-FR-01.4** Om ett regelanrop resulterar i ett fel ska tjänsten publicera ett
  regelsvar med `utfall = ERROR` och felkoden `RIMFROST_OTHER` på svarstopic.
- **RTFMK-FR-01.5** Regelanropet ska innehålla `aktivitetId`; om fältet `processvyUrl`
  medföljer ska det vidarebefordras vid skapandet av OUL-uppgiften.

### RTFMK-FR-02 — OUL-uppgiftshantering

Tjänsten hanterar livscykeln för kompletteringsuppgifter i det Operativa Uppgiftslagret.

- **RTFMK-FR-02.1** Tjänsten ska skapa en OUL-uppgift med `aktivitetId` (och, om
  förekommande, `processvyUrl`) när komplettering krävs.
- **RTFMK-FR-02.2** Tjänsten ska stänga OUL-uppgiften när kompletteringen är slutförd
  via `POST /{handlaggningId}/done`.
- **RTFMK-FR-02.3** Tjänsten ska konsumera statushändelser från Kafka-topic
  `operativt-uppgiftslager-status-notification.rtf-manuell-komplettering` för att
  ta emot meddelanden om att OUL-uppgiften har avslutats externt.

### RTFMK-FR-03 — Hämta kompletteringsdata

Handläggaren ska kunna hämta aktuell kompletteringsdata för ett ärende.

- **RTFMK-FR-03.1** `GET /regel/rtf-manuell-komplettering/{handlaggningId}` ska returnera
  ärendets nuvarande personnummer och avsikt (båda kan vara null om de saknas).
- **RTFMK-FR-03.2** Svaret ska utöver regelspecifik data också innehålla
  `informationsaccess`-objekt med `uppgiftId`, `regelId`, `regelVersion` och
  `anvandare`.
- **RTFMK-FR-03.3** Om ärendet inte finns ska tjänsten returnera HTTP 404.
- **RTFMK-FR-03.4** Om anropande användares identitet saknas ska tjänsten returnera
  HTTP 401.
- **RTFMK-FR-03.5** Om handläggaren saknar behörighet (SID-kontroll misslyckades) ska
  tjänsten returnera HTTP 403.

### RTFMK-FR-04 — Registrera kompletteringssvar

Handläggaren ska kunna registrera personnummer och avsikt för ett ärende.

- **RTFMK-FR-04.1** `PATCH /regel/rtf-manuell-komplettering/{handlaggningId}` ska ta emot
  personnummer och avsikt och uppdatera ärendets yrkande.
- **RTFMK-FR-04.2** Om en personnummerroll redan finns i `rollerIYrkande` ska dess värde
  uppdateras och det befintliga `yrkandeRollId` bevaras.
- **RTFMK-FR-04.3** Om ingen personnummerroll finns ska en ny skapas med ett genererat
  `rollId`.
- **RTFMK-FR-04.4** Övriga roller i `rollerIYrkande` (ej av typ personnummer) ska lämnas
  oförändrade vid en uppdatering.
- **RTFMK-FR-04.5** Yrkandets `avsikt` ska sättas till det inkommande värdet.
- **RTFMK-FR-04.6** Yrkandets `version` ska ökas med 1 vid varje uppdatering.
- **RTFMK-FR-04.7** Vid lyckad registrering ska tjänsten returnera HTTP 204.
- **RTFMK-FR-04.8** Om ärendet inte finns ska tjänsten returnera HTTP 404.
- **RTFMK-FR-04.9** Om handläggaren saknar behörighet (SID-kontroll misslyckades) ska
  tjänsten returnera HTTP 403.
- **RTFMK-FR-04.10** Om handläggningstjänsten är otillgänglig ska tjänsten returnera HTTP 503.

### RTFMK-FR-05 — Slutföra komplettering

Handläggaren ska kunna markera kompletteringen som slutförd när alla uppgifter är ifyllda.

- **RTFMK-FR-05.1** `POST /regel/rtf-manuell-komplettering/{handlaggningId}/done` ska
  slutföra kompletteringen och publicera ett regelsvar med `utfall = JA`.
- **RTFMK-FR-05.2** Om komplettering fortfarande krävs (personnummer eller avsikt
  saknas) ska tjänsten returnera HTTP 422 utan att stänga uppgiften.
- **RTFMK-FR-05.3** Vid lyckad slutföring ska OUL-uppgiften stängas.
- **RTFMK-FR-05.4** Vid lyckad slutföring ska korrelationsdata rensas.
- **RTFMK-FR-05.5** Om ärendet inte finns ska tjänsten returnera HTTP 404.
- **RTFMK-FR-05.6** Om handläggaren saknar behörighet ska tjänsten returnera HTTP 403.

### RTFMK-FR-06 — Utökad uppgiftsbeskrivning

Tjänsten ska exponera en utökad beskrivning av kompletteringsuppgiften.

- **RTFMK-FR-06.1** `GET /regel/rtf-manuell-komplettering/utokadUppgiftsbeskrivning` ska
  returnera en textbeskrivning hämtad från regelns konfigurationsfil (`config.yaml`).

### RTFMK-FR-07 — Skyddad identitet (SID)

Åtkomst till ärenden med skyddad identitet ska skyddas.

- **RTFMK-FR-07.1** Innan data returneras eller uppdateras ska tjänsten kontrollera om
  något ärendedeltagare har skyddad identitet.
- **RTFMK-FR-07.2** Om skyddad identitet förekommer och handläggaren saknar SID-behörighet
  ska tjänsten returnera HTTP 403.
- **RTFMK-FR-07.3** Vid HTTP 403 på grund av SID-kontroll ska tjänsten försöka
  avregistrera handläggaren från OUL-uppgiften.

---

## 3. Icke-funktionella krav

### RTFMK-NFR-01 — Konfiguration

- **RTFMK-NFR-01.1** Tjänsten ska konfigurera `oul.api.base-url` med URL till OUL-tjänsten.
- **RTFMK-NFR-01.2** Tjänsten ska konfigurera `referensdata.api.base-url` med URL till
  referensdata-tjänsten.
- **RTFMK-NFR-01.3** Kafka-topic för inkommande regelanrop ska konfigureras via
  `mp.messaging.incoming.regel-requests.topic`.
- **RTFMK-NFR-01.4** Kafka-topic för utgående regelsvar ska konfigureras via
  `mp.messaging.outgoing.regel-responses.topic`.
- **RTFMK-NFR-01.5** Kafka-topic för OUL-statusnotifikationer ska konfigureras via
  `mp.messaging.incoming.operativt-uppgiftslager-status-notification.topic`.

### RTFMK-NFR-02 — Datapersistens

- **RTFMK-NFR-02.1** Korrelationsdata (ärendeID, uppgiftsID, process-metadata) ska
  persisteras i databasens schema `regel_rtf_manuell_komplettering`.
- **RTFMK-NFR-02.2** Databastabellerna ska ha prefixet `rtf_manuell_komplettering_`.

### RTFMK-NFR-03 — Hälsostatus

- **RTFMK-NFR-03.1** Tjänsten ska exponera ett hälsoendpoint i enlighet med ramverkets krav.

---

## 4. API-gränssnitt (översikt)

| API                                         | Målgrupp       | Specifikationsartefakt                                        |
|---------------------------------------------|----------------|---------------------------------------------------------------|
| REST — kompletteringsdata                   | Handläggare    | `rimfrost-regel-rtf-manuell-komplettering-openapi-jaxrs-spec` |
| Kafka — regelanrop (inkommande)             | Orkestrering   | `rimfrost-framework-regel-komplettering-asyncapi`             |
| Kafka — regelsvar (utgående)                | Orkestrering   | `rimfrost-framework-regel-komplettering-asyncapi`             |
| Kafka — OUL-statusnotifikation (inkommande) | OUL            | `rimfrost-framework-regel-komplettering-asyncapi`             |

---

## 5. Integration med externa tjänster

| Tjänst                        | Syfte                                                                     |
|-------------------------------|---------------------------------------------------------------------------|
| Operativt Uppgiftslager (OUL) | Skapar och stänger kompletteringsuppgifter; tar emot statusnotifikationer |
| Referensdata-tjänst           | Hämtar erbjudandenamn för utökad uppgiftsbeskrivning                      |
| Handläggning Adapter          | Läser och uppdaterar ärendedata (Handlaggning/Yrkande)                    |
| Skyddad Identitet (SID)       | Kontrollerar om ärendedeltagare har skyddad identitet                     |
