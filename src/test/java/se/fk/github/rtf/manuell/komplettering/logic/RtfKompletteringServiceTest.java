package se.fk.github.rtf.manuell.komplettering.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.Idtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableRollIYrkande;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableSakfragaStallningstagandeContainer;
import se.fk.rimfrost.framework.handlaggning.model.RollIYrkande;
import se.fk.rimfrost.framework.handlaggning.model.SakfragaStallningstagandeContainer;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.regel.rtf.manuell.komplettering.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

class RtfKompletteringServiceTest
{

   private static final String PERSONNUMMER_TYP_ID = "personnummer";

   private RtfKompletteringService service;

   @BeforeEach
   void setUp()
   {
      service = new RtfKompletteringService();
      service.objectMapper = new ObjectMapper();
   }

   private static RollIYrkande personnummerRoll(String yrkandeRollId, String varde)
   {
      return ImmutableRollIYrkande.builder()
            .id(UUID.randomUUID())
            .individ(ImmutableIdtyp.builder()
                  .typId(PERSONNUMMER_TYP_ID)
                  .varde(varde)
                  .build())
            .yrkandeRollId(yrkandeRollId)
            .avserYrkande(UUID.randomUUID())
            .build();
   }

   private static RollIYrkande otherRoll(String yrkandeRollId)
   {
      return ImmutableRollIYrkande.builder()
            .id(UUID.randomUUID())
            .individ(ImmutableIdtyp.builder()
                  .typId("annanTyp")
                  .varde("annatVarde")
                  .build())
            .yrkandeRollId(yrkandeRollId)
            .avserYrkande(UUID.randomUUID())
            .build();
   }

   private static SakfragaStallningstagandeContainer folkbokforingContainer()
   {
      return ImmutableSakfragaStallningstagandeContainer.builder()
            .id(UUID.randomUUID())
            .objektTypId("folkbokforing")
            .data("{\"id\":\"fb-test-1\",\"fornamn\":\"Test\",\"efternamn\":\"Testsson\","
                  + "\"kon\":\"MAN\",\"utdelningsadress\":\"Testgatan 1\","
                  + "\"postnummer\":\"12345\",\"postort\":\"Teststaden\",\"careOf\":\"\"}")
            .build();
   }

   private static SakfragaStallningstagandeContainer folkbokforingContainerOfullstandig()
   {
      return ImmutableSakfragaStallningstagandeContainer.builder()
            .id(UUID.randomUUID())
            .objektTypId("folkbokforing")
            .data("{\"id\":\"fb-test-2\",\"utdelningsadress\":\"Testgatan 1\","
                  + "\"postnummer\":\"12345\",\"postort\":\"Teststaden\",\"careOf\":\"\"}")
            .build();
   }

   private static Yrkande yrkandeMock(String avsikt, List<RollIYrkande> roller)
   {
      return yrkandeMock(avsikt, roller, List.of());
   }

   private static Yrkande yrkandeMock(String avsikt, List<RollIYrkande> roller,
         List<SakfragaStallningstagandeContainer> sakfragor)
   {
      Yrkande yrkande = mock(Yrkande.class);
      when(yrkande.id()).thenReturn(UUID.randomUUID());
      when(yrkande.version()).thenReturn(1);
      when(yrkande.ingangtypId()).thenReturn("erbjudande-1");
      when(yrkande.yrkandeDatum()).thenReturn(OffsetDateTime.now());
      when(yrkande.yrkandeStatus()).thenReturn("PAGAENDE");
      when(yrkande.yrkandeFrom()).thenReturn(OffsetDateTime.now());
      when(yrkande.yrkandeTom()).thenReturn(OffsetDateTime.now().plusDays(1));
      when(yrkande.avsikt()).thenReturn(avsikt);
      when(yrkande.rollerIYrkande()).thenReturn(roller);
      when(yrkande.sakfragorStallningstaganden()).thenReturn(sakfragor);
      when(yrkande.beslut()).thenReturn(List.of());
      return yrkande;
   }

