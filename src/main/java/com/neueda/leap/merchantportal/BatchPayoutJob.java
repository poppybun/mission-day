package com.neueda.leap.merchantportal;

import java.util.List;

public class BatchPayoutJob {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BatchPayoutJob.class);

    private BankTransferClient bankTransferClient;
    private PayoutRepository payoutRepository;

    public BatchPayoutJob(BankTransferClient bankTransferClient, PayoutRepository payoutRepository) {
        this.bankTransferClient = bankTransferClient;
        this.payoutRepository = payoutRepository;
    }

    // FIXED (A10): Failed transfers are now properly tracked with FAILED status
    // instead of being silently marked as PAID. This prevents double-payment risks
    // and allows for proper retry logic or manual intervention.
    public void runNightlyBatch(List<PayoutRequest> approvedPayouts) {
        for (PayoutRequest payout : approvedPayouts) {
            try {
                bankTransferClient.transfer(payout.getMerchantId(), payout.getAmount());
                payout.setApprovalStatus("PAID");
            } catch (BankTransferException e) {
                log.error("Transfer failed for payout {}: {}", payout.getId(), e.getMessage());
                payout.setApprovalStatus("FAILED");
            }
            payoutRepository.save(payout);
        }
    }
}
