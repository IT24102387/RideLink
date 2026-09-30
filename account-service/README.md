# Account Service authentication

Requires Java 21. Set `MONGODB_URI` and `JWT_SECRET` before starting with
`./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`).
`JWT_SECRET` is a raw UTF-8 secret of at least 32 bytes; use a cryptographically
random value and keep it outside source control. There is no production default.
`JWT_EXPIRATION_SECONDS` optionally changes the default lifetime of 3600 seconds.

`POST /api/auth/register` retains its existing request and response contract.

`POST /api/auth/login` accepts:

```json
{"email":"nimal@example.com","password":"RideLink123!"}
```

A successful response contains `token`, `tokenType` (`Bearer`), and `expiresAt`.
The HS256-signed JWT contains the user ID in `sub`, `role`, issuer `iss`, issued
time `iat`, and expiration `exp`. Email matching uses the same normalization as
registration. Incorrect credentials and inactive accounts return a generic 401;
invalid request bodies return 400.

For all private endpoints send `Authorization: Bearer <token>`. Requests are
stateless and do not use login cookies. Signature, issuer, expiration and required
claims are validated. Each authenticated request also reads the account from
MongoDB to reject deleted, suspended, disabled, or role-changed accounts immediately.

`PUT /api/users/me/role` requires `Authorization: Bearer <token>` and accepts
`{"role":"PASSENGER"}` or `{"role":"DRIVER"}` (case sensitive). It identifies
the user exclusively from the authenticated JWT subject. Other body fields are
ignored. The endpoint updates only the role; MongoDB auditing manages `updatedAt`.
A 200 response contains `id`, `firstName`, `lastName`, `email`, `phoneNumber`,
`role`, `status`, `createdAt`, and `updatedAt`, never password fields.

After changing roles, log in again with `POST /api/auth/login` to obtain a JWT
with the new role. This endpoint does not issue a replacement token. Tokens whose
role differs from the stored role return 401 on subsequent protected requests.
Submitting the current role is allowed and does not invalidate a matching token.
This is role matching, not permanent token revocation: a still-unexpired token
can match again if the account later switches back to its previous role.

Invalid or missing roles return 400. Missing/invalid JWTs, inactive accounts,
and accounts deleted before authentication return 401. If the user disappears
between authentication and the service lookup, the endpoint returns 404.

`PUT /api/users/me/status` requires a Bearer JWT and accepts
`{"status":"ACTIVE"}`, `{"status":"SUSPENDED"}`, or `{"status":"DISABLED"}`
(case sensitive). Only the authenticated user's status is changed; other body
fields are ignored. It returns the same nine safe profile fields listed above.
MongoDB auditing manages `updatedAt`. Missing or invalid status returns 400;
authentication and missing-user errors follow the same rules as the role endpoint.

After an ACTIVE account changes to SUSPENDED or DISABLED, the successful request
returns 200, but subsequent protected requests using existing JWTs and new login
attempts return 401. Inactive users cannot use this endpoint to reactivate
themselves. Submitting ACTIVE for an already ACTIVE account preserves access.

`PUT /api/admin/users/{userId}/role` requires an authenticated ACTIVE ADMIN and
accepts `{"role":"PASSENGER"}` or `{"role":"DRIVER"}`. Authorization uses the
current MongoDB role after validating that it matches the JWT role. Authenticated
PASSENGER and DRIVER accounts receive 403; missing, invalid, or stale JWTs receive
401. Invalid/missing roles return 400, missing target accounts return 404, and
success returns 200 with the nine safe target-user profile fields listed above.
The target user must log in again after a role change to obtain a matching JWT.

ADMIN is supported for login and authentication, but public registration and
both role-update endpoints cannot assign ADMIN. No administrative provisioning
endpoint or bootstrap account is provided; an ADMIN must already be provisioned
through a trusted process outside these APIs. Existing self-service endpoints
remain available with their existing restrictions.

`PUT /api/admin/users/{userId}/status` requires an authenticated ACTIVE ADMIN
and accepts `{"status":"ACTIVE"}`, `{"status":"SUSPENDED"}`, or
`{"status":"DISABLED"}`. It changes only the target's status, with `updatedAt`
managed by MongoDB auditing, and returns the nine safe profile fields above.
Other body fields are ignored. Missing/invalid status returns 400, missing/invalid
JWT returns 401, authenticated non-admin users receive 403, and a missing target
returns 404.

Suspended and disabled targets cannot log in or use existing JWTs. An admin can
restore them to ACTIVE, after which they can log in again. Existing authentication
checks remain unchanged: an unexpired old JWT can also work again after
reactivation if its role still matches; this endpoint does not permanently revoke
tokens.

Run tests with `.\mvnw.cmd test` on Windows or `./mvnw test` elsewhere. Tests use
a test-only signing secret and mock repository operations; they do not require a
running MongoDB instance or production credentials.
