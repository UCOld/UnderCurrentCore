package undercurrentcore.server;

import org.eclipse.jetty.server.Server;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import undercurrentcore.persist.UCConfiguration;
import undercurrentcore.server.servletHandlers.UCCoreImplServlet;

/**
 * Created by Niel Verster on 5/26/2015.
 */
public class ServerWrapper {

    private static Server server = new Server(UCConfiguration.getServerAPIPort());

    public static void startUCServer() {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/undercurrentcore");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new UCCoreImplServlet()), "/core");

        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopUCServer() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
