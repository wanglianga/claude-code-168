package com.ruralmarket.platform.service;

import com.ruralmarket.platform.config.BizException;
import com.ruralmarket.platform.entity.*;
import com.ruralmarket.platform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 调整服务：临时调位、费用减免、保证金扣罚、信用调整、品类变更。
 * 所有调整必须填写原因与来源，生成 Adjustment 记录并写审计日志，向摊主公开。
 */
@Service
public class AdjustmentService {
    private final AdjustmentRepository adjustmentRepository;
    private final LotteryResultRepository resultRepository;
    private final StallRepository stallRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CreditService creditService;
    private final AuditService auditService;

    public AdjustmentService(AdjustmentRepository adjustmentRepository,
                             LotteryResultRepository resultRepository, StallRepository stallRepository,
                             PaymentRepository paymentRepository, UserRepository userRepository,
                             ApplicationRepository applicationRepository, CreditService creditService,
                             AuditService auditService) {
        this.adjustmentRepository = adjustmentRepository;
        this.resultRepository = resultRepository;
        this.stallRepository = stallRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.creditService = creditService;
        this.auditService = auditService;
    }

    /** 临时调位 / 雨天调位 */
    @Transactional
    public Adjustment relocate(Long resultId, Long newStallId, String type,
                               String reason, String source, Long sourceRefId, User operator) {
        LotteryResult r = resultRepository.findById(resultId)
                .orElseThrow(() -> new BizException("分配单不存在"));
        Stall target = stallRepository.findById(newStallId)
                .orElseThrow(() -> new BizException("目标摊位不存在"));
        if (!target.getActive()) throw new BizException("目标摊位未启用，不能调入");
        // 同一场集市下目标摊位不能已被占用
        List<LotteryResult> eventResults = resultRepository.findByApplicationEventId(r.getApplication().getEvent().getId());
        boolean occupied = eventResults.stream()
                .anyMatch(x -> !x.getId().equals(r.getId()) && x.getStall().getId().equals(target.getId())
                        && !"CANCELLED".equals(x.getStatus()));
        if (occupied) throw new BizException("目标摊位 " + target.getStallNo() + " 已被占用");

        String before = r.getStallNo();
        r.setStall(target);
        r.setStallNo(target.getStallNo());
        r.setEntryTime(calcEntryTime(target.getZone().getEntryOffsetMinutes()));
        r.setStatus("RELOCATED");
        resultRepository.save(r);

        Adjustment adj = base(r.getApplication().getEvent(), r, r.getApplication().getVendor(),
                type, reason, source, sourceRefId, operator);
        adj.setBeforeValue("摊位 " + before);
        adj.setAfterValue("摊位 " + target.getStallNo() + "（入场时间 " + r.getEntryTime() + "）");
        Adjustment saved = adjustmentRepository.save(adj);
        auditService.log(operator.getId(), operator.getName(), "ADJUST_RELOCATE",
                "摊主[" + r.getApplication().getVendor().getName() + "] " + before + " → " + target.getStallNo()
                        + "，原因：" + reason);
        return saved;
    }

