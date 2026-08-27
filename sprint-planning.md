Sprint goal: Find and fix all of the vulnerabilities

Story 1: BatchPayoutJob.java
Developer: Giulia
Estimate: 2

Story 2: MerchantController.java
Developer: Louise
Estimate: 2

Story 3: PayoutAprrovalService.java
Developer: Ian
Estimate: 2

Story 4: WebhookController.java
Developer: Myroslava
Estimate: 2
A08: Software and Data Integrity Failure
The original endpoint accepted a JSON body, read a payoutId and status out of it, and immediately marked that payout as settled, without checking who sent the request. Someone outside the company, with no special access or hacking skills — just knowledge of the web address — could trick the system into thinking money had been paid when it hadn't.
Fix: HMAC signature verification - a standard, industry-accepted way webhook endpoints protect themselves. The fix adds a "secret handshake" between the payment company and this system. Both sides agree on a secret password ahead of time, every message from the payment company now comes with a stamped seal, before acting on any message, the system now recomputes what that seal should look like, using the same secret password, and checks it matches the seal that came with the message. If the seal doesn't match, the message is rejected outright — nothing gets updated, no payout gets marked as paid.
What's still needed: The actual payment provider must be configured to sign its webhook calls the same way (HMAC-SHA256 over the raw body, secret shared with them, sent as a header) — the exact header name and encoding (hex vs base64) need to match whatever the real provider uses.