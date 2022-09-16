package com.neo.doraemon.pocket.util;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 考虑排序值相同的情况下取前几名的数据
 * <pre>
 *     1. 数据：[1,2,3,3,3,4,5]
 *        a. 取前 3 名返回：[1,2,3,3,3]
 *        b. 取前 4 名返回：[1,2,3,3,3]
 *     2. 数据：[1,2,3,4,5]
 *        a. 取前 3 名返回：[1,2,3]
 *        b. 取前 4 名返回：[1,2,3,4]
 *     3. 数据：[5,4,3,2,1]
 *        a. 取前 3 名返回：[1,2,3]
 *        b. 取前 4 名返回：[1,2,3,4]
 * </pre>
 *
 * @author Neo
 * @since 2022/9/16 21:01
 */
@Getter
public class TopContext<T, V> {

    /**
     * 原始数据
     */
    private Collection<T> originalCollect;

    /**
     * 值映射
     */
    private Function<T, V> valueMapper;

    /**
     * 排序方式
     */
    private Comparator<? super T> comparator;

    /**
     * 排序后的原始数据
     */
    private List<T> sortedCollect;

    /**
     * 每个值的数量统计结果
     */
    private Map<V, Long> valueCountMap;

    /**
     * 排序后的值
     */
    private List<V> sortedValueList;

    /**
     * 值排名数据
     */
    private List<ValueRank<V>> rankList;


    public static <T, V> TopContext<T, V> build(Collection<T> originalCollect,
                                                Function<T, V> valueMapper,
                                                Comparator<? super T> comparator) {
        if (CollectionUtils.isEmpty(originalCollect)) {
            return new TopContext<>();
        }

        // 对原始数据根据 Value 排序
        List<T> sortedCollect = originalCollect.stream().sorted(comparator).collect(Collectors.toList());

        // 计算 Value 得分的数量
        Map<V, Long> valueCountMap = originalCollect.stream().collect(Collectors.groupingBy(valueMapper, Collectors.counting()));

        // 对 Value 进行排序
        List<V> sortedValueList = valueCountMap.keySet().stream().sorted().collect(Collectors.toList());

        // 统计每个 Value 的开始排名和结束排名
        long count = 0;
        List<ValueRank<V>> rankList = new ArrayList<>();
        for (V value : sortedValueList) {
            rankList.add(new ValueRank<>(value, count, (count += valueCountMap.get(value))));
        }

        return new TopContext<>(originalCollect, valueMapper, comparator, sortedCollect, valueCountMap, sortedValueList, rankList);
    }

    /**
     * 取前几名的数据
     *
     * @author Neo
     * @since 2022/9/16 09:12
     */
    public List<T> top(int n) {
        if (n < 1) {
            return Collections.EMPTY_LIST;
        }
        if (this.sortedCollect.size() <= n) {
            return new ArrayList<>(this.sortedCollect);
        }

        // 找到目标排名的分数开始排名和结束排名
        Optional<ValueRank<V>> optional = rankList.stream().filter(i -> i.getStartRank() <= n && n <= i.getEndRank()).findFirst();
        if (!optional.isPresent()) {
            return new ArrayList<>(this.sortedCollect);
        }

        ValueRank<V> targetValueRank = optional.get();
        return CollUtil.sub(sortedCollect, 0, (int) targetValueRank.getEndRank());
    }


    private TopContext() {
    }


    private TopContext(Collection<T> originalCollect,
                       Function<T, V> valueMapper,
                       Comparator<? super T> comparator,
                       List<T> sortedCollect,
                       Map<V, Long> valueCountMap,
                       List<V> sortedValueList,
                       List<ValueRank<V>> rankList) {
        this.originalCollect = originalCollect;
        this.valueMapper = valueMapper;
        this.comparator = comparator;
        this.sortedCollect = sortedCollect;
        this.valueCountMap = valueCountMap;
        this.sortedValueList = sortedValueList;
        this.rankList = rankList;
    }


    /**
     * 值排名数据
     *
     * @author Neo
     * @since 2022/9/15 21:48
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValueRank<R> {

        /**
         * 得分
         */
        private R value;

        /**
         * 开始排名
         */
        private long startRank;

        /**
         * 结束排名
         */
        private long endRank;
    }
}
