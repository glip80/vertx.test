package com.example.demo;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import in.yunyul.vertx.console.base.WebConsoleRegistry;
import in.yunyul.vertx.console.eventbus.EventBusConsolePage;
import in.yunyul.vertx.console.health.HealthConsolePage;
import in.yunyul.vertx.console.metrics.MetricsConsolePage;
import in.yunyul.vertx.console.pools.PoolsConsolePage;
import in.yunyul.vertx.console.shell.ShellConsolePage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;

public class MainVerticle extends AbstractVerticle {
    
    public static void main(final String[] args) {
        Launcher.executeCommand("run", MainVerticle.class.getName());
    }
    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                .putHeader("content-type", "text/html")
                .end("<h1>Hello from my first Vert.x 3 application</h1>");
        });
        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(8080);

        MetricRegistry dropwizardRegistry = SharedMetricRegistries.getOrCreate(
            System.getProperty("vertx.metrics.options.registryName") // or use hardcoded name
        );
        HealthChecks hc = HealthChecks.create(vertx);

        hc.register("my-procedure", future -> future.complete(Status.OK()));

// Register with a timeout. The check fails if it does not complete in time.
// The timeout is given in ms.
        hc.register("my-HealthCheck", 2000, future -> future.complete(Status.OK()));
        // Set up web console registry
        MetricsService metricsService = MetricsService.create(vertx);

        WebConsoleRegistry.create("/admin")
            // Add pages
            .addPage(MetricsConsolePage.create(dropwizardRegistry))
//            .addPage(ServicesConsolePage.create(discovery))
//            .addPage(LoggingConsolePage.create())
//            .addPage(CircuitBreakersConsolePage.create())
            .addPage(ShellConsolePage.create())
            .addPage(HealthConsolePage.create(hc))
            .addPage(PoolsConsolePage.create(metricsService))
            .addPage(EventBusConsolePage.create(metricsService))
            .setCacheBusterEnabled(false) // Adds random query string to scripts
            // Mount to router
            .mount(vertx, router);
        System.out.println("HTTP server started on port 8080");
    }
}
