package com.modak.ratelimitednotificationservice.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingConfig  {


    @Autowired
    public RateLimiterRegistry rateLimiterRegistry;


    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("notifications_route", r -> r
                        .path("/notification/**")
                        .filters(f -> f.filter(rateLimitFilter()))
                        .uri("https://3bea8ced-1067-452a-9421-ee52278174d2.mock.pstmn.io/notification")) // URI destino
                .build();
    }


    private GatewayFilter rateLimitFilter() {
        return (exchange, chain) -> {
            String recipient = exchange.getRequest().getHeaders().getFirst("X-TypeNotification");
            String user = exchange.getRequest().getHeaders().getFirst("X-User");

            if (recipient != null && user != null) {
                RateLimiter rateLimiter = getRateLimiter(recipient,user);
                if (rateLimiter != null) {
                    try {
                        rateLimiter.acquirePermission();
                        return chain.filter(exchange);
                    } catch (RequestNotPermitted e) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                } else {
                    exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                    return exchange.getResponse().setComplete();
                }
            }
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        };
    }

    private RateLimiter getRateLimiter(String recipient, String user) {

            return switch (recipient) {
                case "Status" ->  rateLimiterRegistry.rateLimiter("statusRateLimiter");
                case "News" -> rateLimiterRegistry.rateLimiter("newsRateLimiter");
                case "Marketing" ->  rateLimiterRegistry.rateLimiter("marketingRateLimiter");
                default -> null; // Destinatario no válido
            };



    }


}