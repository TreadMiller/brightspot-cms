<%@ page import="

com.psddev.cms.db.Content,
com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.State,
com.psddev.dari.util.JspUtils,
com.psddev.dari.util.ObjectUtils,

java.util.List
" %><%

ToolPageContext wp = new ToolPageContext(pageContext);
State state = State.getInstance(request.getAttribute("object"));
ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
ToolUi ui = field.as(ToolUi.class);
ObjectType type = field.getParentType();

boolean isHidden = ui.isHidden();

if (!isHidden &&
        field.isDeprecated() &&
        ObjectUtils.isBlank(state.get(field.getInternalName()))) {
    isHidden = true;
}

if (!isHidden && type != null) {
    isHidden = !wp.hasPermission("type/" + field.getParentType().getId() + "/read")
            || !wp.hasPermission("type/" + field.getParentType().getId() + "/field/" + fieldName + "/read");
}
if (isHidden) {
    return;
}

boolean isFormPost = (Boolean) request.getAttribute("isFormPost");
boolean isReadOnly = ui.isReadOnly();
if (!isReadOnly && type != null) {
    isReadOnly = !wp.hasPermission("type/" + type.getId() + "/write")
            || !wp.hasPermission("type/" + type.getId() + "/field/" + fieldName + "/write");
}
if (isFormPost && isReadOnly) {
    return;
}

// Wrapped in try/finally because return is used for flow control.
String fieldPrefix = (String) request.getAttribute("fieldPrefix");

if (fieldPrefix == null) {
    fieldPrefix = "";
}

try {
    request.setAttribute("fieldPrefix", fieldPrefix + fieldName + "/");

    // Standard header.
    if (!isFormPost) {
        wp.write("<div class=\"inputContainer");
        if (isReadOnly) {
            wp.write(" inputContainer-readOnly");
        }
        wp.write("\" data-field=\"");
        wp.write(wp.h(fieldPrefix + fieldName));
        wp.write("\" data-name=\"");
        wp.write(wp.h(state.getId()));
        wp.write("/");
        wp.write(wp.h(fieldName));
        wp.write("\">");
        wp.write("<div class=\"label\"><label for=\"", wp.createId(), "\">");
        wp.write(wp.h(field.getLabel()));
        wp.write("</label></div>");

        // Field-specific error messages.
        List<String> errors = state.getErrors(field);
        if (!ObjectUtils.isBlank(errors)) {
            wp.write("<div class=\"error message\">");
            for (String error : errors) {
                wp.write(wp.h(error), " ");
            }
            wp.write("</div>");
        }

        // Write out a helpful note if available.
        String noteHtml = ui.getEffectiveNoteHtml(request.getAttribute("object"));
        if (!ObjectUtils.isBlank(noteHtml)) {
            wp.write("<small class=\"note\">");
            wp.write(noteHtml);
            wp.write("</small>");
        }
    }

    String processorPath = ui.getInputProcessorPath();
    if (processorPath != null) {
        JspUtils.include(request, response, out, processorPath);
        return;
    }

    // Look for class/field-specific handler.
    // TODO - There should be some type of a hook for external plugins.
    String prefix = wp.cmsUrl("/WEB-INF/field/");
    String path = prefix + field.getJavaDeclaringClassName() + "." + fieldName + ".jsp";
    if (application.getResource(path) != null) {
        JspUtils.include(request, response, out, path);
        return;
    }

    // Look for most specific field type handler first.
    // For example, given list/map/any, following JSPs are examined:
    // - list/map/any.jsp
    // - list/map.jsp
    // - list.jsp
    // - default.jsp
    String displayType = ToolUi.getFieldDisplayType(field);
    if (ObjectUtils.isBlank(displayType)) {
        displayType = field.getInternalType();
    }
    while (true) {

        path = prefix + displayType + ".jsp";
        if (application.getResource(path) != null) {
            JspUtils.include(request, response, out, path);
            return;
        }

        int slashAt = displayType.lastIndexOf("/");
        if (slashAt < 0) {
            break;
        } else {
            displayType = displayType.substring(0, slashAt);
        }
    }

    JspUtils.include(request, response, out, prefix + "default.jsp");

} finally {
    request.setAttribute("fieldPrefix", fieldPrefix);

    // Standard footer.
    if (!isFormPost) {
        wp.write("</div>");
    }
}
%>
