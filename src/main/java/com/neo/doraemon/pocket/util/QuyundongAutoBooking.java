package com.neo.doraemon.pocket.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.mail.MailUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 趣运动自动订场地
 *
 * @author Neo
 * @since 2023/9/1 11:20
 */
public class QuyundongAutoBooking {
    public static final String LOGIN_URL = "https://m.quyundong.com/login/";
    public static final String USERNAME = "10086", PASSWORD = "10086@Quyundong";

    public static final List<String> EMAIL_TO = Lists.newArrayList("10086@gmail.com");

    public static final LocalTime START_TIME = LocalTime.of(7, 30), END_TIME = LocalTime.of(21, 30);


    public static final String BOOK_URL = "https://m.quyundong.com/court/book?bid={}&t={}&cid={}";

    public static final List<String> COURT_LIST = Lists.newArrayList("5号场", "2号场", "6号场", "3号场", "4号场", "1号场");

    public static final List<TargetGoods> TARGET_GOODS_CONFIG = Lists.newArrayList(
            new TargetGoods("0907", new TargetTimeGoods("18:00-19:00", 2), new TargetTimeGoods("19:00-20:00", 2))
    );

    public WebDriver driver;


    /**
     * 初始化
     *
     * @author Neo
     * @since 2023/8/23 13:56
     */
    public QuyundongAutoBooking() {
        // 设置为移动端
        ChromeOptions options = new ChromeOptions();
        // 设置后台无窗口运行
        options.addArguments("headless");
        options.addArguments("--no-sandbox");
        options.setExperimentalOption("mobileEmulation", ImmutableMap.of(
                "userAgent", "Mozilla/5.0 (Linux; U; Android 4.0.4; en-gb; GT-I9300 Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
                "deviceMetrics", ImmutableMap.of(
                        "width", 360,
                        "height", 640,
                        "pixelRatio", 3.0
                )
        ));
        driver = new ChromeDriver(options);
    }

