package com.easyvalid.cn.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.easyvalid.cn.annotation.Valid;
import com.easyvalid.cn.annotation.Valids;
import com.easyvalid.cn.basevalid.IValid;
import com.easyvalid.cn.bean.FieldAndIValid;
import com.easyvalid.cn.bean.ValidErrorBean;

public class ValidManager {

    /**
     * 缓存验证信息
     */
    @SuppressWarnings("unchecked")
    private static final Map<Class, List<FieldAndIValid>> CACHEVALID =
            new HashMap<Class, List<FieldAndIValid>>();

    /**
     * 检测所有属性，并且返回所有属性的验证不通过信息
     * 
     * @param o
     * @return
     */
    public static List<ValidErrorBean> validAll(Object o) {

        List<FieldAndIValid> favList = getCacheValid(o);
        List<ValidErrorBean> validAllErrorList = null;
        for (FieldAndIValid fav : favList) {
            ValidErrorBean veb = valid(o, fav);
            if (veb != null) {
                if (validAllErrorList == null) {
                    validAllErrorList = new ArrayList<ValidErrorBean>();
                }
                validAllErrorList.add(veb);
            }
        }
        return validAllErrorList;
    }

    /**
     * 检测所有属性, 如果有一个属性检测不通过，那么返回该错误信息
     */
    public static ValidErrorBean validOne(Object o) {

        List<FieldAndIValid> favList = getCacheValid(o);
        ValidErrorBean veb = null;
        for (FieldAndIValid fav : favList) {
            veb = valid(o, fav);
            if (veb != null) {
                break;
            }
        }

        return veb;
    }

    /**
     * 验证属性
     * 
     * @param o 需要验证的对象
     * @param fav
     * @return
     */
    private static ValidErrorBean valid(Object o, FieldAndIValid fav) {

        Object proValue = null;
        try {
            proValue = fav.getGetMethod().invoke(o, new Object[] {});
        } catch (Exception e) {
            throw new RuntimeException("获取属性值失败", e);
        }

        return fav.getIvalid().valid(o, proValue);
    }

    /**
     * 获取验证器，如果没有 初始或验证器
     * 
     * @param o
     * @return
     */
    private static List<FieldAndIValid> getCacheValid(Object o) {
        Class<?> clazz = o.getClass();
        List<FieldAndIValid> favList = CACHEVALID.get(clazz);

        if (favList == null) {
            synchronized (clazz) {
                favList = CACHEVALID.get(clazz);
                if (favList == null) {
                    initClassToCahce(clazz);
                    favList = CACHEVALID.get(clazz);
                }

            }
        }
        return favList;
    }

    /**
     * 将验证类加入缓存中
     * 
     * @param clazz
     */
    @SuppressWarnings("unchecked")
    private static void initClassToCahce(Class clazz) {

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            setIValidCache(clazz, field,  field.getAnnotation(Valid.class));
            Valids valids = field.getAnnotation(Valids.class);
            if (valids != null) {
                for (Valid valid : valids.value()) {
                    setIValidCache(clazz, field, valid);
                }
            }
        }
        // valid order sort
        Collections.sort(CACHEVALID.get(clazz), new Comparator<FieldAndIValid>() {
            @Override
            public int compare(FieldAndIValid o1, FieldAndIValid o2) {
                return o1.getOrder() - o2.getOrder();
            }
        });

    }

    /**
     * 提取验证类的属性到缓存中
     * 
     * @param clazz
     * @param valid
     */
    @SuppressWarnings("unchecked")
    private static void setIValidCache(Class clazz, Field field, Valid valid) {
        if (valid == null) {
            return;
        }
        IValid ivalid = valid.regular().getValid(clazz, field, valid);

        if (ivalid != null) {
            List<FieldAndIValid> favList = CACHEVALID.get(clazz);
            if (favList == null) {
                favList = new ArrayList<FieldAndIValid>();
                CACHEVALID.put(clazz, favList);
            }
            favList.add(new FieldAndIValid(clazz, field, ivalid, valid));
        }
    }
}
