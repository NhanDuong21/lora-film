package com.project.scoreservice.service;

import com.project.scoreservice.dto.*;
import com.project.scoreservice.entity.MembershipTier;
import com.project.scoreservice.entity.ScoreHistory;
import com.project.scoreservice.entity.UserScore;
import com.project.scoreservice.enumtype.ReconciliationDetailStatus;
import com.project.scoreservice.enumtype.ReconciliationRunStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.enumtype.UserScoreStatus;
import com.project.scoreservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminScoreOperationServiceTest {

    @Autowired
    private AdminScoreOperationService adminScoreOperationService;

    @Autowired
    private UserScoreRepository userScoreRepository;

    @Autowired
    private MembershipTierRepository membershipTierRepository;

    @Autowired
    private ScoreHistoryRepository scoreHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ReconciliationRunRepository reconciliationRunRepository;

    @Autowired
    private ReconciliationDetailRepository reconciliationDetailRepository;

    @Autowired
    private ScoreHoldRepository scoreHoldRepository;

    @Autowired
    private PointExpirationBucketRepository pointExpirationBucketRepository;

    @Autowired
    private MembershipTierHistoryRepository membershipTierHistoryRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        reconciliationDetailRepository.deleteAll();
        reconciliationRunRepository.deleteAll();
        pointExpirationBucketRepository.deleteAll();
        membershipTierHistoryRepository.deleteAll();
        scoreHoldRepository.deleteAll();
        scoreHistoryRepository.deleteAll();
        userScoreRepository.deleteAll();

        MembershipTier silver = membershipTierRepository.findByTierCode("SILVER")
                .orElseGet(() -> membershipTierRepository.save(new MembershipTier(null, "SILVER", 0, new BigDecimal("0.05"), "Silver tier", null, null)));

        UserScore userScore = new UserScore(99991L, 200, 200, silver, null, null);
        userScoreRepository.save(userScore);
    }

    @Test
    void testAdjustScoreAndReverse() {
        ScoreAdjustmentRequest req = new ScoreAdjustmentRequest(null, ScoreAdjustmentType.ADD, 150, false, true, "Test add", "REQ-OP-SRV-ADD-01");
        AdminAdjustmentResponse addRes = adminScoreOperationService.adjustScore(99991L, req, "888", "127.0.0.1");

        assertNotNull(addRes);
        assertEquals(350, addRes.getCurrentPoints());
        assertEquals(350, addRes.getAccumulatedPoints());
        assertFalse(addRes.getIdempotent());

        ReverseAdjustmentRequest revReq = new ReverseAdjustmentRequest(addRes.getHistoryId(), "Test reversal", "REQ-OP-SRV-REV-01");
        AdminAdjustmentResponse revRes = adminScoreOperationService.reverseAdjustment(99991L, revReq, "888", "127.0.0.1");

        assertNotNull(revRes);
        assertEquals(-150, revRes.getPointChange());
        assertEquals(200, revRes.getCurrentPoints());

        long auditCount = auditLogRepository.count();
        assertTrue(auditCount >= 2, "Should have created audit logs for adjustment and reversal");
    }

    @Test
    void testRunReconciliation() {
        ReconciliationDTOs.ReconciliationRunRequest req = new ReconciliationDTOs.ReconciliationRunRequest("BATCH-TEST-01", "Testing reconciliation");
        ReconciliationDTOs.ReconciliationRunResponse res = adminScoreOperationService.runReconciliation(req, "888");

        assertNotNull(res);
        assertEquals("BATCH-TEST-01", res.batchCode());
        assertEquals(ReconciliationRunStatus.COMPLETED, res.status());
        assertEquals(1, res.totalUsers());

        long detailCount = reconciliationDetailRepository.count();
        assertEquals(1, detailCount);
    }

    @Test
    void reconciliationUsesAccumulatedSnapshotsInsteadOfEveryPositiveTransaction() {
        UserScore account = userScoreRepository.findByUserId(99991L).orElseThrow();
        account.setCurrentPoints(250);
        account.setAccumulatedPoints(200);
        userScoreRepository.save(account);

        scoreHistoryRepository.save(ScoreHistory.builder()
                .userScore(account).idempotencyKey("RECON-EARN-1")
                .transactionType(ScoreTransactionType.EARN)
                .actualPointChange(200).balanceBefore(0).balanceAfter(200)
                .accumulatedBefore(0).accumulatedAfter(200).build());
        scoreHistoryRepository.save(ScoreHistory.builder()
                .userScore(account).idempotencyKey("RECON-REFUND-1")
                .transactionType(ScoreTransactionType.REFUND_REDEEM)
                .actualPointChange(50).balanceBefore(200).balanceAfter(250)
                .accumulatedBefore(200).accumulatedAfter(200).build());

        ReconciliationDTOs.ReconciliationRunResponse run = adminScoreOperationService.runReconciliation(
                new ReconciliationDTOs.ReconciliationRunRequest("BATCH-SNAPSHOT-01", "Snapshot semantics"), "888");

        assertEquals(1, run.matchedUsers());
        assertEquals(0, run.mismatchedUsers());
        assertEquals(1, reconciliationDetailRepository.countByRunIdAndStatus(run.id(), ReconciliationDetailStatus.MATCHED));
    }

    @Test
    void freezesScoreAccountWithoutChangingLoginAccount() {
        AdminUserScoreResponse response = adminScoreOperationService.updateAccountStatus(
                99991L,
                new ScoreAccountStatusRequest(UserScoreStatus.LOCKED, "Investigating refund complaint", "CASE-SCORE-001"),
                "888",
                "127.0.0.1");

        assertEquals(UserScoreStatus.LOCKED, response.getStatus());
        assertEquals(UserScoreStatus.LOCKED, userScoreRepository.findByUserId(99991L).orElseThrow().getStatus());
        assertTrue(auditLogRepository.count() > 0);
    }

    @Test
    void testExportScoreData() {
        byte[] historyCsv = adminScoreOperationService.exportScoreData("HISTORY", "CSV", null, null, null);
        assertNotNull(historyCsv);
        assertTrue(historyCsv.length > 0);
        String historyStr = new String(historyCsv, StandardCharsets.UTF_8);
        assertTrue(historyStr.contains("ID,User ID,Transaction Type"), "Should contain CSV header");
    }
}
