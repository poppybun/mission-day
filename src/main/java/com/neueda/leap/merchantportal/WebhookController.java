package com.neueda.leap.merchantportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class WebhookController {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PayoutStatusUpdater payoutStatusUpdater;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // supplied via PAYMENT_WEBHOOK_SECRET env var, never hardcoded
    @Value("${webhooks.payment-provider.secret}")
    private String webhookSecret;

    public WebhookController(PayoutStatusUpdater payoutStatusUpdater) {
        this.payoutStatusUpdater = payoutStatusUpdater;
    }

    // Requires the provider's HMAC-SHA256 signature over the raw body before
    // trusting the payload, so requests that didn't come from the provider are rejected.
    @PostMapping("/api/webhooks/payment-status")
    public ResponseEntity<Void> handlePaymentStatusWebhook(
            @RequestHeader("X-Signature") String signatureHeader,
            // bound as a String, not PaymentStatusEvent, so we sign/verify the exact bytes sent
            @RequestBody String rawBody) throws Exception {
        if (!isValidSignature(rawBody, signatureHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaymentStatusEvent event = objectMapper.readValue(rawBody, PaymentStatusEvent.class);
        payoutStatusUpdater.markSettled(event.getPayoutId(), event.getStatus());
        return ResponseEntity.ok().build();
    }

    private boolean isValidSignature(String rawBody, String signatureHeader) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        String expectedHex = toHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
        // constant-time comparison to avoid leaking the signature via timing
        return MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8));
    }

    // matches the hex encoding the payment provider uses for its X-Signature header
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
