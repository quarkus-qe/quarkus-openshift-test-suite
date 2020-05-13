package io.quarkus.ts.openshift.messaging.artemisjta;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    public Response postCustom(@QueryParam("fail") boolean fail,
                               @QueryParam("transactional") boolean transactional,
                               @NotNull String price) {
        if (transactional) {
            p.produceCustomPrice(price, fail);
        } else {
            p.produceCustomPriceNoJTA(price, fail);
        }
        return Response.ok().build();
    }

    @GET
    @Path("/price")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCustom() {
        return Response.ok(c.getPrice(), MediaType.TEXT_PLAIN).build();
    }

    @DELETE
    @Path("/price")
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteCustom() {
        c.setPrice1("");
        c.setPrice2("");
        return Response.ok().build();
    }

    @POST
    @Path("/noAck")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postNoAck(@NotNull String price) {
        p.produceClientAck(price);
        return Response.ok().build();
    }

    @GET
    @Path("/noAck")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getNoAck(@QueryParam("ack") boolean ack) throws JMSException {
        return Response.ok(c.receiveAndAck(ack), MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public String root() {
        return "All good.";
    }
}
