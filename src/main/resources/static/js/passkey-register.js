// Weiteren Passkey registrieren und den Passkey-Manager über gültige Passkeys informieren
async function registerPasskey(csrf, label) {
    const csrfHeader = { [csrf.header]: csrf.token };

    const optionsResponse = await fetch("/webauthn/register/options", {
        method: "POST",
        headers: csrfHeader,
    });
    const options = PublicKeyCredential.parseCreationOptionsFromJSON(await optionsResponse.json());

    const credential = await navigator.credentials.create({ publicKey: options });

    const registerResponse = await fetch("/webauthn/register", {
        method: "POST",
        headers: { ...csrfHeader, "Content-Type": "application/json" },
        body: JSON.stringify({ publicKey: { credential: credential.toJSON(), label } }),
    });
    if (!registerResponse.ok || !(await registerResponse.json()).success) {
        throw new Error(`Registrierung fehlgeschlagen: HTTP ${registerResponse.status}`);
    }
}

// WebAuthn Signal API: gelöschte Passkeys verschwinden auch aus dem Passkey-Manager
async function signalAcceptedCredentials(list) {
    if (!list.dataset.userHandle || !PublicKeyCredential.signalAllAcceptedCredentials) {
        return;
    }
    await PublicKeyCredential.signalAllAcceptedCredentials({
        rpId: location.hostname,
        userId: list.dataset.userHandle,
        allAcceptedCredentialIds: [...list.querySelectorAll("[data-credential-id]")].map(e => e.dataset.credentialId),
    });
}

document.addEventListener("DOMContentLoaded", () => {
    const csrf = {
        header: document.querySelector("meta[name=_csrf_header]").content,
        token: document.querySelector("meta[name=_csrf]").content,
    };
    const form = document.getElementById("passkey-add");
    const error = document.getElementById("passkey-error");
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        error.hidden = true;
        try {
            await registerPasskey(csrf, form.label.value);
            window.location.href = "/konto/passkeys?hinzugefuegt";
        } catch (e) {
            console.error(e);
            error.hidden = false;
        }
    });
    signalAcceptedCredentials(document.getElementById("passkey-list")).catch(console.error);
});
