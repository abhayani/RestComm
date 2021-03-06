package org.mobicents.servlet.restcomm.rvd.security;



import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.exceptions.UserNotAuthenticated;
import org.mobicents.servlet.restcomm.rvd.http.RvdResponse;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

public class AutheticationFilter implements ResourceFilter, ContainerRequestFilter {
    static final Logger logger = Logger.getLogger(AutheticationFilter.class.getName());

    public AutheticationFilter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return null;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {

        //logger.info("Running AutheticationFilter");
        //String path = request.getPath();
        //URI baseuri = request.getBaseUri();
        //Principal principal = request.getUserPrincipal();
        //SecurityContext secContext = request.getSecurityContext();
        //String authenticationScheme = secContext.getAuthenticationScheme();
        //Principal principal2 = secContext.getUserPrincipal();
        SecurityContext securityContext = request.getSecurityContext();


        Cookie ticketCookie = request.getCookies().get(RvdConfiguration.TICKET_COOKIE_NAME);
        if ( ticketCookie != null ) {
            String rawTicket = ticketCookie.getValue();
            String[] ticketParts = rawTicket.split(":");

            if ( ticketParts.length == 2 ) {
                String ticketUsername = ticketParts[0];
                String ticketId = ticketParts[1];
                //throw new WebApplicationException();
                //String ticketId = "111"; // simulate retrieving ticketId from a header

                //logger.debug("Received a request with ticket " + rawTicket);

                TicketRepository tickets =  TicketRepository.getInstance();
                tickets.remindStaleTicketRemoval();
                Ticket ticket = tickets.findTicket(ticketId);
                if ( ticket != null ) {
                    if ( ticket.getUserId() != null && ticket.getUserId().equals(ticketUsername) ) {
                        ticket.accessedNow();
                        RvdUser user = new RvdUser(ticket.getUserId());
                        securityContext = new RvdSecurityContext(user);
                        request.setSecurityContext(securityContext);
                        //logger.debug("granted access to request with ticket id" + ticketId);
                        return request;
                    }
                }
            }
            // Since access was not granted, this is probably an bad cookie. Remove it from the request so that it won't be renewed from the SessionKeepAliveFilter
            request.getCookies().remove(RvdConfiguration.TICKET_COOKIE_NAME);
        }

        //return request;

        logger.debug("denied access for request ");
        RvdException e = new UserNotAuthenticated();
        RvdResponse rvdResponse = new RvdResponse(RvdResponse.Status.ERROR).setException(e);
        Response res = Response.status(Status.UNAUTHORIZED).entity(rvdResponse.asJson()).type(MediaType.APPLICATION_JSON).build();
        throw new WebApplicationException( res );
    }

}