    private void sleep() {
        this.sleep(2000L);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 登录
     *
     * @author Neo
     * @since 2023/6/1 13:50
     */
    public void login() {
        driver.get(LOGIN_URL);
        this.sleep();
        // 输入用户名密码
        driver.findElement(By.className("J_tel")).sendKeys(USERNAME);
        driver.findElement(By.className("J_pwd")).sendKeys(PASSWORD);
        // 点击登录按钮
        driver.findElement(By.className("J_submit")).click();
    }


    /**
     * 预定
     *
     * @author Neo
     * @since 2023/8/24 14:34
     */
    public void booking() {
        for (TargetGoods targetGoods : TARGET_GOODS_CONFIG) {
            if (targetGoods.isFinished()) {
                continue;
            }

            String targetDate = this.dateConvert(targetGoods.getDate());

            driver = this.driver.switchTo().newWindow(WindowType.TAB);
            driver.get(this.generateBookingUrl(targetDate));
            this.sleep();
            List<WebElement> courtElementList = driver.findElement(By.className("book-list")).findElements(By.className("available"));

            List<String> courtContentList = courtElementList.stream().map(i -> i.getAttribute("content")).collect(Collectors.toList());
            System.out.println(DateUtil.now() + " >>> " + (CollectionUtils.isEmpty(courtContentList) ? "无任何可预订场地" : Joiner.on(",").join(courtContentList)));

            List<TargetTimeGoods> targetTimeGoodsList = targetGoods.getGoodsList();
            for (TargetTimeGoods targetTimeGoods : targetTimeGoodsList) {
                if (targetTimeGoods.isFinished()) {
                    break;
                }

                List<String> targetGoodsList = this.generateTargetGoods(targetTimeGoods.getTime());

                outer:
                for (String goods : targetGoodsList) {
                    for (WebElement courtElement : courtElementList) {
                        String content = courtElement.getAttribute("content");
                        if (StringUtils.equals(goods, content)) {
                            courtElement.click();
                            targetTimeGoods.reserved(goods.split(" ")[1]);
                        }
                        if (targetTimeGoods.isFinished()) {
                            break outer;
                        }
                    }
                }
            }

            // 提交订单
            this.submit(targetGoods);
        }


        long currentBookingSum = TARGET_GOODS_CONFIG.stream()
                .map(TargetGoods::getGoodsList)
                .flatMap(Collection::stream)
                .map(TargetTimeGoods::getCurrentBookingNum)
                .mapToInt(Integer::intValue)
                .sum();
        // 本次预定数量大于 0 ，发送预定成功邮件并处理预定成功数据
        if (currentBookingSum > 0L) {
            this.assembleSuccessEmail();
            this.processSuccessData();
        }
    }

    /**
     * 提交订单
     *
     * @author Neo
     * @since 2023/8/31 15:25
     */
    public void submit(TargetGoods targetGoods) {
        // 本次预定数量大于 0 ，提交订单
        long bookingSum = targetGoods.getGoodsList().stream()
                .map(TargetTimeGoods::getCurrentBookingNum)
                .mapToInt(Integer::intValue)
                .sum();
        if (bookingSum > 0L) {
            driver.findElement(By.className("J_submit")).click();
            this.sleep(2000);
            driver.findElement(By.id("orderSubmit")).click();
            this.sleep(2000);
        }

        // 关闭当前窗口，切换到下一个窗口
        driver.close();
        driver.switchTo().window(driver.getWindowHandles().iterator().next());
    }


    /**
     * 预定成功后的动作
     *
     * @author Neo
     * @since 2023/8/25 10:29
     */
    public void assembleSuccessEmail() {
        System.out.println(LocalDateTime.now());
        System.out.println("预定成功：" + JSONUtil.formatJsonStr(JSONUtil.toJsonStr(TARGET_GOODS_CONFIG)));
        // 发送预定成功的邮件
        StringBuilder context = new StringBuilder();
        context.append("<table style=\"border-collapse: collapse; width: 500px;\">");
        context.append("<tr>");
        context.append("<th style=\"width: 100px; border: 1px solid black; padding: 8px; text-align: center; background-color: green; color: white;\">日期</th>");
        context.append("<th style=\"width: 100px; border: 1px solid black; padding: 8px; text-align: center; background-color: green; color: white;\">时间</th>");
        context.append("<th style=\"width: 100px; border: 1px solid black; padding: 8px; text-align: center; background-color: green; color: white;\">已预订</th>");
        context.append("</tr>");
        for (TargetGoods targetGoods : TARGET_GOODS_CONFIG) {
            for (TargetTimeGoods targetTimeGoods : targetGoods.getGoodsList()) {
                context.append("<tr>");
                context.append("<td style=\"width: 100px; border: 1px solid black; padding: 8px; text-align: center;\">")
                        .append(targetGoods.getDate(), 0, 2).append("月").append(targetGoods.getDate().substring(2)).append("日")
                        .append("</td>");
                context.append("<td style=\"width: 100px; border: 1px solid black; padding: 8px; text-align: center;\">")
                        .append(targetTimeGoods.getTime())
                        .append("</td>");
                context.append("<td style=\"width: 100px; border: 1px solid black; padding: 8px; text-align: center;\">")
                        .append(String.join(",", targetTimeGoods.getReservedNos()))
                        .append("</td>");
                context.append("</tr>");
            }
        }
        context.append("</table>");
        try {
            MailUtil.sendHtml(EMAIL_TO, "【场地预定成功】", context.toString());
        } catch (Exception e) {
            System.err.println("邮件发送失败");
            System.err.println(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * 处理预定数据
     *
     * @author Neo
     * @since 2023/8/31 16:13
     */
    public void processSuccessData() {
        for (TargetGoods targetGoods : TARGET_GOODS_CONFIG) {
            for (TargetTimeGoods targetTimeGoods : targetGoods.getGoodsList()) {
                targetTimeGoods.setReservedNum(targetTimeGoods.getReservedNum() + targetTimeGoods.getCurrentBookingNum());
                targetTimeGoods.setCurrentBookingNum(0);
            }
        }
    }


    /**
     * 时间转换
     *
     * @author Neo
     * @since 2023/8/23 15:40
     */
    public String dateConvert(String date) {
        date = LocalDate.now().getYear() + date;
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.valueOf(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000);
    }


    /**
     * 生成指定体育中心指定场地类型下单URL
     *
     * @author Neo
     * @since 2023/8/23 15:51
     */
    public String generateBookingUrl(String date) {
        return generateBookingUrl("10086", "1", date);
    }


    /**
     * 生成下单URL
     *
     * @author Neo
     * @since 2023/8/23 15:51
     */
    public String generateBookingUrl(String businessId, String categoryId, String date) {
        return StrFormatter.format(BOOK_URL, businessId, date, categoryId);
    }

    /**
     * 根据优先级生成商品列表
     *
     * @author Neo
     * @since 2023/8/23 15:59
     */
    public List<String> generateTargetGoods(String time) {
        List<String> result = new ArrayList<>();
        for (String court : COURT_LIST) {
            result.add(time + " " + court);
        }
        return result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetGoods {
        private String date;

        private List<TargetTimeGoods> goodsList;

        public TargetGoods(String date, TargetTimeGoods... goodsArray) {
            this.date = date;
            if (Objects.nonNull(goodsArray)) {
                this.goodsList = Arrays.asList(goodsArray);
            }
        }

        public boolean isFinished() {
            return this.goodsList.stream().allMatch(TargetTimeGoods::isFinished);
        }
    }

    @Data
    @NoArgsConstructor
    public static class TargetTimeGoods {
        private String time;

        /**
         * 目标数量
         */
        private Integer targetNum;

        /**
         * 已预订数量
         */
        private Integer reservedNum;

        /**
         * 本次预定数量
         */
        private Integer currentBookingNum;

        private List<String> reservedNos;

        public TargetTimeGoods(String time, int targetNum) {
            this.time = time;
            this.targetNum = targetNum;
            this.reservedNum = 0;
            this.currentBookingNum = 0;
            this.reservedNos = new ArrayList<>();
        }

        public void reserved(String reservedNo) {
            this.reservedNos.add(reservedNo);
            this.currentBookingNumIncr();
        }

        public void currentBookingNumIncr() {
            this.currentBookingNum++;
        }

        /**
         * 是否已完成
         */
        public boolean isFinished() {
            return this.currentBookingNum + this.reservedNum >= this.targetNum;
        }
    }


    public static void main(String[] args) {
        QuyundongAutoBooking autoOrder = new QuyundongAutoBooking();

        // 注册 JVM 关闭钩子函数
        Runtime.getRuntime().addShutdownHook(new Thread(() -> autoOrder.driver.quit()));

        autoOrder.login();
        autoOrder.sleep();

        while (true) {
            try {
                LocalTime now = LocalTime.now();
                if (now.isBefore(START_TIME) || now.isAfter(END_TIME)) {
                    continue;
                }

                autoOrder.booking();

                boolean finished = TARGET_GOODS_CONFIG.stream().allMatch(TargetGoods::isFinished);
                if (finished) {
                    break;
                }

                long nextLoop = RandomUtil.randomLong(30 * 1000, 60 * 1000);
                autoOrder.sleep(nextLoop);
            } catch (Exception e) {
                System.out.println(ExceptionUtils.getStackTrace(e));
            }
        }
    }


}
