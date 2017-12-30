package app.hongs.util.verify;

import app.hongs.HongsException;

/**
 * 规则包裹
 * @author Hongs
 */
public class Rulo extends Rule {
    
    private final Rulx func;

    public Rulo(Rulx func) {
        this . func  = func;
    }

    @Override
    public Object verify(Object value) throws Wrong, Wrongs, HongsException {
        return func.verify(value,this);
    }

    /**
     * 批量将 Rulx 转换为 Rule
     * @param rule
     * @return 
     */
    public static Rule[] toRules(Rulx... rule) {
        Rule[] list = new Rule[rule.length];
        for (int i = 0; i < rule.length; i ++) {
            list[i] = new Rulo(rule[i]);
        }
        return list;
    }
    
}
