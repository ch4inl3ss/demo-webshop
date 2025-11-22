(function () {
    const storageKey = 'nerdshirts-passkeys';
    const registerButton = document.getElementById('passkeyRegisterButton');
    const loginButton = document.getElementById('passkeyLoginButton');
    const select = document.getElementById('passkeySelect');
    const emailField = document.getElementById('passkeyEmail');
    const labelField = document.getElementById('passkeyLabel');
    const messageBox = document.getElementById('passkeyMessage');

    if (!registerButton || !loginButton || !select || !emailField || !labelField || !messageBox) {
        return;
    }

    const readPasskeys = () => {
        try {
            const raw = localStorage.getItem(storageKey);
            return raw ? JSON.parse(raw) : [];
        } catch (err) {
            console.error('Passkey storage error', err);
            return [];
        }
    };

    const writePasskeys = (items) => {
        localStorage.setItem(storageKey, JSON.stringify(items));
    };

    const updateSelect = () => {
        const keys = readPasskeys();
        select.innerHTML = '';
        if (!keys.length) {
            const empty = document.createElement('option');
            empty.textContent = 'Kein Passkey vorhanden';
            empty.value = '';
            select.appendChild(empty);
            select.disabled = true;
            loginButton.disabled = true;
            return;
        }
        select.disabled = false;
        loginButton.disabled = false;
        keys.forEach((key, index) => {
            const option = document.createElement('option');
            option.value = key.credentialId;
            option.textContent = `${key.label} (${key.email})`;
            if (index === 0) {
                option.selected = true;
            }
            select.appendChild(option);
        });
    };

    const toBase64Url = (buffer) => {
        const bytes = new Uint8Array(buffer);
        let binary = '';
        bytes.forEach((byte) => {
            binary += String.fromCharCode(byte);
        });
        return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    };

    const fromBase64Url = (value) => {
        const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
        const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
        const binary = atob(padded);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i += 1) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes;
    };

    const spkiToPem = (buffer) => {
        const base64 = btoa(String.fromCharCode(...new Uint8Array(buffer)));
        const wrapped = base64.match(/.{1,64}/g)?.join('\n') ?? base64;
        return `-----BEGIN PUBLIC KEY-----\n${wrapped}\n-----END PUBLIC KEY-----`;
    };

    const setMessage = (text, type = 'secondary') => {
        messageBox.textContent = text;
        messageBox.className = '';
        messageBox.classList.add('alert', `alert-${type}`);
    };

    registerButton.addEventListener('click', async () => {
        const email = emailField.value.trim();
        if (!email) {
            setMessage('Bitte gib die E-Mail-Adresse deines Kontos an.', 'warning');
            return;
        }
        try {
            setMessage('Fordere Challenge an …', 'info');
            const optionsResponse = await fetch('/api/passkeys/register/options', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email })
            });
            if (!optionsResponse.ok) {
                const error = await optionsResponse.text();
                throw new Error(error || 'Challenge konnte nicht geladen werden');
            }
            const options = await optionsResponse.json();
            const keyPair = await window.crypto.subtle.generateKey({
                name: 'ECDSA',
                namedCurve: 'P-256'
            }, true, ['sign', 'verify']);
            const credentialId = crypto.randomUUID();
            const signatureBuffer = await window.crypto.subtle.sign({
                name: 'ECDSA',
                hash: 'SHA-256'
            }, keyPair.privateKey, fromBase64Url(options.challenge));
            const publicKeyBuffer = await window.crypto.subtle.exportKey('spki', keyPair.publicKey);
            const privateKey = await window.crypto.subtle.exportKey('jwk', keyPair.privateKey);
            const payload = {
                email,
                credentialId,
                publicKeyPem: spkiToPem(publicKeyBuffer),
                signature: toBase64Url(signatureBuffer),
                algorithm: 'SHA256withECDSA'
            };
            const registerResponse = await fetch('/api/passkeys/register/complete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!registerResponse.ok) {
                const error = await registerResponse.text();
                throw new Error(error || 'Registrierung fehlgeschlagen');
            }
            const keys = readPasskeys();
            keys.push({
                email,
                credentialId,
                label: labelField.value.trim() || `Passkey ${new Date().toLocaleDateString()}`,
                algorithm: 'SHA256withECDSA',
                privateKey
            });
            writePasskeys(keys);
            updateSelect();
            setMessage('Passkey wurde sicher für dieses Gerät gespeichert.', 'success');
        } catch (error) {
            console.error(error);
            setMessage(error.message || 'Passkey konnte nicht angelegt werden.', 'danger');
        }
    });

    loginButton.addEventListener('click', async () => {
        const email = emailField.value.trim();
        const credentialId = select.value;
        if (!email || !credentialId) {
            setMessage('Bitte wähle einen Passkey und gib die zugehörige E-Mail-Adresse an.', 'warning');
            return;
        }
        const keys = readPasskeys();
        const selected = keys.find((key) => key.credentialId === credentialId);
        if (!selected) {
            setMessage('Ausgewählter Passkey ist nicht mehr vorhanden.', 'danger');
            updateSelect();
            return;
        }
        if (selected.email !== email) {
            setMessage('Dieser Passkey gehört zu einer anderen E-Mail-Adresse.', 'warning');
            return;
        }
        try {
            setMessage('Fordere Challenge für den Login an …', 'info');
            const challengeResponse = await fetch('/api/passkeys/login/options', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, credentialId })
            });
            if (!challengeResponse.ok) {
                const error = await challengeResponse.text();
                throw new Error(error || 'Challenge konnte nicht geladen werden');
            }
            const options = await challengeResponse.json();
            const privateKey = await window.crypto.subtle.importKey('jwk', selected.privateKey, {
                name: 'ECDSA',
                namedCurve: 'P-256'
            }, false, ['sign']);
            const signatureBuffer = await window.crypto.subtle.sign({
                name: 'ECDSA',
                hash: 'SHA-256'
            }, privateKey, fromBase64Url(options.challenge));
            const loginResponse = await fetch('/api/passkeys/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email,
                    credentialId,
                    signature: toBase64Url(signatureBuffer),
                    algorithm: selected.algorithm || 'SHA256withECDSA'
                })
            });
            if (!loginResponse.ok) {
                const error = await loginResponse.text();
                throw new Error(error || 'Login fehlgeschlagen');
            }
            setMessage('Erfolgreich angemeldet. Du wirst weitergeleitet …', 'success');
            setTimeout(() => {
                window.location.href = '/';
            }, 800);
        } catch (error) {
            console.error(error);
            setMessage(error.message || 'Anmeldung mit Passkey nicht möglich.', 'danger');
        }
    });

    updateSelect();
})();
