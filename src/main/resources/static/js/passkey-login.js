// Login mit Passkey gegen die Endpunkte von Spring Security
async function passkeyLogin(csrf) {
    const csrfHeader = { [csrf.header]: csrf.token };

    // 1. Challenge holen
    const optionsResponse = await fetch("/webauthn/authenticate/options", {
        method: "POST",
        headers: csrfHeader,
    });
    const options = PublicKeyCredential.parseRequestOptionsFromJSON(await optionsResponse.json());

    // 2. Browser nach dem Passkey fragen
    const credential = await navigator.credentials.get({ publicKey: options });

    // 3. Signatur prüfen lassen, Session anlegen
    const loginResponse = await fetch("/login/webauthn", {
        method: "POST",
        headers: { ...csrfHeader, "Content-Type": "application/json" },
        body: JSON.stringify({ ...credential.toJSON(), credType: credential.type }),
    });
    if (!loginResponse.ok) {
        throw new Error(`Login fehlgeschlagen: HTTP ${loginResponse.status}`);
    }
    const { redirectUrl } = await loginResponse.json();
    window.location.href = redirectUrl;
}

document.addEventListener("DOMContentLoaded", () => {
    const button = document.getElementById("passkey-login");
    const error = document.getElementById("passkey-error");
    const csrf = {
        header: document.querySelector("meta[name=_csrf_header]").content,
        token: document.querySelector("meta[name=_csrf]").content,
    };
    button.addEventListener("click", async () => {
        error.hidden = true;
        try {
            await passkeyLogin(csrf);
        } catch (e) {
            console.error(e);
            error.hidden = false;
        }
    });
});
