# Plan: FKPOC-1270 — Aktivera automatisk komplettering vid uppdaterad identitetsdata

## Background

Tjänsten ska reagera på `IdentitetUppdateradEvent` — händelser som skickas när
identitetsdata för en person har uppdaterats. Ramverket hanterar själva mottagandet
och utvärderingen, men behöver veta vilka identiteter som ska bevakas. Det sker
genom att `getPrenumerationsIdentiteter()` implementeras i `RtfKompletteringService`
och returnerar de personnummer-identiteter som finns i ärendets yrkande. Om
identitetsdata uppdateras och kompletteringen därmed är fullständig avslutar ramverket
kompletteringen automatiskt. Konfigurationsproperties för prenumerations-API och
reply-to-topic måste också läggas till (RTFMK-FR-09).

---

## Approach

TDD: skriv test för `getPrenumerationsIdentiteter()` först, implementera sedan.
Konfigurationssteget görs sist och är icke-testbart.

---

## Steps

- [ ] **1. Implementera `getPrenumerationsIdentiteter()`**

  Lägg till override i `RtfKompletteringService`:

  ```java
  @Override
  public List<Idtyp> getPrenumerationsIdentiteter(Handlaggning handlaggning) {
      return handlaggning.yrkande().rollerIYrkande().stream()
          .filter(r -> "personnummer".equals(r.individ().typId()))
          .map(RollIYrkande::individ)
          .toList();
  }
  ```

  Fil: `src/main/java/se/fk/github/rtf/manuell/komplettering/logic/RtfKompletteringService.java`

---

- [ ] **2. Lägg till tester för `getPrenumerationsIdentiteter()`**

  Lägg till i `RtfKompletteringServiceTest`:

  - Returnerar tom lista när `rollerIYrkande` är tom
  - Returnerar personnummer-identiteter (inklusive de med tomt värde)
  - Utesluter roller av annan typ

  Fil: `src/test/java/se/fk/github/rtf/manuell/komplettering/logic/RtfKompletteringServiceTest.java`

---

- [ ] **3. Konfigurera identitetsprenumeration i `application.properties`**

  Lägg till:

  ```properties
  identitet-prenumeration.api.base-url=http://rimfrost-k8s-identitet-prenumeration:8080
  mp.messaging.incoming.identitet-uppdaterad.topic=identitet-uppdaterad-rtf-manuell-komplettering
  quarkus.kafka.devservices.topics=...,identitet-uppdaterad-rtf-manuell-komplettering
  ```

  Fil: `src/main/resources/application.properties`

---

## Execution order

1 → 2 → 3
