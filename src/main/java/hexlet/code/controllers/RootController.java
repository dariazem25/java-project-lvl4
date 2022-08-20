package hexlet.code.controllers;

import io.javalin.http.Handler;

public final class RootController {

    private static Handler welcome;

    public static Handler getWelcome() {
        welcome = ctx -> {
            ctx.render("index.html");
        };
        return welcome;
    }
}
