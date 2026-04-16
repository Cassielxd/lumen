# Lumen Web Demo

Vue 3 + Element Plus demo for the current monolith backend.

## Features

- Switch OAuth2 `client`
- Login by password / OTP / Passkey
- Show different dashboard content after login
- Inspect current sessions
- Register and manage current account passkeys

## Default demo accounts

- Username: `admin`
- Password: `123456`
- OTP mobile: `17034642999`

## Recommended clients

- `test`: no captcha, good for local debugging
- `app`: member-facing example
- `daemon`: community staff example
- `lumen`: platform staff example

## Run

```bash
npm install
npm run dev
```

Use `http://localhost:5173` for the demo page.

The Vite dev server proxies `/admin/*` to `http://localhost:9999`.

Passkey notes:

- Use `localhost`, not `127.0.0.1`
- Or use a real HTTPS domain
- WebAuthn registration and login will be rejected on IP-address hosts
