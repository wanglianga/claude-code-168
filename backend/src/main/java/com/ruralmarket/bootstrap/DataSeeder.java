package com.ruralmarket.bootstrap;

import com.ruralmarket.domain.*;
import com.ruralmarket.repo.*;
import com.ruralmarket.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 演示数据：
 * - 2026-09-30 中秋大集（已结束）：完整的报名→抽签→收费→入场→巡查→留样→投诉扣罚链路，供复盘演示；
 * - 2026-10-08 重阳大集（报名中）：区域摊位已就绪，报名有通过/待审核，供现场抽签演示。
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SysUserRepository userRepository;
    private final FairDayRepository fairDayRepository;
    private final MarketZoneRepository zoneRepository;
    private final StallRepository stallRepository;
    private final ApplicationRepository applicationRepository;
    private final AllocationRepository allocationRepository;
    private final InspectionRepository inspectionRepository;
    private final FoodSampleRepository foodSampleRepository;
    private final ComplaintRepository complaintRepository;
    private final FairDayService fairDayService;
    private final DrawService drawService;
    private final OperationService operationService;
    private final String uploadDir;

    public DataSeeder(SysUserRepository userRepository, FairDayRepository fairDayRepository,
                      MarketZoneRepository zoneRepository, StallRepository stallRepository,
                      ApplicationRepository applicationRepository, AllocationRepository allocationRepository,
                      InspectionRepository inspectionRepository, FoodSampleRepository foodSampleRepository,
                      ComplaintRepository complaintRepository, FairDayService fairDayService,
                      DrawService drawService, OperationService operationService,
                      @Value("${app.upload.dir}") String uploadDir) {
        this.userRepository = userRepository;
        this.fairDayRepository = fairDayRepository;
        this.zoneRepository = zoneRepository;
        this.stallRepository = stallRepository;
        this.applicationRepository = applicationRepository;
        this.allocationRepository = allocationRepository;
        this.inspectionRepository = inspectionRepository;
        this.foodSampleRepository = foodSampleRepository;
        this.complaintRepository = complaintRepository;
        this.fairDayService = fairDayService;
        this.drawService = drawService;
        this.operationService = operationService;
        this.uploadDir = uploadDir;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("初始化演示数据...");
        String licenseImg = demoImage("license.png", new Color(180, 205, 230));
        String setupImg = demoImage("setup.png", new Color(210, 225, 190));
        String sampleImg = demoImage("sample.png", new Color(235, 215, 175));
        String inspectImg = demoImage("inspect.png", new Color(230, 200, 200));
        String evidenceImg = demoImage("evidence.png", new Color(215, 205, 230));

        // ---------- 用户 ----------
        SysUser admin = user("admin", "admin", "乡镇管理员-林镇", "13800000001", Role.ADMIN, 100, "镇政府市监办");
        SysUser staff = user("staff", "staff", "市管员-老周", "13800000002", Role.STAFF, 100, "市场管理处");
        userRepository.saveAll(List.of(admin, staff));

        SysUser wang = user("wanglu", "123456", "王禄（卤味摊）", "13900000011", Role.VENDOR, 105, "河西村");
        SysUser li = user("liju", "123456", "李菊（糕点摊）", "13900000012", Role.VENDOR, 100, "河东村");
        SysUser zhang = user("zhangde", "123456", "张德（凉菜摊）", "13900000013", Role.VENDOR, 92, "王庄村");
        SysUser ma = user("macai", "123456", "马彩（烤鸭摊）", "13900000014", Role.VENDOR, 98, "集市后街");
        SysUser chen = user("chenfang", "123456", "陈芳（蔬菜摊）", "13900000015", Role.VENDOR, 110, "张家湾");
        SysUser zhao = user("zhaoyong", "123456", "赵勇（水果摊）", "13900000016", Role.VENDOR, 85, "五里铺");
        SysUser sun = user("sunqiang", "123456", "孙强（活禽摊）", "13900000017", Role.VENDOR, 100, "孙家庄");
        SysUser zhou = user("zhouwei", "123456", "周伟（活禽摊）", "13900000018", Role.VENDOR, 96, "周场村");
        SysUser wu = user("wugui", "123456", "吴贵（服装摊）", "13900000019", Role.VENDOR, 100, "吴塘口");
        SysUser zheng = user("zhengli", "123456", "郑力（杂货摊）", "13900000020", Role.VENDOR, 100, "新搬来摊主");
        userRepository.saveAll(List.of(wang, li, zhang, ma, chen, zhao, sun, zhou, wu, zheng));

        // ================= 历史集市（中秋大集，已结束） =================
        FairDay oldDay = new FairDay();
        oldDay.setName("2026-09-30 中秋大集");
        oldDay.setMarketDate(LocalDate.of(2026, 9, 30));
        oldDay.setSignupDeadline(LocalDateTime.of(2026, 9, 29, 18, 0));
        oldDay.setDrawTime(LocalTime.of(4, 30));
        oldDay.setStatus(DayStatus.OPEN);
        oldDay.setRulesText("""
                本场抽签规则：1.按品类分区（熟食冷链区/活禽隔离区/普通区）；
                2.邻近消防通道摊位不安排熟食明火与活禽；3.用电摊主安排带电源摊位；
                4.车辆尺寸须与摊位匹配；5.老摊主（历史赶集次数）与信用分高者优先抽签；
                6.抽签使用当场公示随机种子，结果可复算；7.临时调位、减免、扣罚一律台账公示。""");
        fairDayRepository.save(oldDay);

        MarketZone oa = zone(oldDay, "A", "普通干货区", ZoneType.NORMAL, "30.00", "10.00", "100.00", "邻近东街");
        MarketZone ob = zone(oldDay, "B", "熟食冷链区", ZoneType.COOKED, "40.00", "15.00", "200.00", "配电冰柜，靠近水源");
        MarketZone oc = zone(oldDay, "C", "活禽隔离区", ZoneType.POULTRY, "25.00", "0.00", "150.00", "下风口，独立消毒");
        zoneRepository.saveAll(List.of(oa, ob, oc));
        fairDayService.generateStalls(oldDay.getId(), oa.getId(), "A", 12,
                new BigDecimal("4.50"), new BigDecimal("3.50"), 4, List.of(1));
        fairDayService.generateStalls(oldDay.getId(), ob.getId(), "B", 8,
                new BigDecimal("4.00"), new BigDecimal("3.00"), 6, List.of());
        fairDayService.generateStalls(oldDay.getId(), oc.getId(), "C", 4,
                new BigDecimal("5.00"), new BigDecimal("4.00"), 0, List.of());

        // 历史报名（均已审核，历史赶集次数不同 → 老摊主优先）
        app(oldDay, wang, "卤味", "卤鸡爪、卤豆干、卤蛋", true, false, 3, 2, true, 2000,
                licenseImg, "赶集12次，无重大投诉", 12);
        app(oldDay, li, "糕点", "发糕、月饼、桃酥", true, false, 3, 2, false, 0,
                licenseImg, "赶集8次", 8);
        app(oldDay, zhang, "凉菜", "凉拌菜、凉面", true, false, 2.5, 1.8, false, 0,
                licenseImg, "赶集3次，曾被提醒明码标价", 3);
        app(oldDay, ma, "烤鸭", "现切烤鸭、卤鸭", true, false, 3.5, 2, true, 3000,
                licenseImg, "赶集9次", 9);
        app(oldDay, chen, "蔬菜", "自产应季蔬菜", false, false, 4, 3, false, 0,
                null, "赶集20次，老摊主，历年文明经营户", 20);
        app(oldDay, zhao, "水果", "应季水果、果切", false, false, 4, 3, false, 0,
                null, "赶集5次，曾因缺斤少两被提醒", 5);
        app(oldDay, sun, "活禽", "土鸡、土鸭（现宰代加工）", false, true, 4.5, 3.5, false, 0,
                licenseImg, "赶集15次，检疫合格", 15);
        app(oldDay, zhou, "活禽", "鸽子、鹌鹑", false, true, 3, 2.5, false, 0,
                licenseImg, "赶集2次", 2);
        app(oldDay, wu, "服装", "秋冬服装、鞋袜", false, false, 4, 2.5, false, 0,
                null, "赶集6次", 6);
        app(oldDay, zheng, "杂货", "锅碗瓢盆日用杂货", false, false, 3.5, 2, false, 0,
                null, "第一次赶集", 0);
        applicationRepository.flush();
        // 全部审核通过
        applicationRepository.findByFairDayOrderById(oldDay).forEach(a -> {
            a.setStatus(ApplyStatus.APPROVED);
            a.setReviewedAt(LocalDateTime.of(2026, 9, 29, 19, 0));
            a.setReviewer(staff);
            a.setReviewNote("证照齐全，资格核实无误；历史赶集次数已核实");
        });
        applicationRepository.flush();

        drawService.draw(oldDay.getId(), "zhongqiu2026", staff);
        operationService.updateDayStatus(oldDay.getId(), DayStatus.ONGOING);

        var oldAllocs = allocationRepository.findByFairDayOrderByStall_StallNo(oldDay);
        Allocation wangA = byVendor(oldAllocs, wang);
        Allocation liA = byVendor(oldAllocs, li);
        Allocation zhangA = byVendor(oldAllocs, zhang);
        Allocation maA = byVendor(oldAllocs, ma);
        Allocation chenA = byVendor(oldAllocs, chen);
        Allocation zhaoA = byVendor(oldAllocs, zhao);
        Allocation sunA = byVendor(oldAllocs, sun);
        Allocation zhouA = byVendor(oldAllocs, zhou);
        Allocation wuA = byVendor(oldAllocs, wu);
        Allocation zhengA = byVendor(oldAllocs, zheng);

        // 服装摊吴贵未到场
        operationService.markNoShow(wuA.getId(), staff);

        // 雨天积水：蔬菜摊陈芳临时调位（同区空闲摊），全程留痕
        List<Stall> allocatedStalls = oldAllocs.stream().map(Allocation::getStall).toList();
        Stall freeA = stallRepository.findByFairDayAndEnabledTrueOrderByStallNo(oldDay).stream()
                .filter(s -> s.getZone().getType() == ZoneType.NORMAL)
                .filter(s -> allocatedStalls.stream().noneMatch(x -> x.getId().equals(s.getId())))
                .findFirst().orElseThrow();
        operationService.moveStall(chenA.getId(), freeA.getId(),
                "凌晨降雨原摊位积水约10厘米，无法摆放蔬菜，调至同区干燥空摊，收费标准不变",
                "天气调整", null, staff);

        // 困难摊主郑力费用减免（持村委会证明），其余全额缴清
        operationService.waiveFee(zhengA.getId(), new BigDecimal("30.00"),
                "郑力为新搬来低保家庭，持村委会困难证明，经现场公示后减免摊位费30元（保证金不免）",
                "困难减免", null, admin);
        for (Allocation a : List.of(wangA, liA, zhangA, maA, chenA, zhaoA, sunA, zhouA, zhengA)) {
            Allocation fresh = allocationRepository.findById(a.getId()).orElseThrow();
            operationService.payFee(fresh.getId(), operationService.outstanding(fresh), staff);
        }

        // 扫码入场核验（拍摄摊位布置）
        for (Allocation a : List.of(wangA, liA, zhangA, maA, chenA, zhaoA, sunA, zhouA, zhengA)) {
            Allocation fresh = allocationRepository.findById(a.getId()).orElseThrow();
            operationService.checkInByCode(fresh.getCheckCode(), setupImg,
                    "摊位布置合规，已拍摄存档", staff);
        }

        // ---------- 巡查 ----------
        inspection(oldDay, wangA, wang, staff, 2, 4, 4, 5, 3,
                "卫生较差,未佩戴口罩手套", inspectImg, "卤味摊操作台有积水，责令当场整改", true);
        inspection(oldDay, zhaoA, zhao, staff, 4, 4, 1, 4, 5,
                "缺斤少两", inspectImg, "电子秤未贴检定合格标识，复称少20克/斤", true);
        inspection(oldDay, wuA, wu, staff, 5, 5, 5, 2, 5,
                "占道经营", inspectImg, "服装货架越出黄线约40厘米（未到场，留单告知）", false);
        inspection(oldDay, sunA, sun, staff, 3, 5, 5, 5, 5,
                "活禽隔离不到位", inspectImg, "禽类排泄物未及时密封清理，已要求消毒", true);
        inspection(oldDay, chenA, chen, staff, 5, 5, 5, 5, 5,
                "", inspectImg, "文明经营户，各项规范", false);
        inspection(oldDay, zhengA, zheng, staff, 4, 4, 4, 4, 2,
                "私拉电线", inspectImg, "自备插线板未接地，已更换市场统一电源", true);

        // ---------- 食品留样（马彩未留样 → 食品安全风险） ----------
        foodSample(oldDay, wangA, wang, "卤鸡爪（拼盘留样）", "WL-0930-01", "市场留样柜 B-2",
                LocalDateTime.of(2026, 9, 30, 6, 20), sampleImg, 3.5);
        foodSample(oldDay, liA, li, "五仁月饼", "LJ-0930-01", "市场留样柜 B-3",
                LocalDateTime.of(2026, 9, 30, 6, 40), sampleImg, 4.0);
        foodSample(oldDay, zhangA, zhang, "凉拌三丝", "ZD-0930-01", "市场留样柜 B-1",
                LocalDateTime.of(2026, 9, 30, 7, 0), sampleImg, 4.5);

        // ---------- 投诉与处置 ----------
        Complaint c1 = new Complaint();
        c1.setFairDay(oldDay); c1.setVendor(wang); c1.setAllocation(wangA);
        c1.setType("腹泻疑似食物中毒"); c1.setComplainant("刘先生"); c1.setContact("137****1122");
        c1.setContent("9月30日中午食用王禄卤味摊购买的卤豆干后，下午出现腹泻呕吐，医院诊断急性肠胃炎，同批次2人不适。");
        c1.setEvidencePhotos(evidenceImg);
        complaintRepository.save(c1);
        operationService.handleComplaint(c1.getId(), ComplaintStatus.RESOLVED,
                "经查该批次卤豆干留样柜温度偏高（8℃），且摊主上午未戴手套操作。已陪同消费者就医，"
                        + "摊主承担医药费320元，扣保证金100元、信用扣15分，责令停业整改3天并复查健康证。",
                true, new BigDecimal("100.00"), -15, wangA.getId(),
                "留样温控不达标+操作不规范", staff);

        Complaint c2 = new Complaint();
        c2.setFairDay(oldDay); c2.setVendor(zhao); c2.setAllocation(zhaoA);
        c2.setType("缺斤少两"); c2.setComplainant("陈阿姨"); c2.setContact("136****3344");
        c2.setContent("买了5斤苹果回家复称只有4斤3两。");
        c2.setEvidencePhotos(evidenceImg);
        complaintRepository.save(c2);
        operationService.handleComplaint(c2.getId(), ComplaintStatus.RESOLVED,
                "复称属实，摊主当场补足并道歉；扣保证金50元、信用扣10分，计量器具送检定。",
                true, new BigDecimal("50.00"), -10, zhaoA.getId(), "计量器具未检定且实际短缺", staff);

        Complaint c3 = new Complaint();
        c3.setFairDay(oldDay); c3.setVendor(ma); c3.setAllocation(maA);
        c3.setType("卫生问题"); c3.setComplainant("匿名消费者"); c3.setContact("");
        c3.setContent("烤鸭摊苍蝇较多，砧板生熟不分，担心食品安全。");
        c3.setEvidencePhotos(evidenceImg);
        c3.setStatus(ComplaintStatus.RECEIVED); // 待处理，供演示
        complaintRepository.save(c3);

        // 散场退还部分保证金
        operationService.refundDeposit(liA.getId(), null, "无违规，散场全额退还保证金", staff);
        operationService.refundDeposit(sunA.getId(), null, "整改到位，散场全额退还保证金", staff);

        operationService.updateDayStatus(oldDay.getId(), DayStatus.CLOSED);

        // ================= 新集市（重阳大集，报名中） =================
        FairDay newDay = new FairDay();
        newDay.setName("2026-10-08 重阳大集");
        newDay.setMarketDate(LocalDate.of(2026, 10, 8));
        newDay.setSignupDeadline(LocalDateTime.of(2026, 10, 7, 18, 0));
        newDay.setDrawTime(LocalTime.of(4, 30));
        newDay.setStatus(DayStatus.OPEN);
        newDay.setRulesText("""
                本场抽签规则：1.按品类分区（熟食冷链区/活禽隔离区/普通区）；
                2.邻近消防通道摊位不安排熟食明火与活禽；3.用电摊主安排带电源摊位；
                4.车辆尺寸须与摊位匹配；5.老摊主（历史赶集次数）与信用分高者优先抽签；
                6.抽签使用当场公示随机种子，结果可复算；7.临时调位、减免、扣罚一律台账公示。""");
        fairDayRepository.save(newDay);

        MarketZone na = zone(newDay, "A", "普通干货区", ZoneType.NORMAL, "30.00", "10.00", "100.00", "邻近东街");
        MarketZone nb = zone(newDay, "B", "熟食冷链区", ZoneType.COOKED, "40.00", "15.00", "200.00", "配电冰柜，靠近水源");
        MarketZone nc = zone(newDay, "C", "活禽隔离区", ZoneType.POULTRY, "25.00", "0.00", "150.00", "下风口，独立消毒");
        zoneRepository.saveAll(List.of(na, nb, nc));
        fairDayService.generateStalls(newDay.getId(), na.getId(), "A", 12,
                new BigDecimal("4.50"), new BigDecimal("3.50"), 4, List.of(1));
        fairDayService.generateStalls(newDay.getId(), nb.getId(), "B", 8,
                new BigDecimal("4.00"), new BigDecimal("3.00"), 6, List.of());
        fairDayService.generateStalls(newDay.getId(), nc.getId(), "C", 4,
                new BigDecimal("5.00"), new BigDecimal("4.00"), 0, List.of());

        // 已通过审核 6 户
        Application a1 = app(newDay, wang, "卤味", "卤味拼盘、卤豆干", true, false, 3, 2, true, 2000,
                licenseImg, "赶集13次", 13);
        Application a2 = app(newDay, zhang, "凉菜", "凉拌菜", true, false, 2.5, 1.8, false, 0,
                licenseImg, "赶集4次，已参加整改培训", 4);
        Application a3 = app(newDay, sun, "活禽", "土鸡土鸭", false, true, 4.5, 3.5, false, 0,
                licenseImg, "赶集16次", 16);
        Application a4 = app(newDay, chen, "蔬菜", "自产白菜、萝卜", false, false, 4, 3, false, 0,
                null, "赶集21次，老摊主", 21);
        // 赵勇申报大货车（5.2米）→ A区摊位4.5米，抽签时将进候补
        Application a5 = app(newDay, zhao, "水果", "苹果、橘子（大货车直销）", false, false,
                5.2, 3, false, 0, null, "赶集6次", 6);
        Application a6 = app(newDay, wu, "服装", "保暖内衣", false, false, 4, 2.5, false, 0,
                null, "赶集7次", 7);
        // 待审核 2 户（李菊熟食尚未传齐证照；郑力杂货）
        app(newDay, li, "糕点", "重阳糕、发糕", true, false, 3, 2, false, 0,
                null, "赶集9次（健康证待补传）", 9);
        app(newDay, zheng, "杂货", "日用品", false, false, 3.5, 2, false, 0,
                null, "赶集2次", 2);
        applicationRepository.flush();
        for (Application a : List.of(a1, a2, a3, a4, a5, a6)) {
            a.setStatus(ApplyStatus.APPROVED);
            a.setReviewer(staff);
            a.setReviewedAt(LocalDateTime.now());
            a.setReviewNote("资格核实通过");
        }
        applicationRepository.flush();

        log.info("演示数据初始化完成：2 个集市、12 名用户、历史集市完整业务链路");
    }

    private SysUser user(String username, String pwd, String name, String phone, Role role,
                         int credit, String address) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(pwd);
        u.setName(name);
        u.setPhone(phone);
        u.setRole(role);
        u.setCreditScore(credit);
        u.setAddress(address);
        return u;
    }

    private MarketZone zone(FairDay day, String code, String name, ZoneType type,
                            String fee, String elec, String deposit, String note) {
        MarketZone z = new MarketZone();
        z.setFairDay(day);
        z.setCode(code);
        z.setName(name);
        z.setType(type);
        z.setStallFee(new BigDecimal(fee));
        z.setElectricityFee(new BigDecimal(elec));
        z.setDepositStandard(new BigDecimal(deposit));
        z.setNote(note);
        return z;
    }

    private Application app(FairDay day, SysUser vendor, String category, String goods,
                            boolean cooked, boolean poultry, double vLen, double vWidth,
                            boolean elec, int power, String license, String history, int historyCount) {
        Application a = new Application();
        a.setFairDay(day);
        a.setVendor(vendor);
        a.setCategory(category);
        a.setGoodsDesc(goods);
        a.setCookedFood(cooked);
        a.setLivePoultry(poultry);
        a.setVehicleLength(BigDecimal.valueOf(vLen));
        a.setVehicleWidth(BigDecimal.valueOf(vWidth));
        a.setNeedElectricity(elec);
        a.setPowerWatt(power);
        a.setLicensePhotos(license);
        a.setHistoryRecord(history);
        a.setHistoryCount(historyCount);
        return applicationRepository.save(a);
    }

    private Allocation byVendor(List<Allocation> allocations, SysUser vendor) {
        return allocations.stream()
                .filter(a -> a.getVendor().getId().equals(vendor.getId()))
                .findFirst().orElseThrow(() -> new IllegalStateException("缺少抽签结果: " + vendor.getName()));
    }

    private void inspection(FairDay day, Allocation allocation, SysUser vendor, SysUser staff,
                            int hygiene, int price, int measure, int aisle, int electric,
                            String issues, String photo, String remark, boolean rectify) {
        Inspection ins = new Inspection();
        ins.setFairDay(day);
        ins.setAllocation(allocation);
        ins.setVendor(vendor);
        ins.setStaff(staff);
        ins.setHygieneScore(hygiene);
        ins.setPriceTagScore(price);
        ins.setMeasureScore(measure);
        ins.setAisleScore(aisle);
        ins.setElectricScore(electric);
        ins.setIssues(issues);
        ins.setPhotos(photo);
        ins.setRemark(remark);
        ins.setRequireRectify(rectify);
        inspectionRepository.save(ins);
    }

    private void foodSample(FairDay day, Allocation allocation, SysUser vendor, String name,
                            String batch, String location, LocalDateTime sampledAt,
                            String photo, double temp) {
        FoodSample s = new FoodSample();
        s.setFairDay(day);
        s.setAllocation(allocation);
        s.setVendor(vendor);
        s.setFoodName(name);
        s.setBatchNo(batch);
        s.setStorageLocation(location);
        s.setSampledAt(sampledAt);
        s.setPhotos(photo);
        s.setStorageTemp(temp);
        s.setRemark("密封留样盒，不少于125克，保存48小时");
        foodSampleRepository.save(s);
    }

    /** 生成演示用占位图片，避免种子数据中的照片 404 */
    private String demoImage(String filename, Color color) {
        try {
            Path dir = Paths.get(uploadDir, "demo");
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            if (!Files.exists(target)) {
                BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(color);
                g.fillRect(0, 0, 320, 200);
                g.setColor(new Color(90, 90, 90));
                g.setStroke(new BasicStroke(3));
                g.drawRect(10, 10, 300, 180);
                g.setStroke(new BasicStroke(2));
                g.drawLine(10, 150, 310, 50);
                g.fillOval(60, 55, 34, 34);
                g.setColor(new Color(60, 60, 60));
                g.setFont(new Font("SansSerif", Font.BOLD, 18));
                g.drawString("DEMO: " + filename.replace(".png", ""), 110, 120);
                g.dispose();
                ImageIO.write(img, "png", target.toFile());
            }
            return "/uploads/demo/" + filename;
        } catch (Exception e) {
            throw new IllegalStateException("生成演示图片失败", e);
        }
    }
}
