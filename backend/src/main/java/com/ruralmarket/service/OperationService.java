package com.ruralmarket.service;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.domain.*;
import com.ruralmarket.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 现场运营服务：收费确认、费用减免、保证金扣罚/退还、临时调位、
 * 入场核验、巡查、投诉处置。所有改变抽签结果/费用/信用的动作经调整台账留痕。
 */
@Service
public class OperationService {

    private final AllocationRepository allocationRepository;
    private final StallRepository stallRepository;
    private final FairDayRepository fairDayRepository;
    private final CheckInRepository checkInRepository;
    private final InspectionRepository inspectionRepository;
    private final FoodSampleRepository foodSampleRepository;
    private final ComplaintRepository complaintRepository;
    private final LedgerService ledgerService;

    public OperationService(AllocationRepository allocationRepository, StallRepository stallRepository,
                            FairDayRepository fairDayRepository,
                            CheckInRepository checkInRepository, InspectionRepository inspectionRepository,
                            FoodSampleRepository foodSampleRepository, ComplaintRepository complaintRepository,
                            LedgerService ledgerService) {
        this.allocationRepository = allocationRepository;
        this.stallRepository = stallRepository;
        this.fairDayRepository = fairDayRepository;
        this.checkInRepository = checkInRepository;
        this.inspectionRepository = inspectionRepository;
        this.foodSampleRepository = foodSampleRepository;
        this.complaintRepository = complaintRepository;
        this.ledgerService = ledgerService;
    }

    // ---------------- 收费 ----------------

    public BigDecimal outstanding(Allocation a) {
        return a.getTotalAmount().subtract(a.getWaivedAmount())
                .subtract(a.getPaidAmount()).max(BigDecimal.ZERO);
    }

    private void refreshFeeStatus(Allocation a) {
        if (outstanding(a).compareTo(BigDecimal.ZERO) <= 0) {
            if (a.getWaivedAmount().compareTo(BigDecimal.ZERO) > 0
                    && a.getPaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                a.setFeeStatus(FeeStatus.WAIVED);
            } else {
                a.setFeeStatus(FeeStatus.PAID);
            }
        } else if (a.getWaivedAmount().compareTo(BigDecimal.ZERO) > 0) {
            a.setFeeStatus(FeeStatus.UNPAID);
        } else {
            a.setFeeStatus(FeeStatus.UNPAID);
        }
    }

    /** 摊主/工作人员确认缴费到账 */
    @Transactional
    public Allocation payFee(Long allocationId, BigDecimal amount, SysUser operator) {
        Allocation a = requireAllocation(allocationId);
        if (a.getStatus() == AllocationStatus.WAITLIST) {
            throw new ApiException("候补记录无需缴费");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("缴费金额必须大于 0");
        }
        BigDecimal beforePaid = a.getPaidAmount();
        if (amount.compareTo(outstanding(a)) > 0) {
            throw new ApiException("缴费金额超过应收 " + outstanding(a) + " 元");
        }
        a.setPaidAmount(beforePaid.add(amount));
        refreshFeeStatus(a);
        Allocation saved = allocationRepository.save(a);
        // 到账本身不属“调整”，在审计日志留痕（控制器调用 AuditService）
        return saved;
    }

