const registration = await navigator.serviceWorker.register("sw.js");

navigator.serviceWorker.addEventListener("message", event => {
    document.body.textContent = event.data;
});

await Notification.requestPermission();

const applicationServerKey = await fetch("/vapid")

const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: new Uint8Array(await applicationServerKey.arrayBuffer()),
});

await fetch("/send", {method: "post", body: JSON.stringify(subscription.toJSON())});
