package org.sluck.arch.stream.converter;

/**
 * 事件转换器接口，用于将转换事件
 *
 * Created by sunxy on 2019/3/27 15:50.
 */
public interface EventConverter<E, T> {

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
    T fromEvent(E event);

    /**
     * 将目标类型转换为事件类型
     *
     * @param playload
     * @return
     */
    E toEvent(T playload);
}
