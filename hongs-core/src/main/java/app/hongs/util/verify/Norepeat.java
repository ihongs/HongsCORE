package app.hongs.util.verify;

public class Norepeat extends Rule {
    @Override
    public Object verify(Object value) throws Wrong {
        try {
            Repeated rule = new Repeated();
            rule.setHelper(helper);
            rule.setParams(params);
            rule.setValues(values);
            value = rule.verify(value);
        }   catch (Wrong w) {
            return value;
        }
        throw new Wrong("fore.form.norepeat");
    }
}
