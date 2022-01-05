package top.fallenangel.tools.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 简化使用{@link JSON#parseObject(String, Type, Feature...)}时的书写量
 */
@SuppressWarnings("unused")
public abstract class T<E> {
    private final Type type;

    public T() {
        Type superType = getClass().getGenericSuperclass();
        type = ((ParameterizedType) superType).getActualTypeArguments()[0];
    }

    public Type t() {
        return type;
    }
}
