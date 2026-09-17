package se.fk.github.rtf.manuell.komplettering.logic;

import java.util.ArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableYrkande;
import se.fk.rimfrost.framework.handlaggning.model.IndividYrkandeRoll;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringService;
import se.fk.rimfrost.regel.rtf.manuell.komplettering.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

@ApplicationScoped
public class RtfKompletteringService implements RegelKompletteringService<RtfKompletteringData>
{

   @Override
   public boolean isKompletteringRequired(Handlaggning handlaggning)
   {
      var yrkande = handlaggning.yrkande();

      boolean harPersonnummer = yrkande.individYrkandeRoller().stream()
            .anyMatch(r -> "personnummer".equals(r.individ().typId())
                  && r.individ().varde() != null
                  && !r.individ().varde().isBlank());

      boolean saknarAvsikt = yrkande.avsikt() == null || yrkande.avsikt().isBlank();

      return !harPersonnummer || saknarAvsikt;
   }

   @Override
   public RtfKompletteringData readSvarData(Handlaggning handlaggning)
   {
      var yrkande = handlaggning.yrkande();
      var personnummer = yrkande.individYrkandeRoller().stream()
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

      var existingRollId = yrkande.individYrkandeRoller().stream()
            .filter(r -> "personnummer".equals(r.individ().typId()))
            .map(IndividYrkandeRoll::yrkandeRollId)
            .findFirst()
            .orElse(UUID.randomUUID().toString());

      var updatedRoller = new ArrayList<IndividYrkandeRoll>(
            yrkande.individYrkandeRoller().stream()
                  .filter(r -> !"personnummer".equals(r.individ().typId()))
                  .toList());

      updatedRoller.add(ImmutableIndividYrkandeRoll.builder()
            .individ(ImmutableIdtyp.builder()
                  .typId("personnummer")
                  .varde(request.getPersonnummer())
                  .build())
            .yrkandeRollId(existingRollId)
            .build());

      var updatedYrkande = ImmutableYrkande.builder().from(yrkande)
            .avsikt(request.getAvsikt())
            .individYrkandeRoller(updatedRoller).build();

      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(handlaggning.version())
            .yrkande(updatedYrkande)
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .build();
   }

}
