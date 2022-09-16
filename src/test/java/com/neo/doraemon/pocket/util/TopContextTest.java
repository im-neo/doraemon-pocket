package com.neo.doraemon.pocket.util;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;

public class TopContextTest {
    public static void main(String[] args) {
        List<Student> collect = Lists.newArrayList(
                new Student(1, 7),
                new Student(2, 6),
                new Student(3, 5),
                new Student(3, 4),
                new Student(3, 3),
                new Student(3, 2),
                new Student(4, 1)
        );


        System.out.println("按成绩排序取倒数 3 名");
        List<Student> result = TopContext.build(collect, Student::getScore, Comparator.comparing(Student::getScore)).top(3);
        for (Student student : result) {
            System.out.println(student);
        }

        System.out.println("按成绩排序取前 3 名");
        result = TopContext.build(collect, Student::getScore, Comparator.comparing(Student::getScore).reversed()).top(3);
        for (Student student : result) {
            System.out.println(student);
        }


        TopContext<Student, Integer> topContext = TopContext.build(
                collect,
                Student::getRank,
                Comparator.comparing(Student::getRank, Comparator.reverseOrder())
        );
        System.out.println("按排名排序取倒数 2 名");
        result = topContext.top(2);
        for (Student student : result) {
            System.out.println(student);
        }

        System.out.println("按排名排序取倒数 3 名");
        result = topContext.top(3);
        for (Student student : result) {
            System.out.println(student);
        }
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Student {
        private Integer score;
        private Integer rank;
    }
}