    /** 费用减免 */
    @Transactional
    public Adjustment waiveFee(Long paymentId, String reason, String source, Long sourceRefId, User operator) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BizException("账单不存在"));
        if (!"UNPAID".equals(p.getStatus())) throw new BizException("仅未缴账单可减免（当前：" + p.getStatus() + "）");
        p.setStatus("WAIVED");
        p.setRemark("减免原因：" + reason);
        paymentRepository.save(p);

        Adjustment adj = base(p.getEvent(), p.getResult(), p.getVendor(),
                "FEE_WAIVER", reason, source, sourceRefId, operator);
        adj.setBeforeValue(p.getItemName() + " ¥" + p.getAmount() + " 未缴");
        adj.setAfterValue("已减免 ¥" + p.getAmount());
        Adjustment saved = adjustmentRepository.save(adj);
        auditService.log(operator.getId(), operator.getName(), "ADJUST_FEE_WAIVER",
                "摊主[" + p.getVendor().getName() + "] 减免 " + p.getItemName() + " ¥" + p.getAmount()
                        + "，原因：" + reason);
        return saved;
    }

    /** 保证金扣罚 */
    @Transactional
    public Adjustment penalizeDeposit(Long paymentId, String reason, String source, Long sourceRefId, User operator) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BizException("账单不存在"));
        if (!"DEPOSIT".equals(p.getItemType())) throw new BizException("仅保证金账单可扣罚");
        if ("DEDUCTED".equals(p.getStatus())) throw new BizException("该保证金已扣罚");
        String beforeStatus = p.getStatus();
        p.setStatus("DEDUCTED");
        p.setRemark("扣罚原因：" + reason);
        paymentRepository.save(p);

        Adjustment adj = base(p.getEvent(), p.getResult(), p.getVendor(),
                "DEPOSIT_PENALTY", reason, source, sourceRefId, operator);
        adj.setBeforeValue("保证金 ¥" + p.getAmount() + " " + beforeStatus);
        adj.setAfterValue("已扣罚 ¥" + p.getAmount());
        Adjustment saved = adjustmentRepository.save(adj);
        auditService.log(operator.getId(), operator.getName(), "ADJUST_DEPOSIT_PENALTY",
                "摊主[" + p.getVendor().getName() + "] 保证金扣罚 ¥" + p.getAmount() + "，原因：" + reason);
        return saved;
    }

    /** 信用调整 */
    @Transactional
    public Adjustment adjustCredit(Long vendorId, int delta, String reason, String source,
                                   Long sourceRefId, User operator) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new BizException("摊主不存在"));
        int before = vendor.getCreditScore();
        CreditLog log = creditService.adjust(vendor, delta, reason, "ADJUSTMENT", null, operator);

        Adjustment adj = base(null, null, vendor, "CREDIT_ADJUST", reason, source, sourceRefId, operator);
        adj.setBeforeValue("信用分 " + before);
        adj.setAfterValue("信用分 " + log.getScoreAfter() + "（变动 " + (delta >= 0 ? "+" : "") + delta + "）");
        Adjustment saved = adjustmentRepository.save(adj);
        auditService.log(operator.getId(), operator.getName(), "ADJUST_CREDIT",
                "摊主[" + vendor.getName() + "] 信用 " + before + " → " + log.getScoreAfter() + "，原因：" + reason);
        return saved;
    }

    /** 临时换品类 */
    @Transactional
    public Adjustment changeCategory(Long resultId, String newCategory, boolean sellsCookedFood,
                                     boolean sellsLivePoultry, String reason, String source,
                                     Long sourceRefId, User operator) {
        if (!Constants.CATEGORIES.containsKey(newCategory)) throw new BizException("品类不合法");
        LotteryResult r = resultRepository.findById(resultId)
                .orElseThrow(() -> new BizException("分配单不存在"));
        Application app = r.getApplication();
        String before = Constants.categoryName(app.getCategory());
        app.setCategory(newCategory);
        app.setSellsCookedFood(sellsCookedFood);
        app.setSellsLivePoultry(sellsLivePoultry);
        applicationRepository.save(app);

        Adjustment adj = base(app.getEvent(), r, app.getVendor(),
                "CATEGORY_CHANGE", reason, source, sourceRefId, operator);
        adj.setBeforeValue(before);
        adj.setAfterValue(Constants.categoryName(newCategory));
        Adjustment saved = adjustmentRepository.save(adj);
        auditService.log(operator.getId(), operator.getName(), "ADJUST_CATEGORY",
                "摊主[" + app.getVendor().getName() + "] 品类 " + before + " → "
                        + Constants.categoryName(newCategory) + "，原因：" + reason);
        return saved;
    }

    private Adjustment base(MarketEvent event, LotteryResult result, User vendor, String type,
                            String reason, String source, Long sourceRefId, User operator) {
        Adjustment adj = new Adjustment();
        adj.setEvent(event);
        adj.setResult(result);
        adj.setVendor(vendor);
        adj.setType(type);
        adj.setReason(reason);
        adj.setSource(source);
        adj.setSourceRefId(sourceRefId);
        adj.setOperator(operator);
        return adj;
    }

    private String calcEntryTime(int offsetMinutes) {
        int total = 6 * 60 + offsetMinutes;
        return String.format("%02d:%02d", total / 60, total % 60);
    }
}
