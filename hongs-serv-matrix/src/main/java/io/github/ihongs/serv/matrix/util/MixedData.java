/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.ihongs.serv.matrix.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 混合读取, 写入目标
 * 仅能 get/put, 专供 Data 的 includes/incloses
 * @author Hongs
 */
public class MixedData implements Map {

        public final Map ND;
        public final Map OD;

        /**
         * @param nd 目标数据
         * @param od 默认数据
         */
        public MixedData(Map nd, Map od) {
            this.ND = nd;
            this.OD = od;
        }

        @Override
        public Object get (Object key) {
            return ND.containsKey(key)
                 ? ND.get (key)
                 : OD.get (key);
        }

        @Override
        public Object put (Object key , Object value) {
            return ND.put (key, value);
        }

        @Override
        @Deprecated
        public Object remove(Object key) {
            return ND.remove(key);
        }

        @Override
        @Deprecated
        public void putAll(Map map) {
            ND.putAll(map);
        }

        @Override
        @Deprecated
        public void clear() {
            ND.clear();
        }

        @Override
        @Deprecated
        public boolean isEmpty() {
            return  ND.isEmpty();
        }

        @Override
        @Deprecated
        public boolean containsKey(Object key) {
            return  ND.containsKey(key);
        }

        @Override
        @Deprecated
        public boolean containsValue(Object value) {
            return  ND.containsKey(value);
        }

        @Override
        @Deprecated
        public int size() {
            return ND.size();
        }

        @Override
        @Deprecated
        public Collection values() {
            return ND.values();
        }

        @Override
        @Deprecated
        public Set keySet() {
            return ND.keySet();
        }

        @Override
        @Deprecated
        public Set entrySet() {
            return ND.entrySet();
        }

}
