package se.fk.github.rtf.manuell.komplettering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.Idtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.IndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;
import se.fk.github.rtf.manuell.komplettering.logic.RtfKompletteringService;

class RtfKompletteringServiceTest {

   private static final String PERSONNUMMER_TYP_ID = "personnummer";

   private RtfKompletteringService service;

   @BeforeEach
   void setUp() {
      service = new RtfKompletteringService();
   }

   private static IndividYrkandeRoll personnummerRoll(String yrkandeRollId, String varde) {
      return ImmutableIndividYrkandeRoll.builder()
            .individ(ImmutableIdtyp.builder()
                  .typId(PERSONNUMMER_TYP_ID)
                  .varde(varde)
                  .build())
            .yrkandeRollId(yrkandeRollId)
            .build();
   }

   private static IndividYrkandeRoll otherRoll(String yrkandeRollId) {
      return ImmutableIndividYrkandeRoll.builder()
            .individ(ImmutableIdtyp.builder()
                  .typId("annanTyp")
                  .varde("annatVarde")
                  .build())
            .yrkandeRollId(yrkandeRollId)
            .build();
   }

   private static Yrkande yrkandeMock(String avsikt, List<IndividYrkandeRoll> roller) {
      Yrkande yrkande = mock(Yrkande.class);
      when(yrkande.id()).thenReturn(UUID.randomUUID());
      when(yrkande.version()).thenReturn(1);
      when(yrkande.erbjudandeId()).thenReturn("erbjudande-1");
      when(yrkande.yrkandeDatum()).thenReturn(OffsetDateTime.now());
      when(yrkande.yrkandeStatus()).thenReturn("PAGAENDE");
      when(yrkande.yrkandeFrom()).thenReturn(OffsetDateTime.now());
      when(yrkande.yrkandeTom()).thenReturn(OffsetDateTime.now().plusDays(1));
      when(yrkande.avsikt()).thenReturn(avsikt);
      when(yrkande.individYrkandeRoller()).thenReturn(roller);
      when(yrkande.produceradeResultat()).thenReturn(List.of());
      when(yrkande.beslut()).thenReturn(null);
      return yrkande;
   }

