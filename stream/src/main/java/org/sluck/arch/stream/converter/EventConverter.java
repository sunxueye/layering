package org.sluck.arch.stream.converter;

/**
 * 事件转换器接口，用于将转换事件
 *
 * Created by sunxy on 2019/3/27 15:50.
 */
public interface EventConverter {

    /**
     * 是否可以将源类型转换为目标类型
     *
     * @param sourceType
     * @param targetType
     * @return
     */
    boolean canConvert(Class sourceType, Class targetType);

    /**
     * 将事件类型转换为目标类型
     *
     * @param event
     * @return
     */
    <T, E> T fromEvent(E event, Class<T> targetType);

    /**
     * 将目标类型转换为事件类型
     *
     * @param playload
     * @return
     */
    <E, T> E toEvent(Class<E> targetType, T playload);
}