   private static Handlaggning handlaggningMock(Yrkande yrkande)
   {
      Handlaggning handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.version()).thenReturn(2);
      when(handlaggning.yrkande()).thenReturn(yrkande);
      when(handlaggning.skapadTS()).thenReturn(OffsetDateTime.now());
      when(handlaggning.avslutadTS()).thenReturn(null);
      when(handlaggning.handlaggningspecifikationId()).thenReturn(UUID.randomUUID());
      when(handlaggning.handlaggningIdTyp()).thenReturn(UUID.randomUUID().toString());
      return handlaggning;
   }

   @Test
   void isKompletteringRequired_returnsTrue_whenPersonnummerSaknasHelt()
   {
      Yrkande yrkande = yrkandeMock("avsikt", List.of());
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isTrue();
   }

   @Test
   void isKompletteringRequired_returnsTrue_whenPersonnummerVardeArBlankt()
   {
      Yrkande yrkande = yrkandeMock("avsikt", List.of(personnummerRoll("roll-1", "")));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isTrue();
   }

   @Test
   void isKompletteringRequired_returnsTrue_whenAvsiktSaknas()
   {
      Yrkande yrkande = yrkandeMock(null, List.of(personnummerRoll("roll-1", "199001011234")));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isTrue();
   }

   @Test
   void isKompletteringRequired_returnsTrue_whenAvsiktArBlank()
   {
      Yrkande yrkande = yrkandeMock("", List.of(personnummerRoll("roll-1", "199001011234")));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isTrue();
   }

   @Test
   @DisplayName("RTFMK-FR-08.4: isKompletteringRequired returnerar true när folkbokföring data saknar obligatoriska fält")
   void isKompletteringRequired_returnsTrue_whenFolkbokforingDataArOfullstandig()
   {
      Yrkande yrkande = yrkandeMock("avsikt", List.of(personnummerRoll("roll-1", "199001011234")),
            List.of(folkbokforingContainerOfullstandig()));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isTrue();
   }

   @Test
   @DisplayName("RTFMK-FR-08.4: isKompletteringRequired returnerar true när folkbokföring saknas i sakfragorStallningstaganden")
   void isKompletteringRequired_returnsTrue_whenFolkbokforingSaknas()
   {
      Yrkande yrkande = yrkandeMock("avsikt", List.of(personnummerRoll("roll-1", "199001011234")));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isTrue();
   }

   @Test
   @DisplayName("RTFMK-FR-08.3: isKompletteringRequired returnerar false när personnummer, avsikt och folkbokföring alla finns")
   void isKompletteringRequired_returnsFalse_whenPersonnummerAvsiktOchFolkbokforingFinns()
   {
      Yrkande yrkande = yrkandeMock("avsikt", List.of(personnummerRoll("roll-1", "199001011234")),
            List.of(folkbokforingContainer()));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.isKompletteringRequired(handlaggning)).isFalse();
   }

   @Test
   void readSvarData_extraherarPersonnummerOchAvsikt()
   {
      Yrkande yrkande = yrkandeMock("min avsikt", List.of(personnummerRoll("roll-1", "199001011234")));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      RtfKompletteringData data = service.readSvarData(handlaggning);

      assertThat(data.getPersonnummer()).isEqualTo("199001011234");
      assertThat(data.getAvsikt()).isEqualTo("min avsikt");
   }

   @Test
   void readSvarData_returnerarNullPersonnummer_omRollSaknas()
   {
      Yrkande yrkande = yrkandeMock("min avsikt", List.of());
      Handlaggning handlaggning = handlaggningMock(yrkande);

      RtfKompletteringData data = service.readSvarData(handlaggning);

      assertThat(data.getPersonnummer()).isNull();
      assertThat(data.getAvsikt()).isEqualTo("min avsikt");
   }

   @Test
   void registerSvar_uppdaterarBefintligPersonnummerRollMedBehallenRollId()
   {
      RollIYrkande befintligRoll = personnummerRoll("befintlig-roll-id", "gammaltVarde");
      RollIYrkande annanRoll = otherRoll("annan-roll-id");
      Yrkande yrkande = yrkandeMock("gammal avsikt", List.of(befintligRoll, annanRoll));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      RtfKompletteringData request = new RtfKompletteringData();
      request.setPersonnummer("199001011234");
      request.setAvsikt("ny avsikt");

      var update = service.registerSvar(handlaggning, request);

      assertThat(update.handlaggning().id()).isEqualTo(handlaggning.id());
      assertThat(update.handlaggning().version()).isEqualTo(handlaggning.version());
      assertThat(update.handlaggning().skapadTS()).isEqualTo(handlaggning.skapadTS());
      assertThat(update.handlaggning().avslutadTS()).isEqualTo(handlaggning.avslutadTS());
      assertThat(update.handlaggning().handlaggningspecifikationId()).isEqualTo(handlaggning.handlaggningspecifikationId());
      assertThat(update.handlaggning().yrkande().avsikt()).isEqualTo("ny avsikt");

      List<RollIYrkande> resultRoller = update.handlaggning().yrkande().rollerIYrkande();
      assertThat(resultRoller).hasSize(2);

      Idtyp uppdateradPersonnummer = resultRoller.stream()
            .filter(r -> PERSONNUMMER_TYP_ID.equals(r.individ().typId()))
            .findFirst()
            .map(RollIYrkande::individ)
            .orElseThrow();
      assertThat(uppdateradPersonnummer.varde()).isEqualTo("199001011234");

      String bevaradRollId = resultRoller.stream()
            .filter(r -> PERSONNUMMER_TYP_ID.equals(r.individ().typId()))
            .map(RollIYrkande::yrkandeRollId)
            .findFirst()
            .orElseThrow();
      assertThat(bevaradRollId).isEqualTo("befintlig-roll-id");

      assertThat(resultRoller).anyMatch(r -> "annan-roll-id".equals(r.yrkandeRollId()));
   }

   @Test
   @DisplayName("RTFMK-FR-09: getPrenumerationsIdentiteter returnerar tom lista när rollerIYrkande är tom")
   void getPrenumerationsIdentiteter_returnerarTomLista_narRollerArTomma()
   {
      Yrkande yrkande = yrkandeMock("avsikt", List.of());
      Handlaggning handlaggning = handlaggningMock(yrkande);

      assertThat(service.getPrenumerationsIdentiteter(handlaggning)).isEmpty();
   }

   @Test
   @DisplayName("RTFMK-FR-09: getPrenumerationsIdentiteter returnerar personnummer-identiteter inklusive de med tomt värde")
   void getPrenumerationsIdentiteter_returnerarPersonnummerIdentiteter()
   {
      RollIYrkande medVarde = personnummerRoll("roll-1", "199001011234");
      RollIYrkande tomtVarde = personnummerRoll("roll-2", "");
      Yrkande yrkande = yrkandeMock("avsikt", List.of(medVarde, tomtVarde));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      List<Idtyp> result = service.getPrenumerationsIdentiteter(handlaggning);

      assertThat(result).hasSize(2);
      assertThat(result).extracting(Idtyp::typId).containsOnly(PERSONNUMMER_TYP_ID);
      assertThat(result).extracting(Idtyp::varde).containsExactlyInAnyOrder("199001011234", "");
   }

   @Test
   @DisplayName("RTFMK-FR-09: getPrenumerationsIdentiteter utesluter roller av annan typ")
   void getPrenumerationsIdentiteter_utesluter_rollerAvAnnanTyp()
   {
      RollIYrkande personnummerRoll = personnummerRoll("roll-1", "199001011234");
      RollIYrkande annanRoll = otherRoll("roll-2");
      Yrkande yrkande = yrkandeMock("avsikt", List.of(personnummerRoll, annanRoll));
      Handlaggning handlaggning = handlaggningMock(yrkande);

      List<Idtyp> result = service.getPrenumerationsIdentiteter(handlaggning);

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().typId()).isEqualTo(PERSONNUMMER_TYP_ID);
      assertThat(result.getFirst().varde()).isEqualTo("199001011234");
   }

   @Test
   void registerSvar_skaparNyPersonnummerRoll_omIngenFinnsSedanTidigare()
   {
      Yrkande yrkande = yrkandeMock("gammal avsikt", List.of());
      Handlaggning handlaggning = handlaggningMock(yrkande);

      RtfKompletteringData request = new RtfKompletteringData();
      request.setPersonnummer("199001011234");
      request.setAvsikt("ny avsikt");

      var update = service.registerSvar(handlaggning, request);

      List<RollIYrkande> resultRoller = update.handlaggning().yrkande().rollerIYrkande();
      assertThat(resultRoller).hasSize(1);

      RollIYrkande nyRoll = resultRoller.getFirst();
      assertThat(nyRoll.individ().typId()).isEqualTo(PERSONNUMMER_TYP_ID);
      assertThat(nyRoll.individ().varde()).isEqualTo("199001011234");
      assertThat(UUID.fromString(nyRoll.yrkandeRollId())).isNotNull();
   }
}
