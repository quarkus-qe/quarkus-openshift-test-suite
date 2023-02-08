package io.quarkus.ts.openshift.messaging.artemisjta;

import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class PriceResource {

    @Inject
    ConsumerService c;

    @Inject
    ProducerService p;

    @POST
    @Path("/price-tx")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postCustomUsingTransactional(@QueryParam("fail") boolean fail,
            @NotNull String price) {
        p.produceCustomPrice(price, fail);
        return Response.ok().build();
    }

    @POST
    @Path("/price-non-tx")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postCustomWithoutTransactional(@QueryParam("fail") boolean fail,
            @NotNull String price) {
        p.produceCustomPriceNoJTA(price, fail);
        return Response.ok().build();
    }

    @GET
    @Path("/price-1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPriceOne() {
        return Response.ok(c.readPriceOne()).build();
    }

    @GET
    @Path("/price-2")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPriceTwo() {
        return Response.ok(c.readPriceTwo()).build();
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
        return Response.ok(c.receiveAndAck(ack)).build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public String root() {
        return "All good.";
    }
}
