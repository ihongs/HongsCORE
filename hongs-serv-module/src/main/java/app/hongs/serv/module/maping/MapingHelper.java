package app.hongs.serv.module.maping;

import app.hongs.Cnst;
import app.hongs.util.Synt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据别名映射
 * @author Hongs
 */
public class MapingHelper {

    Map maping;
    Map mapout;

    public MapingHelper(Map form) {
        maping = new HashMap();
        mapout = new HashMap();
        for(Object ot : form.entrySet()) {
            Map.Entry et = (Map.Entry) ot;
            String n = (String) et.getKey();
            Map    c = (Map ) et.getValue();
            String a = (String) c.get("alias");
            if (a != null && a.length() != 0 ) {
                maping.put(a, n);
                mapout.put(n, a);
            }
        }
    }

    private Map map(Map info, Map map) {
        Map newInfo = new LinkedHashMap();
        for(Object ot : info.entrySet( )) {
            Map.Entry et = (Map.Entry) ot;
            Object k = et.getKey(  );
            Object v = et.getValue();
            Object n = map.get(k);
            if (k != null) {
                newInfo.put(n, v);
            } else {
                newInfo.put(k, v);
            }
        }
        return  newInfo;
    }

    public Set in (Set term) {
        if(maping.isEmpty()) {
            return term;
        }
        Set newTerm = new LinkedHashSet();
        for(Object o : term) {
            Object x = maping.get(o);
            if (x != null) {
                newTerm.add(x);
            } else {
                newTerm.add(o);
            }
        }
        return  newTerm;
    }

    public Map in (Map info) {
        if(maping.isEmpty()) {
            return info;
        }
        return map(info, maping);
    }

    public Map out(Map info) {
        if(mapout.isEmpty()) {
            return info;
        }
        return map(info, mapout);
    }

    public Map checkRequest(Map data) {
        if(maping.isEmpty()) {
            return data;
        }

        Map newData = in(data);
        Map subData ;
        Set setData ;

        subData = Synt.declare(newData.get(Cnst.OR_KEY), Map.class);
        if (subData != null) {
            newData.put(Cnst.OR_KEY, in(subData));
        }

        subData = Synt.declare(newData.get(Cnst.AR_KEY), Map.class);
        if (subData != null) {
            newData.put(Cnst.AR_KEY, in(subData));
        }

        subData = Synt.declare(newData.get(Cnst.SR_KEY), Map.class);
        if (subData != null) {
            newData.put(Cnst.SR_KEY, in(subData));
        }

        setData = Synt.asTerms(newData.get(Cnst.RB_KEY));
        if (setData != null) {
            newData.put(Cnst.RB_KEY, in(setData));
        }

        setData = Synt.asTerms(newData.get(Cnst.OB_KEY));
        if (setData != null) {
            newData.put(Cnst.OB_KEY, in(setData));
        }

        return newData;
    }

    public Map checkResults(Map data) {
        if(mapout.isEmpty()) {
            return data;
        }

        Map newData = new LinkedHashMap(data);

        Map anum = Synt.declare(data.get("enum"), Map.class);
        if (anum != null) {
            newData.put("enum", out(anum));
        }

        Map info = Synt.declare(data.get("info"), Map.class);
        if (info != null) {
            newData.put("info", out(info));
        }

        // 列表数据需要遍历每一行
        List<Map> list = Synt.declare(data.get("list"), List.class);
        if (list != null) {
            List newList = new ArrayList(list.size( ));
            for(Map item : list) {
                 newList.add(out(item) );
            }
            newData.put("list", newList);
        }

        return data;
    }

}
