package core.framework.impl.template.fragment;

import core.framework.impl.template.TemplateContext;

/**
 * @author neo
 */
interface Fragment {
    void process(StringBuilder builder, TemplateContext context);
}
