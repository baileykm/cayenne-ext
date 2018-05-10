package com.pr.cayenne.ext;

import org.apache.cayenne.CayenneDataObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityUtils {

    final static private Pattern PATTERN_LIST_PARAMS = Pattern.compile("<L(.+?);");

    /**
     * 将 CayenneDataObject封装成指定的VO对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T packVO(CayenneDataObject entity, Class<T> voClass) {
        if (entity == null) {
            throw new RuntimeException("entity为空!");
        }
        if (CayenneDataObject.class.isAssignableFrom(voClass)) {
            throw new RuntimeException("VO对象不应为CayenneDataObject子类!");
        }


        // 创建VO对应实例
        T vo = null;
        try {
            vo = voClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("实例化VO对象出错!", e);
        }

        Method[] methods = voClass.getMethods();
        Class<?> entityClass = entity.getClass();

        for (Method method : methods) {

            String methodName = method.getName();

            // 忽略非setter方法
            // 忽略标注了不进行序列化的字段
            if (!method.isAccessible() && !methodName.startsWith("set") || method.isAnnotationPresent(IgnoreSerialize.class)) continue;

            Method setter = method;

            // 看CayenneDataObject中是否有对应的getter方法
            Method getter = null;
            try {
                getter = entityClass.getMethod("get" + methodName.substring(3));
            } catch (NoSuchMethodException | SecurityException e) {
                continue;
            }

            // 完全匹配, 执行数据复制
            try {
                Object value = getter.invoke(entity);
                if (value instanceof List) {        // List值
                    // 提取setter的泛型参数类型
                    Field f = setter.getClass().getDeclaredField("signature");
                    f.setAccessible(true);
                    String paramString = f.get(setter).toString();
                    f.setAccessible(false);
                    Matcher matcher = PATTERN_LIST_PARAMS.matcher(paramString);
                    Class<?> setterParameterType = null;
                    if (matcher.find()) {
                        String className = matcher.group(1).replace("/", ".");
                        setterParameterType = Class.forName(className);
                    }
                    if (setterParameterType == null) throw new RuntimeException("复制属性值出错: " + methodName);
                    setter.invoke(vo, packVOList((List<? extends CayenneDataObject>) value, setterParameterType));
                } else if (value instanceof CayenneDataObject) {    // 值为Cayenne对象
                    Class<?>[] setterParameterTypes = setter.getParameterTypes();
                    setter.invoke(vo, packVO((CayenneDataObject) value, setterParameterTypes[0]));
                } else {        // 普通值
                    setter.invoke(vo, value);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException
                    | NoSuchFieldException | SecurityException e) {
                throw new RuntimeException("复制属性值出错: " + methodName, e);
            }
        }
        return vo;
    }

    /**
     * 将 CayenneDataObject List 封装成指定的VO对象的List
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> packVOList(List<? extends CayenneDataObject> entityList, Class<T> voClass) {

        if (entityList == null) return null;
        if (entityList.isEmpty()) return new ArrayList<T>(0);

        /*
         * 先找出有对应getter和setter的方法, 以提高效率
         */
        List<Method> getters = new ArrayList<>();
        List<Method> setters = new ArrayList<>();

        Class<?> entityClass = entityList.get(0).getClass();
        Method[] methods = voClass.getMethods();
        for (Method method : methods) {

            String methodName = method.getName();

            // 忽略非setter方法
            if (!method.isAccessible() && !methodName.startsWith("set") || method.isAnnotationPresent(IgnoreSerialize.class)) continue;

            Method setter = method;

            // 看CayenneDataObject中是否有对应的getter方法
            Method getter = null;
            try {
                getter = entityClass.getMethod("get" + methodName.substring(3));
            } catch (NoSuchMethodException | SecurityException e) {
                continue;
            }

            getters.add(getter);
            setters.add(setter);
        }

        // 进行复制
        List<T> result = new ArrayList<>(entityList.size());
        for (CayenneDataObject entity : entityList) {
            T vo = null;
            try {
                vo = voClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("实例化VO对象出错!", e);
            }

            for (int i = 0, size = setters.size(); i < size; i++) {
                try {
                    Object value = getters.get(i).invoke(entity);
                    Method setter = setters.get(i);
                    if (value instanceof CayenneDataObject) {
                        Class<?>[] setterParameterTypes = setters.get(i).getParameterTypes();
                        setter.invoke(vo, packVO((CayenneDataObject) value, setterParameterTypes[0]));
                    } else if (value instanceof List) {
                        // 提取setter的泛型参数类型
                        Field f = setter.getClass().getDeclaredField("signature");
                        f.setAccessible(true);
                        String paramString = f.get(setter).toString();
                        f.setAccessible(false);
                        Matcher matcher = PATTERN_LIST_PARAMS.matcher(paramString);
                        Class<?> setterParameterType = null;
                        if (matcher.find()) {
                            String className = matcher.group(1).replace("/", ".");
                            setterParameterType = Class.forName(className);
                        }
                        if (setterParameterType == null) throw new RuntimeException("复制属性值出错: " + setter.getName());
                        setter.invoke(vo, packVOList((List<? extends CayenneDataObject>) value, setterParameterType));
                    } else {
                        setter.invoke(vo, value);
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException
                        | SecurityException | ClassNotFoundException e) {
                    throw new RuntimeException("复制属性值出错!", e);
                }
            }
            result.add(vo);
        }
        return result;
    }
}