    /** 费用减免（困难摊主、雨天政策等），必须填理由与来源 */
    @Transactional
    public Allocation waiveFee(Long allocationId, BigDecimal amount, String reason,
                               String sourceType, Long sourceId, SysUser operator) {
        Allocation a = requireAllocation(allocationId);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("减免金额必须大于 0");
        }
        BigDecimal maxWaivable = a.getTotalAmount().subtract(a.getWaivedAmount())
                .subtract(a.getPaidAmount());
        if (amount.compareTo(maxWaivable) > 0) {
            throw new ApiException("减免金额超过剩余应收 " + maxWaivable + " 元");
        }
        String before = "已减免" + a.getWaivedAmount() + "元";
        a.setWaivedAmount(a.getWaivedAmount().add(amount));
        refreshFeeStatus(a);
        allocationRepository.save(a);
        ledgerService.record(AdjustType.FEE_WAIVER, a.getFairDay(), a.getVendor(), a,
                before, "已减免" + a.getWaivedAmount() + "元", amount, null,
                sourceType == null ? "现场处置" : sourceType, sourceId, reason, operator);
        return a;
    }

    /** 保证金扣罚（投诉查实、巡查违规等），可联动信用扣分 */
    @Transactional
    public Allocation deductDeposit(Long allocationId, BigDecimal amount, Integer creditDelta,
                                    String reason, String sourceType, Long sourceId, SysUser operator) {
        Allocation a = requireAllocation(allocationId);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("扣罚金额必须大于 0");
        }
        BigDecimal refundable = a.getDeposit().subtract(a.getDeductedAmount()).subtract(a.getRefundedAmount());
        if (amount.compareTo(refundable) > 0) {
            throw new ApiException("可扣罚保证金余额不足（剩余可扣 " + refundable + " 元）");
        }
        String before = "已扣" + a.getDeductedAmount() + "元/信用" + a.getVendor().getCreditScore();
        a.setDeductedAmount(a.getDeductedAmount().add(amount));
        allocationRepository.save(a);
        // flush 后再记台账，台账中同步调整信用分
        ledgerService.record(AdjustType.DEPOSIT_DEDUCT, a.getFairDay(), a.getVendor(), a,
                before,
                "已扣" + a.getDeductedAmount() + "元", amount, creditDelta,
                sourceType == null ? "现场处置" : sourceType, sourceId, reason, operator);
        return a;
    }

    /** 散场退还剩余保证金 */
    @Transactional
    public Allocation refundDeposit(Long allocationId, BigDecimal amount, String reason, SysUser operator) {
        Allocation a = requireAllocation(allocationId);
        BigDecimal refundable = a.getDeposit().subtract(a.getDeductedAmount()).subtract(a.getRefundedAmount());
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            amount = refundable; // 默认全额退剩余
        }
        if (amount.compareTo(refundable) > 0) {
            throw new ApiException("可退保证金不足（剩余 " + refundable + " 元）");
        }
        a.setRefundedAmount(a.getRefundedAmount().add(amount));
        allocationRepository.save(a);
        ledgerService.record(AdjustType.DEPOSIT_REFUND, a.getFairDay(), a.getVendor(), a,
                "已退" + (a.getRefundedAmount().subtract(amount)) + "元",
                "已退" + a.getRefundedAmount() + "元", amount, null,
                "散场结算", null, reason == null ? "集市结束正常退还剩余保证金" : reason, operator);
        return a;
    }

    /**
     * 临时调位：雨天避雨、消防/积水、邻里边界纠纷等。
     * 目标摊位必须空闲且尺寸/用电/区域规则匹配，调位全程留痕。
     */
    @Transactional
    public Allocation moveStall(Long allocationId, Long targetStallId, String reason,
                                String sourceType, Long sourceId, SysUser operator) {
        Allocation a = requireAllocation(allocationId);
        if (a.getStatus() == AllocationStatus.WAITLIST) {
            throw new ApiException("候补记录不能调位");
        }
        Stall target = stallRepository.findById(targetStallId)
                .orElseThrow(() -> new ApiException("目标摊位不存在"));
        if (!target.getEnabled()) {
            throw new ApiException("目标摊位已停用");
        }
        if (!target.getFairDay().getId().equals(a.getFairDay().getId())) {
            throw new ApiException("不能跨集市调位");
        }
        if (target.getId().equals(a.getStall() == null ? null : a.getStall().getId())) {
            throw new ApiException("目标摊位与当前摊位相同");
        }
        List<Allocation> occupiers = allocationRepository.findByStallId(target.getId()).stream()
                .filter(x -> x.getStatus() != AllocationStatus.CANCELLED
                        && x.getStatus() != AllocationStatus.NO_SHOW
                        && x.getStatus() != AllocationStatus.WAITLIST)
                .toList();
        if (!occupiers.isEmpty()) {
            throw new ApiException("目标摊位 " + target.getStallNo() + " 已被占用（摊主："
                    + occupiers.get(0).getVendor().getName() + "）");
        }
        Application app = a.getApplication();
        if (app != null) {
            ZoneType required = Boolean.TRUE.equals(app.getLivePoultry()) ? ZoneType.POULTRY
                    : Boolean.TRUE.equals(app.getCookedFood()) ? ZoneType.COOKED : ZoneType.NORMAL;
            if (target.getZone().getType() != required) {
                throw new ApiException("目标区域不符合品类隔离要求，需为" + required.getLabel());
            }
            if (Boolean.TRUE.equals(app.getNeedElectricity()) && !Boolean.TRUE.equals(target.getHasElectricity())) {
                throw new ApiException("该摊位无电源，不满足申报用电需求");
            }
            if (Boolean.TRUE.equals(target.getNearFireLane())
                    && (Boolean.TRUE.equals(app.getCookedFood()) || Boolean.TRUE.equals(app.getLivePoultry()))) {
                throw new ApiException("该摊位紧邻消防通道，不能安排熟食/活禽");
            }
        }
        Stall before = a.getStall();
        a.setStall(target);
        if (a.getStatus() == AllocationStatus.ALLOCATED || a.getStatus() == AllocationStatus.CHECKED_IN) {
            a.setStatus(AllocationStatus.ADJUSTED);
        }
        // 按新摊位区域重算费用，多退少补通过减免/补收另行处理，这里仅更新标准并提示
        MarketZone z = target.getZone();
        BigDecimal newStallFee = z.getStallFee();
        BigDecimal newElec = a.getElectricityFee().compareTo(BigDecimal.ZERO) > 0 ? z.getElectricityFee() : BigDecimal.ZERO;
        BigDecimal feeDelta = newStallFee.add(newElec)
                .subtract(a.getStallFee().add(a.getElectricityFee()));
        a.setStallFee(newStallFee);
        a.setElectricityFee(newElec);
        BigDecimal newTotal = newStallFee.add(newElec).add(a.getDeposit());
        a.setTotalAmount(newTotal);
        allocationRepository.save(a);
        ledgerService.record(AdjustType.STALL_MOVE, a.getFairDay(), a.getVendor(), a,
                before == null ? "无" : before.getStallNo(),
                target.getStallNo() + "(费差" + feeDelta + "元)", null, null,
                sourceType == null ? "现场处置" : sourceType, sourceId, reason, operator);
        return a;
    }

    // ---------------- 入场核验 ----------------

    @Transactional
    public CheckIn checkInByCode(String code, String setupPhotos, String remark, SysUser staff) {
        Allocation a = allocationRepository.findByCheckCode(code)
                .orElseThrow(() -> new ApiException("核销码无效，查不到抽签记录"));
        if (!a.getFairDay().getStatus().equals(DayStatus.ONGOING)
                && !a.getFairDay().getStatus().equals(DayStatus.DRAWN)) {
            throw new ApiException("当前集市不在可入场状态");
        }
        if (a.getStatus() == AllocationStatus.WAITLIST) {
            throw new ApiException("该摊主为候补，暂不能入场");
        }
        if (a.getStatus() == AllocationStatus.CANCELLED || a.getStatus() == AllocationStatus.NO_SHOW) {
            throw new ApiException("该抽签记录已取消/记为未到场");
        }
        if (a.getFeeStatus() == FeeStatus.UNPAID) {
            throw new ApiException("费用未缴清（待收 " + outstanding(a) + " 元），请先到收费处缴费");
        }
        CheckIn checkIn = new CheckIn();
        checkIn.setAllocation(a);
        checkIn.setStaff(staff);
        checkIn.setSetupPhotos(setupPhotos);
        checkIn.setRemark(remark);
        checkInRepository.save(checkIn);

        a.setStatus(AllocationStatus.CHECKED_IN);
        allocationRepository.save(a);
        return checkIn;
    }

    public Allocation lookupByCode(String code) {
        return allocationRepository.findByCheckCode(code)
                .orElseThrow(() -> new ApiException("核销码无效"));
    }

    // ---------------- 巡查 ----------------

    @Transactional
    public Inspection createInspection(Inspection inspection, Long fairDayId, Long vendorId,
                                       Long allocationId, SysUser staff) {
        Inspection ins = new Inspection();
        FairDay day = new FairDay();
        day.setId(fairDayId);
        ins.setFairDay(day);
        SysUser vendor = new SysUser();
        vendor.setId(vendorId);
        ins.setVendor(vendor);
        if (allocationId != null) {
            Allocation a = allocationRepository.findById(allocationId).orElseThrow(() -> new ApiException("抽签记录不存在"));
            ins.setAllocation(a);
        }
        ins.setStaff(staff);
        ins.setHygieneScore(clamp(inspection.getHygieneScore()));
        ins.setPriceTagScore(clamp(inspection.getPriceTagScore()));
        ins.setMeasureScore(clamp(inspection.getMeasureScore()));
        ins.setAisleScore(clamp(inspection.getAisleScore()));
        ins.setElectricScore(clamp(inspection.getElectricScore()));
        ins.setIssues(inspection.getIssues());
        ins.setPhotos(inspection.getPhotos());
        ins.setRemark(inspection.getRemark());
        ins.setRequireRectify(Boolean.TRUE.equals(inspection.getRequireRectify()));
        return inspectionRepository.save(ins);
    }

    private Integer clamp(Integer v) {
        if (v == null) return 5;
        return Math.max(0, Math.min(5, v));
    }

    // ---------------- 投诉处置 ----------------

    @Transactional
    public Complaint handleComplaint(Long complaintId, ComplaintStatus status, String handleResult,
                                     boolean deduct, BigDecimal deductAmount, Integer creditDelta,
                                     Long allocationId, String reason, SysUser handler) {
        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException("投诉不存在"));
        Allocation allocation = null;
        if (allocationId != null) {
            allocation = allocationRepository.findById(allocationId)
                    .orElseThrow(() -> new ApiException("抽签记录不存在"));
        } else if (c.getAllocation() != null) {
            allocation = c.getAllocation();
        }
        c.setStatus(status);
        c.setHandleResult(handleResult);
        c.setHandler(handler);
        c.setHandledAt(LocalDateTime.now());
        if (allocation != null && c.getAllocation() == null) {
            c.setAllocation(allocation);
        }
        complaintRepository.save(c);

        if (deduct) {
            if (allocation == null) {
                throw new ApiException("扣罚保证金需要关联当场抽签记录");
            }
            String fullReason = "投诉#" + complaintId + "（" + c.getType() + "）查实："
                    + (reason == null ? handleResult : reason);
            deductDeposit(allocation.getId(),
                    deductAmount == null ? new BigDecimal("50") : deductAmount,
                    creditDelta == null ? -10 : creditDelta,
                    fullReason, "投诉", complaintId, handler);
        }
        return c;
    }

    // ---------------- 状态流转 ----------------

    @Transactional
    public FairDay updateDayStatus(Long dayId, DayStatus status) {
        FairDay day = fairDayRepository.findById(dayId)
                .orElseThrow(() -> new ApiException("赶集日不存在"));
        day.setStatus(status);
        return fairDayRepository.save(day);
    }

    @Transactional
    public void markNoShow(Long allocationId, SysUser operator) {
        Allocation a = requireAllocation(allocationId);
        if (a.getStatus() == AllocationStatus.CHECKED_IN) {
            throw new ApiException("已入场的摊主不能标记为未到场");
        }
        a.setStatus(AllocationStatus.NO_SHOW);
        allocationRepository.save(a);
    }

    private Allocation requireAllocation(Long id) {
        return allocationRepository.findById(id).orElseThrow(() -> new ApiException("抽签记录不存在"));
    }
}
