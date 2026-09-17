package se.fk.github.rtf.manuell.komplettering.presentation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;
import se.fk.rimfrost.framework.regel.komplettering.presentation.rest.RegelKompletteringController;
import se.fk.rimfrost.regel.rtf.manuell.komplettering.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

@Path("/regel/rtf-manuell-komplettering")
@ApplicationScoped
public class RtfKompletteringController extends RegelKompletteringController<RtfKompletteringData>
{

}
