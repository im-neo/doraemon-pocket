package com.neo.doraemon.pocket.util;

import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

public class PasswordCrackingTest {


    public static void main(String[] args) {
        PasswordCracking passwordCracking = new PasswordCracking(6, 9, "e10adc3949ba59abbe56e057f20f883e");
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            passwordCracking.crack();
        } catch (RuntimeException e) {
        }
        System.out.println("耗时：" + stopwatch.elapsed(TimeUnit.SECONDS));
    }
}