   private static Handlaggning handlaggningMock(Yrkande yrkande) {
      Handlaggning handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.version()).thenReturn(2);
      when(handlaggning.yrkande()).thenReturn(yrkande);
      when(handlaggning.skapadTS()).thenReturn(OffsetDateTime.now());
      when(handlaggning.avslutadTS()).thenReturn(null);
      when(handlaggning.handlaggningspecifikationId()).thenReturn(UUID.randomUUID());
      return handlaggning;
   }

   @Test
   void isKompletteringRequired_returnsTrue_whenPersonnummerSaknasHelt() {
      Yrkande yrkande = yrkandeMock("avsikt", List.of());
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isTrue();
   }

   @Test
   void isKompletteringRequired_returnsTrue_whenPersonnummerVardeArBlankt() {
      Yrkande yrkande = yrkandeMock("avsikt", List.of(personnummerRoll("roll-1", "")));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isTrue();
   }

   @Test
   void isKompletteringRequired_returnsTrue_whenAvsiktSaknas() {
      Yrkande yrkande = yrkandeMock(null, List.of(personnummerRoll("roll-1", "199001011234")));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isTrue();
   }

   @Test
   void isKompletteringRequired_returnsTrue_whenAvsiktArBlank() {
      Yrkande yrkande = yrkandeMock("", List.of(personnummerRoll("roll-1", "199001011234")));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isTrue();
   }

   @Test
   void isKompletteringRequired_returnsFalse_whenPersonnummerOchAvsiktFinns() {
      Yrkande yrkande = yrkandeMock("avsikt", List.of(personnummerRoll("roll-1", "199001011234")));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isFalse();
   }

   @Test
   void readSvarData_extraherarPersonnummerOchAvsikt() {
      Yrkande yrkande = yrkandeMock("min avsikt", List.of(personnummerRoll("roll-1", "199001011234")));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      RtfKompletteringData data = service.readSvarData(handlaggning);

      assertThat(data.getPersonnummer()).isEqualTo("199001011234");
      assertThat(data.getAvsikt()).isEqualTo("min avsikt");
   }

   @Test
   void readSvarData_returnerarNullPersonnummer_omRollSaknas() {
      Yrkande yrkande = yrkandeMock("min avsikt", List.of());
      Handlaggning handlaggning = handlaggningMock(yrkande);

      RtfKompletteringData data = service.readSvarData(handlaggning);

      assertThat(data.getPersonnummer()).isNull();
      assertThat(data.getAvsikt()).isEqualTo("min avsikt");
   }

   @Test
   void registerSvar_uppdaterarBefintligPersonnummerRollMedBehallenRollId() {
      IndividYrkandeRoll befintligRoll = personnummerRoll("befintlig-roll-id", "gammaltVarde");
      IndividYrkandeRoll annanRoll = otherRoll("annan-roll-id");
      Yrkande yrkande = yrkandeMock("gammal avsikt", List.of(befintligRoll, annanRoll));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      RtfKompletteringData request = new RtfKompletteringData();
      request.setPersonnummer("199001011234");
      request.setAvsikt("ny avsikt");

      var update = service.registerSvar(handlaggning, request);

      assertThat(update.id()).isEqualTo(handlaggning.id());
      assertThat(update.version()).isEqualTo(handlaggning.version());
      assertThat(update.skapadTS()).isEqualTo(handlaggning.skapadTS());
      assertThat(update.avslutadTS()).isEqualTo(handlaggning.avslutadTS());
      assertThat(update.handlaggningspecifikationId()).isEqualTo(handlaggning.handlaggningspecifikationId());
      assertThat(update.yrkande().avsikt()).isEqualTo("ny avsikt");

      List<IndividYrkandeRoll> resultRoller = update.yrkande().individYrkandeRoller();
      assertThat(resultRoller).hasSize(2);

      Idtyp uppdateradPersonnummer = resultRoller.stream()
            .filter(r -> PERSONNUMMER_TYP_ID.equals(r.individ().typId()))
            .findFirst()
            .map(IndividYrkandeRoll::individ)
            .orElseThrow();
      assertThat(uppdateradPersonnummer.varde()).isEqualTo("199001011234");

      String bevaradRollId = resultRoller.stream()
            .filter(r -> PERSONNUMMER_TYP_ID.equals(r.individ().typId()))
            .map(IndividYrkandeRoll::yrkandeRollId)
            .findFirst()
            .orElseThrow();
      assertThat(bevaradRollId).isEqualTo("befintlig-roll-id");

      assertThat(resultRoller).anyMatch(r -> "annan-roll-id".equals(r.yrkandeRollId()));
   }

   @Test
   void registerSvar_skaparNyPersonnummerRoll_omIngenFinnsSedanTidigare() {
      Yrkande yrkande = yrkandeMock("gammal avsikt", List.of());
      Handlaggning handlaggning = handlaggningMock(yrkande);

      RtfKompletteringData request = new RtfKompletteringData();
      request.setPersonnummer("199001011234");
      request.setAvsikt("ny avsikt");

      var update = service.registerSvar(handlaggning, request);

      List<IndividYrkandeRoll> resultRoller = update.yrkande().individYrkandeRoller();
      assertThat(resultRoller).hasSize(1);

      IndividYrkandeRoll nyRoll = resultRoller.get(0);
      assertThat(nyRoll.individ().typId()).isEqualTo(PERSONNUMMER_TYP_ID);
      assertThat(nyRoll.individ().varde()).isEqualTo("199001011234");
      assertThat(UUID.fromString(nyRoll.yrkandeRollId())).isNotNull();
   }
}
