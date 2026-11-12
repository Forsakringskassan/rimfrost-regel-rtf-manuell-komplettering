package se.fk.github.rtf.manuell.komplettering.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.Idtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableRollIYrkande;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableYrkande;
import se.fk.rimfrost.framework.handlaggning.model.RollIYrkande;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringService;
import se.fk.rimfrost.regel.rtf.manuell.komplettering.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

@ApplicationScoped
public class RtfKompletteringService implements RegelKompletteringService<RtfKompletteringData>
{
   @Inject
   ObjectMapper objectMapper;

   @Override
   public boolean isKompletteringRequired(Handlaggning handlaggning)
   {
      var yrkande = handlaggning.yrkande();

      boolean harPersonnummer = yrkande.rollerIYrkande().stream()
            .anyMatch(r -> "personnummer".equals(r.individ().typId())
                  && r.individ().varde() != null
                  && !r.individ().varde().isBlank());

      boolean saknarAvsikt = yrkande.avsikt() == null || yrkande.avsikt().isBlank();

      boolean saknarFolkbokforing = yrkande.sakfragorStallningstaganden().stream()
            .filter(s -> "folkbokforing".equals(s.objektTypId()))
            .noneMatch(s -> harGiltigFolkbokforingData(s.data()));

      return !harPersonnummer || saknarAvsikt || saknarFolkbokforing;
   }

   /**
    * Returns true if the JSON data string contains non-blank values for all required
    * folkbokföring fields: {@code fornamn}, {@code efternamn}, {@code kon},
    * {@code utdelningsadress}, {@code postnummer}, {@code postort}.
    */
   private boolean harGiltigFolkbokforingData(String data)
   {
      try
      {
         JsonNode node = objectMapper.readTree(data);
         return isNonBlank(node, "fornamn")
               && isNonBlank(node, "efternamn")
               && isNonBlank(node, "kon")
               && isNonBlank(node, "utdelningsadress")
               && isNonBlank(node, "postnummer")
               && isNonBlank(node, "postort");
      }
      catch (JsonProcessingException e)
      {
         return false;
      }
   }

   private static boolean isNonBlank(JsonNode node, String field)
   {
      var value = node.path(field).asText(null);
      return value != null && !value.isBlank();
   }

   /**
    * Returns all personnummer identities from the yrkande for identity subscription (RTFMK-FR-09).
    *
    * @param handlaggning the current handlaggning
    * @return personnummer identities to subscribe to
    */
   @Override
   public List<Idtyp> getPrenumerationsIdentiteter(Handlaggning handlaggning)
   {
      return handlaggning.yrkande().rollerIYrkande().stream()
            .filter(r -> "personnummer".equals(r.individ().typId()))
            .map(RollIYrkande::individ)
            .toList();
   }

   @Override
   public RtfKompletteringData readSvarData(Handlaggning handlaggning)
   {
      var yrkande = handlaggning.yrkande();
      var personnummer = yrkande.rollerIYrkande().stream()
            .filter(r -> "personnummer".equals(r.individ().typId()))
            .map(r -> r.individ().varde())
            .findFirst()
            .orElse(null);

      var data = new RtfKompletteringData();
      data.setPersonnummer(personnummer);
      data.setAvsikt(yrkande.avsikt());
      return data;
   }

   @Override
   public HandlaggningUpdate registerSvar(Handlaggning handlaggning, RtfKompletteringData request)
   {
      var yrkande = handlaggning.yrkande();

      var existingRollId = yrkande.rollerIYrkande().stream()
            .filter(r -> "personnummer".equals(r.individ().typId()))
            .map(RollIYrkande::yrkandeRollId)
            .findFirst()
            .orElse(UUID.randomUUID().toString());

      var updatedRoller = new ArrayList<>(
            yrkande.rollerIYrkande().stream()
                  .filter(r -> !"personnummer".equals(r.individ().typId()))
                  .toList());

      updatedRoller.add(ImmutableRollIYrkande.builder()
            .id(UUID.randomUUID())
            .individ(ImmutableIdtyp.builder()
                  .typId("personnummer")
                  .varde(request.getPersonnummer())
                  .build())
            .yrkandeRollId(existingRollId)
            .avserYrkande(yrkande.id())
            .build());

      var updatedYrkande = ImmutableYrkande.builder().from(yrkande)
            .version(yrkande.version() + 1)
            .avsikt(request.getAvsikt())
            .rollerIYrkande(updatedRoller).build();

      var updatedHandlaggning = ImmutableHandlaggning.builder().from(handlaggning)
            .yrkande(updatedYrkande)
            .build();

      return ImmutableHandlaggningUpdate.builder().handlaggning(updatedHandlaggning).build();
   }

}
