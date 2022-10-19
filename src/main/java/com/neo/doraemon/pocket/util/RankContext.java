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
public class RankContext<T, V> {

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
     * 排序后的值
     */
    private List<V> sortedValueList;

    /**
     * 构建
     *
     * @author Neo
     * @since 2022/10/10 20:37
     */
    public static <T, V> RankContext<T, V> build(Collection<T> originalCollect,
                                                 Function<T, V> valueMapper,
                                                 Comparator<? super T> comparator) {
        if (CollectionUtils.isEmpty(originalCollect)) {
            return new RankContext<>();
        }

        // 对原始数据根据 Value 排序
        List<T> sortedCollect = originalCollect.stream().sorted(comparator).collect(Collectors.toList());

        // 映射 Value
        List<V> sortedValueList = sortedCollect.stream().map(valueMapper).collect(Collectors.toList());

        return new RankContext<>(originalCollect, valueMapper, comparator, sortedCollect, sortedValueList);
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

        V value = this.sortedValueList.get(n);

        int lastIndex = this.sortedValueList.lastIndexOf(value);

        return CollUtil.sub(sortedCollect, 0, lastIndex + 1);
    }

    /**
     * 排名
     * sortedValueList = [1,2,2,3,3,3]
     * 输入    输出
     * 1      1
     * 2      2
     * 3      4
     *
     * @author Neo
     * @since 2022/10/10 20:26
     */
    public int rank(V value) {
        return this.sortedValueList.indexOf(value) + 1;
    }

    /**
     * 倒数排名
     * sortedValueList = [1,2,2,3,3,3]
     * 输入    输出
     * 1      6
     * 2      4
     * 3      1
     *
     * @author Neo
     * @since 2022/10/10 20:26
     */
    public int reverseRank(V value) {
        return this.sortedValueList.size() - this.sortedValueList.lastIndexOf(value);
    }


    private RankContext() {
    }


    private RankContext(Collection<T> originalCollect,
                        Function<T, V> valueMapper,
                        Comparator<? super T> comparator,
                        List<T> sortedCollect,
                        List<V> sortedValueList) {
        this.originalCollect = originalCollect;
        this.valueMapper = valueMapper;
        this.comparator = comparator;
        this.sortedCollect = sortedCollect;
        this.sortedValueList = sortedValueList;
    }
}
