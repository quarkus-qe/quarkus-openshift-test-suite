package io.quarkus.ts.openshift.messaging.artemisjta;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static io.quarkus.ts.openshift.messaging.artemisjta.ConsumerService.exToS;

@Path("/")
public class PriceResource {

    @Inject
    ConsumerService c;

    @Inject
    ProducerService p;

    @POST
    @Path("/price")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response postCustom(@QueryParam("fail") boolean fail, @NotNull String price) {
        try {
            p.produceCustomPrice(price, fail);
        } catch (Exception e) {
            return Response.serverError().entity(exToS(e)).build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("/NoJTAPrice")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postCustomNoJTA(@QueryParam("fail") boolean fail, @NotNull String price) {
        try {
            p.produceCustomPriceNoJTA(price, fail);
        } catch (Exception e) {
            return Response.serverError().entity(exToS(e)).build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("/price")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCustom() {
        return Response.ok(c.getPrice(), MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public String root() {
        return "All good.";
    }

}
