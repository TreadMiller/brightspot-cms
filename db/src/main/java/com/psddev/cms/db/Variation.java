package com.psddev.cms.db;

import com.psddev.dari.db.Database;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.PeriodicValue;
import com.psddev.dari.util.PullThroughValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Changes objects based on rules.
 *
 * <p>Some of the uses of this class are: A/B testing, internationalization,
 * and delivery of a different layout for mobile.
 */
public class Variation extends Record {

    public static final String APPLIED_EXTRA = "cms.variation.applied";

    private static final Logger LOGGER = LoggerFactory.getLogger(Variation.class);

    @Indexed(unique = true)
    @Required
    private String name;

    @Indexed(unique = true)
    @Required
    @ToolUi.Hidden
    private double position;

    @Required
    private Rule rule;

    @Required
    private Operation operation;

    /** Returns the name. Displayed in the tool UI. */
    public String getName() {
        return this.name;
    }

    /** Sets the name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the position. Determines the order during {@link #applyAll}. */
    public double getPosition() {
        return this.position;
    }

    /** Sets the position. */
    public void setPosition(double position) {
        this.position = position;
    }

    /** Returns the rule. */
    public Rule getRule() {
        return this.rule;
    }

    /** Sets the rule. */
    public void setRule(Rule rule) {
        this.rule = rule;
    }

    /** Returns the operation. */
    public Operation getOperation() {
        return this.operation;
    }

    /** Sets the operation. */
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    /** {@link Variation} utility methods. */
    public static final class Static {

        private Static() {
        }

        private static final PullThroughValue<PeriodicValue<List<Variation>>>
                ALL = new PullThroughValue<PeriodicValue<List<Variation>>>() {

            @Override
            protected PeriodicValue<List<Variation>> produce() {
                return new PeriodicValue<List<Variation>>() {

                    @Override
                    protected List<Variation> update() {
                        Query<Variation> query = Query.from(Variation.class).sortAscending("position").using(Database.Static.getDefaultOriginal());
                        Date cacheUpdate = getUpdateDate();
                        Date databaseUpdate = query.lastUpdate();

                        if (databaseUpdate == null || (cacheUpdate != null && !databaseUpdate.after(cacheUpdate))) {
                            List<Variation> variations = get();
                            return variations != null ? variations : Collections.<Variation>emptyList();
                        }

                        LOGGER.info("Loading variations");
                        return query.selectAll();
                    }
                };
            }
        };

        /**
         * Applies all variations to the given {@code object} using the
         * given {@code profile}.
         *
         * @throws IllegalArgumentException If the given {@code object}
         *         or {@code profile} is {@code null}.
         */
        public static void applyAll(Object object, Profile profile) {
            ErrorUtils.errorIfNull(object, "object");
            ErrorUtils.errorIfNull(profile, "profile");

            List<Variation> applied = getApplied(object);

            for (Variation variation : ALL.get().get()) {
                try {
                    if (!applied.contains(variation) &&
                            variation.getRule().evaluate(variation, profile, object)) {
                        applied.add(variation);
                        variation.getOperation().evaluate(variation, profile, object);
                    }

                } catch (Throwable error) {
                    LOGGER.warn(String.format(
                            "Can't apply variation [%s] to [%s]!",
                            variation.getId(),
                            State.getInstance(object).getId()),
                            error);
                }
            }
        }

        /**
         * Returns the list of variations that have been applied to
         * the given {@code object} so far.
         *
         * @throws IllegalArgumentException If the given {@code object}
         *         is {@code null}.
         */
        public static List<Variation> getApplied(Object object) {
            ErrorUtils.errorIfNull(object, "object");

            Map<String, Object> extras = State.getInstance(object).getExtras();
            @SuppressWarnings("unchecked")
            List<Variation> applied = (List<Variation>) extras.get(APPLIED_EXTRA);

            if (applied == null) {
                applied = new ArrayList<Variation>();
                extras.put(APPLIED_EXTRA, applied);
            }

            return applied;
        }
    }

    // --- Deprecated ---

    /** @deprecated Use {@link Static#applyAll} instead. */
    @Deprecated
    public static void applyAll(Object object, Profile profile) {
        Static.applyAll(object, profile);
    }

    /** @deprecated Use {@link Static#getApplied} instead. */
    @Deprecated
    public static List<Variation> getApplied(Object object) {
        return Static.getApplied(object);
    }
}
