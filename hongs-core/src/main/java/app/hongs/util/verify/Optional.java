package app.hongs.util.verify;

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
             return FALSE;
         }
         return value;
     }
 }
