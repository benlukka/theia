import * as Sentry from "@sentry/react";
Sentry.init({
    dsn: "https://ee192e480fa3d8f55d6d9398271b7208@o4509379436150789.ingest.de.sentry.io/4509386242654288",
    tracesSampleRate: 1.0,
    _experiments: { enableLogs: true },
    integrations: [
        // send console.log, console.error, and console.warn calls as logs to Sentry
        Sentry.captureConsoleIntegration({ levels: [ "error", "warn"] }),
    ],
});
