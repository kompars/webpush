self.addEventListener("push", (event) => {
    event.waitUntil(
        self.clients.matchAll({includeUncontrolled: true}).then(windows => {
            windows[0].postMessage(event.data.text());
        })
    );
});
