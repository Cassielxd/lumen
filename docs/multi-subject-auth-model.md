# Client-Centric OAuth2 Authentication Model

## Positioning

The current project should stay on the existing OAuth2 authorization-server model.

At this stage:

- `client` is the business audience boundary
- `account` is the logged-in principal
- `grant_type` is the login and token issuance mechanism
- no extra `subject_type` layer is needed
- no extra `login_method` relation table is needed

The current system already has a working client-grant authorization mechanism. The correct extension direction is to extend OAuth2 grants, not to build a second authorization layer beside OAuth2.

## Core Objects

### Account

`account` is the concrete principal that logs in.

It provides:

- identity
- password or other credential material
- authorities and business permissions

### Client

`client` is the audience boundary and application boundary.

Examples:

- `member-web`
- `member-ios`
- `community-console-web`
- `platform-console-web`

In the current codebase, a client is stored in `sys_oauth_client_details`.

### Grant Type

`grant_type` is the mechanism used by the client to obtain a token.

The project already uses:

- standard grants such as `authorization_code`, `refresh_token`, `client_credentials`
- custom grants such as `password`
- legacy custom mobile-code login, currently compatible with `mobile` and `otp`

For this project, human login expansion should continue through custom grant types.

### Session And Authorization

Runtime token and authorization state should remain inside the existing OAuth2 authorization service implementation.

The current project already has:

- `RegisteredClient` loading
- `OAuth2AuthorizationService`
- token generation
- custom grant provider chain

That is the correct extension point.

## Existing Mechanism In This Project

### 1. Client Configuration Source

`sys_oauth_client_details` is the source of client protocol configuration.

It already carries:

- `client_id`
- `client_secret`
- `scope`
- `resource_ids`
- `authorized_grant_types`
- token TTL settings

The key point is:

- `authorized_grant_types` already decides which login/token flows a client may use

No second client-login-method table should be introduced beside it.

### 2. RegisteredClient Construction

`LumenRemoteRegisteredClientRepository` reads `sys_oauth_client_details` and converts every `authorized_grant_types` item into `RegisteredClient.authorizationGrantTypes(...)`.

So the OAuth2 framework already knows which grant types are allowed for each client.

### 3. Token Endpoint Dispatch

`AuthorizationServerConfiguration` registers:

- standard token converters
- custom token converters
- custom authentication providers

This means token requests are already dispatched by `grant_type`.

### 4. Per-Grant Client Validation

Each custom grant provider performs client validation against `RegisteredClient.getAuthorizationGrantTypes()`.

That means:

- the client permission check already exists
- it already happens inside the OAuth2 flow
- there is no need to build another login-method authorization layer

## Current Design Decision

The current model should be:

- client-centric
- OAuth2 grant-centric
- resource authorization still based on client scopes/resources plus account permissions

And it should explicitly avoid:

- `subject_type` tables
- generic `auth_login_method` tables
- generic `auth_client_login_method` relation tables

Those abstractions do not add runtime value in the current project stage.

## How To Extend Login Capability

When a new login capability is added, use the existing OAuth2 extension path.

### Step 1. Define A New Grant

Examples:

- `otp`
- `passkey`

If compatibility is needed, the old grant can be retained temporarily as an alias.

### Step 2. Implement The Grant Chain

Add the matching OAuth2 components:

- `AuthenticationConverter`
- `AuthenticationToken`
- `AuthenticationProvider`
- registration in the authorization-server configuration

If the current handler SPI remains useful, it can stay as an internal code organization mechanism. It is not a second business authorization model.

### Step 3. Authorize The Grant On The Client

Add the grant string to `sys_oauth_client_details.authorized_grant_types` for the clients that should support it.

This is the only per-client authorization source for login flow enablement.

### Step 4. Add Optional Admin Dictionary Data

If the admin side needs selectable options or display labels, add the new grant into the existing `grant_types` dictionary data.

This is display/configuration support only. It is not an auth-domain truth source.

## Basic Data For This Phase

Only the following data should be extended for login-flow expansion:

- `sys_dict` / `sys_dict_item`
  - maintain `grant_types` display items when needed by admin configuration
- `sys_oauth_client_details`
  - maintain the actual client grant authorization in `authorized_grant_types`

No extra auth table is required for login-way enablement at this phase.

## Multi-Audience Handling

For the current business boundary:

- member-side clients serve member accounts
- community-side clients serve community staff accounts
- platform-side clients serve platform staff accounts

This mapping should be expressed by client configuration and routing, not by introducing another generic subject-type system.

If a later phase truly needs:

- one client serving multiple audience contexts
- one person switching contexts under the same client

then a separate subject-context model can be discussed later.

## Session Direction

The session model should continue to build on OAuth2 authorization storage.

Operationally, the project should still think in terms of:

- one successful token issuance creates one authorization/session context
- multi-device means multiple token chains
- revoke should be possible per device or per client later

This can be evolved on top of the existing authorization store instead of introducing an unrelated login-method table.

## Decision Summary

The current project should use:

- `sys_oauth_client_details.authorized_grant_types` as the only client login-flow authorization source
- custom OAuth2 grants as the extension mechanism for new login capabilities
- existing OAuth2 authorization storage as the runtime session basis

The current project should not use:

- extra login-method authorization tables
- parallel client-login capability models
- subject-type abstractions without immediate runtime need

## Current Implementation Status

The current codebase now has these login flows:

- `password`
- `otp`
- `passkey`

### Passkey Registration

Passkey registration is bound to the current authenticated account and uses the existing `auth_account_credential` table.

Current endpoints:

- `POST /passkey/current/register/options`
- `POST /passkey/current/register`
- `GET /passkey/current/list`
- `DELETE /passkey/current/{credentialKey}`

The registration flow uses:

- a short-lived Redis challenge
- `auth_account_credential.credential_type = PASSKEY`
- `credential_key` as the WebAuthn credential id
- `secret_value` as serialized public key metadata and signature counter

### Passkey Login

Passkey login is implemented as a custom OAuth2 grant:

- `grant_type=passkey`

Public login bootstrap endpoint:

- `POST /passkey/assertion/options`

Token issuance still happens through:

- `POST /oauth2/token`

Required token request parameters:

- `grant_type=passkey`
- `username`
- `credentialId`
- `clientDataJSON`
- `authenticatorData`
- `signature`

### Passkey Session Behavior

Passkey login enters the same session pipeline as other login flows:

- `auth_session` is written on successful login
- `sid` is returned in the token response
- session revoke blocks resource access
- session revoke also blocks `refresh_token` renewal

## Remaining Extension Points

The current implementation is already sufficient for a full-stack demo, but it is not yet a production-grade identity platform.

### 1. Credential Lifecycle Management

The project already has the minimum credential model, but platform operations still need richer lifecycle control:

- reset password for a specific client account
- disable only OTP for an account
- force passkey registration
- bulk revoke credentials after risk events

### 2. Session Risk Controls

The current explicit session model supports:

- multi-device session listing
- current-device logout
- other-device logout
- token and session revoke

Still missing are:

- trusted-device remember rules
- refresh-token reuse detection policy
- suspicious IP or device anomaly handling
- global operator session dashboard

### 3. Audit And Compliance

The current project exposes usable runtime state, but audit depth is still thin.

Later phases may require:

- login-factor audit trail
- passkey registration audit
- session revoke audit
- operator action correlation with `sid`

### Current Limits

The current implementation intentionally stays minimal:

- no separate passkey table was added
- no attestation trust-chain verification is performed
- challenge state is Redis-only and short-lived
- `app` and `lumen` clients are the current passkey-enabled seeds
- no recovery code or trusted-device model exists yet
