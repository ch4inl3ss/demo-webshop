// Passkey-Frontend: übersetzt zwischen den JSON-Optionen der Yubico-Library
// (base64url-Strings) und der WebAuthn-Browser-API (ArrayBuffer).
(function () {
    'use strict';

    function b64urlToBuffer(value) {
        const base64 = value.replace(/-/g, '+').replace(/_/g, '/');
        const padded = base64 + '='.repeat((4 - base64.length % 4) % 4);
        const binary = atob(padded);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        return bytes.buffer;
    }

    function bufferToB64url(buffer) {
        const bytes = new Uint8Array(buffer);
        let binary = '';
        for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
        return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    }

    async function postJson(url, body) {
        const response = await fetch(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
            credentials: 'same-origin',
            body: body === undefined ? undefined : JSON.stringify(body)
        });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.message || ('HTTP ' + response.status));
        return data;
    }

    function showMessage(text, kind) {
        const el = document.getElementById('passkey-message');
        if (!el) { alert(text); return; }
        el.hidden = false;
        el.className = 'alert alert-' + kind;
        el.textContent = text;
    }

    async function registerPasskey() {
        try {
            const {publicKey} = await postJson('/passkeys/register/options');
            publicKey.challenge = b64urlToBuffer(publicKey.challenge);
            publicKey.user.id = b64urlToBuffer(publicKey.user.id);
            (publicKey.excludeCredentials || []).forEach(c => c.id = b64urlToBuffer(c.id));

            const credential = await navigator.credentials.create({publicKey});
            const payload = {
                id: credential.id,
                rawId: bufferToB64url(credential.rawId),
                type: credential.type,
                response: {
                    clientDataJSON: bufferToB64url(credential.response.clientDataJSON),
                    attestationObject: bufferToB64url(credential.response.attestationObject),
                    transports: credential.response.getTransports ? credential.response.getTransports() : []
                },
                clientExtensionResults: credential.getClientExtensionResults()
            };
            const label = (navigator.userAgentData && navigator.userAgentData.platform) || 'Passkey';
            await postJson('/passkeys/register?label=' + encodeURIComponent(label), payload);
            showMessage('Passkey registriert.', 'success');
            setTimeout(() => window.location.reload(), 600);
        } catch (e) {
            showMessage('Passkey konnte nicht registriert werden: ' + e.message, 'error');
        }
    }

    async function loginWithPasskey() {
        try {
            const {publicKey} = await postJson('/passkeys/login/options');
            publicKey.challenge = b64urlToBuffer(publicKey.challenge);
            (publicKey.allowCredentials || []).forEach(c => c.id = b64urlToBuffer(c.id));

            const credential = await navigator.credentials.get({publicKey});
            const payload = {
                id: credential.id,
                rawId: bufferToB64url(credential.rawId),
                type: credential.type,
                response: {
                    clientDataJSON: bufferToB64url(credential.response.clientDataJSON),
                    authenticatorData: bufferToB64url(credential.response.authenticatorData),
                    signature: bufferToB64url(credential.response.signature),
                    userHandle: credential.response.userHandle ? bufferToB64url(credential.response.userHandle) : null
                },
                clientExtensionResults: credential.getClientExtensionResults()
            };
            const result = await postJson('/passkeys/login', payload);
            window.location.href = result.redirect || '/';
        } catch (e) {
            showMessage('Passkey-Login fehlgeschlagen: ' + e.message, 'error');
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        const reg = document.getElementById('register-passkey');
        if (reg) reg.addEventListener('click', registerPasskey);
        const login = document.getElementById('login-passkey');
        if (login) {
            if (!window.PublicKeyCredential) login.hidden = true;
            login.addEventListener('click', loginWithPasskey);
        }
    });
})();
