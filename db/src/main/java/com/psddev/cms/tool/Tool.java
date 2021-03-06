package com.psddev.cms.tool;

import com.psddev.dari.db.Application;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectFieldComparator;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Brightspot application, typically used by the internal staff. */
public abstract class Tool extends Application {

    public static final String CONTENT_BOTTOM_WIDGET_POSITION = "cms.contentBottom";
    public static final String CONTENT_RIGHT_WIDGET_POSITION = "cms.contentRight";
    public static final String DASHBOARD_WIDGET_POSITION = "cms.dashboard";

    /**
     * Returns plugins provided by this tool.
     *
     * @return May be {@code null} if this tool doesn't provide any plugins.
     */
    public List<Plugin> getPlugins() {
        return null;
    }

    /**
     * Creates an area with the given parameters.
     *
     * @return Never {@code null}.
     */
    protected Area createArea2(String displayName, String internalName, String hierarchy, String url) {
        Area area = new Area();
        area.setDisplayName(displayName);
        area.setInternalName(internalName);
        area.setHierarchy(hierarchy);
        area.setUrl(url);
        return area;
    }

    /**
     * Creates a JSP widget with the given parameters.
     *
     * @return Never {@code null}.
     */
    protected JspWidget createJspWidget(String displayName, String internalName, String jsp, String positionName, double positionColumn, double positionRow) {
        JspWidget widget = new JspWidget();
        widget.setDisplayName(displayName);
        widget.setInternalName(internalName);
        widget.setJsp(jsp);
        widget.addPosition(positionName, positionColumn, positionRow);
        return widget;
    }

    /** {@link Tool} utility methods. */
    public static final class Static {

        private Static() {
        }

        /**
         * Returns all plugins across all tools.
         *
         * @return Never {@code null}. Sorted by {@code displayName}.
         */
        @SuppressWarnings("unchecked")
        public static List<Plugin> getPlugins() {
            List<Plugin> databasePlugins = Query.from(Plugin.class).selectAll();
            List<Plugin> plugins = new ArrayList<Plugin>();

            for (ObjectType type : Database.Static.getDefault().getEnvironment().getTypesByGroup(Tool.class.getName())) {
                if (type.isAbstract() || type.isEmbedded()) {
                    continue;
                }

                Class<?> objectClass = type.getObjectClass();

                if (objectClass == null || !Tool.class.isAssignableFrom(objectClass)) {
                    continue;
                }

                Tool tool = Application.Static.getInstance((Class<? extends Tool>) objectClass);
                List<Plugin> toolPlugins = tool.getPlugins();

                if (toolPlugins != null && !toolPlugins.isEmpty()) {
                    for (Plugin plugin : toolPlugins) {
                        plugin.setTool(tool);
                        plugins.add(plugin);
                    }

                } else {
                    for (Plugin plugin : databasePlugins) {
                        if (tool.equals(plugin.getTool())) {
                            plugins.add(plugin);
                        }
                    }
                }
            }

            Collections.sort(plugins, new ObjectFieldComparator("displayName", true));

            return plugins;
        }

        /**
         * Returns all plugins of the given {@code pluginClass} across all
         * tools.
         *
         * @return Never {@code null}. Sorted by {@code displayName}.
         */
        @SuppressWarnings("unchecked")
        public static <T extends Plugin> List<T> getPluginsByClass(Class<T> pluginClass) {
            List<Plugin> plugins = getPlugins();

            for (Iterator<Plugin> i = plugins.iterator(); i.hasNext(); ) {
                Plugin plugin = i.next();

                if (!pluginClass.isInstance(plugin)) {
                    i.remove();
                }
            }

            return (List<T>) plugins;
        }

        /** Returns all top-level areas. */
        public static List<Area> getTopAreas() {
            List<Area> topAreas = new ArrayList<Area>();
            Area first = null;
            Area last = null;

            for (Area area : getPluginsByClass(Area.class)) {
                if (area.getHierarchy().contains("/")) {
                    continue;
                }

                if (area.getTool() instanceof CmsTool) {
                    String internalName = area.getInternalName();
                    if ("dashboard".equals(internalName)) {
                        first = area;
                        continue;
                    } else if ("admin".equals(internalName)) {
                        last = area;
                        continue;
                    }
                }

                topAreas.add(area);
            }

            if (first != null) {
                topAreas.add(0, first);
            }

            if (last != null) {
                topAreas.add(last);
            }

            return topAreas;
        }

