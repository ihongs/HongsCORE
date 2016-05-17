package app.hongs.util.verify;

/**
 * 可选约束
 * @author Hongs
 */
public class Optional extends Rule {
     @Override
     public Object verify(Object value) throws Wrong {
         try {
             Required rule = new Required();
             rule.setHelper(helper);
             rule.setParams(params);
             rule.setValues(values);
             value = rule.verify(value);
         }   catch (Wrong w) {
             return BLANK;
         }
         return value;
     }
 }
