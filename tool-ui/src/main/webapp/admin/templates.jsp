<%@ page import="

com.psddev.cms.db.ContentSection,
com.psddev.cms.db.HorizontalContainerSection,
com.psddev.cms.db.MainSection,
com.psddev.cms.db.ScriptSection,
com.psddev.cms.db.Section,
com.psddev.cms.db.VerticalContainerSection,
com.psddev.cms.db.Template,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Query,
com.psddev.dari.db.State,

java.util.List
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requirePermission("area/admin/adminTemplates")) {
    return;
}

Object selected = wp.findOrReserve(Template.class, ContentSection.class, HorizontalContainerSection.class, MainSection.class, ScriptSection.class, VerticalContainerSection.class);
State selectedState = State.getInstance(selected);
if (wp.include("/WEB-INF/updateObject.jsp", "object", selected)) {
    return;
}

List<Section> shareableSections = Query.
        from(Section.class).
        where("isShareable = true").
        and(wp.siteItemsPredicate()).
        select();

// --- Presentation ---

%><% wp.include("/WEB-INF/header.jsp"); %>

<div class="withLeftNav">
    <div class="leftNav">

        <div class="widget">
            <h1>Templates &amp; Sections</h1>

            <h2>Templates</h2>
            <ul class="links">
                <li class="new<%= selected.getClass() == Template.class && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, Template.class) %>">New Template</a>
                </li>
                <% for (Template template : wp.
                        queryFrom(Template.class).
                        where(wp.siteItemsPredicate()).
                        sortAscending("name").
                        select()) { %>
                    <li<%= template.equals(selected) ? " class=\"selected\"" : "" %>>
                        <a href="<%= wp.objectUrl(null, template) %>"><%= wp.objectLabel(template) %></a>
                    </li>
                <% } %>
            </ul>

            <% if (!shareableSections.isEmpty()) { %>
                <h2>Shareable Sections</h2>
                <ul class="links">
                    <% for (Section section : shareableSections) { %>
                        <li<%= section.equals(selected) ? " class=\"selected\"" : "" %>>
                            <a href="<%= wp.objectUrl(null, section) %>"><%= wp.objectLabel(section) %></a>
                        </li>
                    <% } %>
                </ul>
            <% } %>
        </div>

    </div>
    <div class="main">

        <div class="widget">
            <% wp.include("/WEB-INF/editObject.jsp", "object", selected); %>
        </div>

    </div>
</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
