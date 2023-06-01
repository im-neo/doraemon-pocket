package com.neo.doraemon.pocket.util;

import cn.hutool.crypto.digest.MD5;
import org.apache.commons.lang3.StringUtils;

import java.util.Stack;

public class PasswordCracking {

    private String plaintext;
    private int minLength = 0;
    private int maxLength = 0;
    private String encrypt = "";
    
    /**
     * 指定元素
     */
    public static Character[] ELEMENTS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
            , 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
            //, 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    public static Stack<Character> stack = new Stack<>();

    /**
     * 排列组合
     *
     * @param elements 排列的元素
     * @param length   指定长度
     * @param current  起始位置
     */
    public void permutations(Character[] elements, int length, int current) {
        if (current == length) {
            check(stack);
            return;
        }

        for (int i = 0; i < elements.length; i++) {
            stack.add(elements[i]);
            permutations(elements, length, current + 1);
            stack.pop();
        }
    }

    /**
     * 校验值
     *
     * @param stack
     */
    public void check(Stack<Character> stack) {
        if (null == stack) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Character character : stack) {
            sb.append(character);
        }
        match(sb.toString());
    }

    /**
     * 匹配MD5值
     *
     * @param data
     */
    public void match(String data) {
        if (StringUtils.equals(this.encrypt, MD5.create().digestHex(data.getBytes()))) {
            this.setPlaintext(data);
            System.out.println(data);
            throw new RuntimeException("退出");
        }
    }

    /**
     * 破解
     */
    public void crack() {
        for (int i = this.minLength; i < this.maxLength; i++) {
            permutations(ELEMENTS, i, 0);
        }
    }



    private PasswordCracking() {
    }

    public PasswordCracking(int minLength, int maxLength, String encrypt) {
        if (minLength < 0 || maxLength < 0 || null == encrypt || "".equals(encrypt)) {
            throw new RuntimeException("参数错误");
        }
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.encrypt = encrypt;
    }

    public String getPlaintext() {
        return plaintext;
    }

    private void setPlaintext(String plaintext) {
        this.plaintext = plaintext;
    }
    
}