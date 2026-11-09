package se.fk.github.rtf.manuell.komplettering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.RegelKafkaConnector;
import se.fk.rimfrost.framework.regel.RegelTestData;
import se.fk.rimfrost.framework.regel.komplettering.AbstractRegelKompletteringTestBase;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulUppgiftSpec;
import se.fk.rimfrost.referensdata.erbjudande.ErbjudandeReferensdata;

@QuarkusTest
class RtfKompletteringOulTest extends AbstractRegelKompletteringTestBase
{
   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   // Basklassens fält är paketprivat, så det deklareras igen här. Quarkus injicerar samma mock i båda.
   @InjectMock
   OulUppgiftService oulUppgiftService;

   @InjectMock
   ErbjudandeReferensdata erbjudandeReferensdata;

   /**
    * Handläggning utan personnummer och avsikt, så att regeln kräver komplettering och skapar en uppgift i OUL.
    */
   private Handlaggning handlaggningSomKraverKomplettering()
   {
      var yrkande = Mockito.mock(Yrkande.class);
      var handlaggning = Mockito.mock(Handlaggning.class);

      Mockito.when(yrkande.ingangtypId()).thenReturn("b1cb6133-11af-4a30-9180-5ea8104a74c7");
      Mockito.when(yrkande.rollerIYrkande()).thenReturn(List.of());
      Mockito.when(yrkande.avsikt()).thenReturn(null);
      Mockito.when(handlaggning.yrkande()).thenReturn(yrkande);
      Mockito.when(handlaggning.id()).thenReturn(UUID.randomUUID());
      Mockito.when(handlaggning.version()).thenReturn(1);
      Mockito.when(handlaggning.skapadTS()).thenReturn(OffsetDateTime.now());
      Mockito.when(handlaggning.handlaggningspecifikationId()).thenReturn(UUID.randomUUID());

      return handlaggning;
   }

   @Test
   @DisplayName("OUL-uppgiften skapas med aktivitetId och processvyUrl från regelanropet")
   void should_create_oul_uppgift_with_aktivitet_id_and_processvy_url() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = handlaggningSomKraverKomplettering();
      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(erbjudandeReferensdata.getErbjudandeNamn(Mockito.any())).thenReturn("Vård av husdjur");

      var payload = RegelTestData.newRegelRequestMessagePayload(handlaggningId.toString());
      payload.getData().setProcessvyUrl("/process/vah");
      inMemoryConnector.source(RegelKafkaConnector.regelRequestsChannel).send(payload);

      var captor = ArgumentCaptor.forClass(OulUppgiftSpec.class);
      verify(oulUppgiftService, timeout(5000)).createOulUppgift(captor.capture());
      var oulUppgiftSpec = captor.getValue();

      assertEquals(UUID.fromString(payload.getData().getAktivitetId()), oulUppgiftSpec.aktivitetId());
      assertEquals("/process/vah", oulUppgiftSpec.processvyUrl());
   }

   @Test
   @DisplayName("OUL-uppgiften skapas utan processvyUrl när regelanropet saknar den")
   void should_create_oul_uppgift_without_processvy_url() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = handlaggningSomKraverKomplettering();
      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(erbjudandeReferensdata.getErbjudandeNamn(Mockito.any())).thenReturn("Vård av husdjur");

      var payload = RegelTestData.newRegelRequestMessagePayload(handlaggningId.toString());
      inMemoryConnector.source(RegelKafkaConnector.regelRequestsChannel).send(payload);

      var captor = ArgumentCaptor.forClass(OulUppgiftSpec.class);
      verify(oulUppgiftService, timeout(5000)).createOulUppgift(captor.capture());
      var oulUppgiftSpec = captor.getValue();

      assertEquals(UUID.fromString(payload.getData().getAktivitetId()), oulUppgiftSpec.aktivitetId());
      assertNull(oulUppgiftSpec.processvyUrl());
   }
}
