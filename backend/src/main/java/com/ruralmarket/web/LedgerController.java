package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.AdjustmentLedger;
import com.ruralmarket.repo.AdjustmentLedgerRepository;
import com.ruralmarket.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 调整台账查询：临时调位、费用减免、扣罚、信用调整的“公示墙”，
 * 任何摊主都能看到每条调整的来源与理由。
 */
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final AdjustmentLedgerRepository repository;

    public LedgerController(AdjustmentLedgerRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public R<List<AdjustmentLedger>> list(@RequestParam(required = false) Long fairDayId,
                                          @RequestParam(required = false) Long vendorId,
                                          @RequestParam(required = false) Long allocationId) {
        if (allocationId != null) {
            return R.ok(repository.findByAllocationIdOrderByCreatedAtDesc(allocationId));
        }
        if (fairDayId != null) {
            return R.ok(repository.findByFairDayIdOrderByCreatedAtDesc(fairDayId));
        }
        if (vendorId != null) {
            // 摊主只能查自己的
            if (CurrentUser.require().getRole() == com.ruralmarket.domain.Role.VENDOR
                    && !CurrentUser.get().getId().equals(vendorId)) {
                throw new ApiException(403, "只能查询本人调整记录");
            }
            return R.ok(repository.findByVendorIdOrderByCreatedAtDesc(vendorId));
        }
        return R.ok(repository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/mine")
    public R<List<AdjustmentLedger>> mine() {
        return R.ok(repository.findByVendorIdOrderByCreatedAtDesc(CurrentUser.require().getId()));
    }
}
