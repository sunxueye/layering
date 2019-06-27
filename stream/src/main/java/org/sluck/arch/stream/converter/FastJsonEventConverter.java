package org.sluck.arch.stream.converter;

import com.alibaba.fastjson.JSON;

/**
 * 使用 fastjson 进行 json 到 Object 之间的转换
 *
 * Created by sunxy on 2019/3/27 16:04.
 */
public class FastJsonEventConverter implements EventConverter {

    @Override
    public boolean canConvert(Class sourceType, Class targetType) {
        if (sourceType == null || targetType == null) {
            return false;
        }

        if (isBaseType(sourceType) || isBaseType(targetType)) {
            return false;
        }

        return sourceType.equals(String.class) || targetType.equals(String.class);
    }

    @Override
    public <T, E> T fromEvent(E event, Class<T> targetType) {
        return JSON.parseObject(event.toString(), targetType);
    }

    @Override
    public <E, T> E toEvent(Class<E> targetType, T playload) {
        return (E) JSON.toJSONString(playload);
    }

    /**
     * 判断object是否为基本类型
     * @param className
     * @return
     */
    private boolean isBaseType(Class className) {
        if (className.equals(java.lang.Integer.class) ||
                className.equals(java.lang.Byte.class) ||
                className.equals(java.lang.Long.class) ||
                className.equals(java.lang.Double.class) ||
                className.equals(java.lang.Float.class) ||
                className.equals(java.lang.Character.class) ||
                className.equals(java.lang.Short.class) ||
                className.equals(java.lang.Boolean.class)) {
            return true;
        }
        return false;
    }
}
