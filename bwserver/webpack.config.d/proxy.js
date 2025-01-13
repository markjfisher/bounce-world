if (config.devServer) {
    config.devServer.proxy = [
        {
            // general kv resources I guess? need to probably use these from the web app to get to the services
            context: ["/kv/*", "/kvsse/*"],
            target: 'http://localhost:8080'
        },
        {
            // from the example app for ktor/kvision, which had login/logout functions
            context: ["/login", "/logout"],
            target: 'http://localhost:8080'
        },
        {
            // websockets
            context: ["/kvws/*"],
            target: 'http://localhost:8080',
            ws: true
        }
    ];
}