package io.github.ihongs.dh;

import java.util.Map;
import java.util.Set;

/**
 * 字段参数
 * @author Hongs
 */
public interface IFigure {

    public Map getFields();

    public Map getParams();

    public Set<String> getListable();

    public Set<String> getSortable();

    public Set<String> getFindable();

    public Set<String> getSrchable();

    public Set<String> getRankable();

}