        /**
         * Returns a table of all widgets with the given
         * {@code positionName}.
         */
        public static List<List<Widget>> getWidgets(String positionName) {
            Map<Double, Map<Double, Widget>> widgetsMap = new TreeMap<Double, Map<Double, Widget>>();
            List<List<Widget>> widgetsTable = new ArrayList<List<Widget>>();

            for (Widget widget : getPluginsByClass(Widget.class)) {
                for (Widget.Position position : widget.getPositions()) {
                    if (ObjectUtils.equals(position.getName(), positionName)) {
                        double column = position.getColumn();
                        Map<Double, Widget> widgets = widgetsMap.get(column);

                        if (widgets == null) {
                            widgets = new TreeMap<Double, Widget>();
                            widgetsMap.put(column, widgets);
                        }

                        widgets.put(position.getRow(), widget);
                        break;
                    }
                }
            }

            for (Map<Double, Widget> map : widgetsMap.values()) {
                List<Widget> widgets = new ArrayList<Widget>();

                widgets.addAll(map.values());
                widgetsTable.add(widgets);
            }

            return widgetsTable;
        }
    }

    // --- Deprecated ---

    /** @deprecated Use {@link Static#getPluginsByClass} instead. */
    @Deprecated
    @SuppressWarnings("unchecked")
    public <T extends Plugin> List<T> findPlugins(Class<T> pluginClass) {
        List<T> plugins = new ArrayList<T>();

        for (Plugin plugin : Static.getPlugins()) {
            if (pluginClass.isInstance(plugin) && plugin.getTool() != null) {
                plugins.add((T) plugin);
            }
        }

        return plugins;
    }

    /** @deprecated Use {@link Static#getTopAreas} instead. */
    @Deprecated
    public List<Area> findTopAreas() {
        return Static.getTopAreas();
    }

    /** @deprecated Use {@link Static#getWidgets} instead. */
    @Deprecated
    public List<List<Widget>> findWidgets(String positionName) {
        return Static.getWidgets(positionName);
    }

    // Synchronizes the given {@code plugin} with the existing one in the
    // database if it can be found.
    private void synchronizePlugin(Plugin plugin) {
        State pluginState = plugin.getState();
        Database database = getState().getDatabase();

        pluginState.setDatabase(database);

        UUID typeId = pluginState.getTypeId();
        Tool tool = plugin.getTool();
        String internalName = plugin.getInternalName();

        for (Plugin p : Static.getPlugins()) {
            if (ObjectUtils.equals(typeId, p.getState().getTypeId()) &&
                    ObjectUtils.equals(tool, p.getTool()) &&
                    ObjectUtils.equals(internalName, p.getInternalName())) {
                pluginState.setId(p.getId());
                break;
            }
        }
    }

    /** @deprecated Use {@link #getPlugins} instead. */
    @Deprecated
    public void introducePlugin(Plugin plugin) {
        synchronizePlugin(plugin);
        plugin.save();
    }

    /** @deprecated Use {@link #getPlugins} instead. */
    @Deprecated
    public void discontinuePlugin(Plugin plugin) {
        synchronizePlugin(plugin);
        plugin.delete();
    }

    /** @deprecated No replacement. */
    @Deprecated
    public Area createArea(String displayName, String internalName, Area parent, String url) {
        Area area = new Area();
        area.setTool(this);
        area.setDisplayName(displayName);
        area.setInternalName(internalName);
        area.setParent(parent);
        area.setUrl(url);
        synchronizePlugin(area);
        return area;
    }

    /** @deprecated No replacement. */
    @Deprecated
    public <T extends Widget> T createWidget(Class<T> widgetClass, String displayName, String internalName, String iconName) throws Exception {
        T widget = TypeDefinition.getInstance(widgetClass).newInstance();
        widget.setTool(this);
        widget.setDisplayName(displayName);
        widget.setInternalName(internalName);
        widget.setIconName(iconName);
        synchronizePlugin(widget);
        return widget;
    }
}
