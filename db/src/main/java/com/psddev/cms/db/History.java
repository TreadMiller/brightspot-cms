package com.psddev.cms.db;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.db.ObjectType;

import com.psddev.dari.util.PaginatedResult;

/** Represents previous revisions of objects. */
public class History extends Record {

    private @Indexed String name;
    private @Indexed Date updateDate;
    private @Indexed ToolUser updateUser;
    private @Indexed ObjectType objectType;
    private @Indexed UUID objectId;
    private Map<String, Object> objectOriginals;

    /** Creates a blank instance. */
    protected History() {
    }

    /** Creates an instance based on the given {@code object}. */
    public History(ToolUser user, Object object) {
        State objectState = State.getInstance(object);
        getState().setDatabase(objectState.getDatabase());
        this.updateDate = new Date();
        this.updateUser = user;
        this.objectType = objectState.getType();
        this.objectId = objectState.getId();
        this.objectOriginals = objectState.getValues();
    }

    /**
     * Returns a partial list of all the revisions of the object with the
     * given {@code objectId} within the given {@code offset} and
     * {@code limit}.
     */
    public static PaginatedResult<History> findByObjectId(ToolUser user, UUID objectId, long offset, int limit) {
        return Query
                .from(History.class)
                .where("objectId = ?", objectId)
                .sortDescending("updateDate")
                .select(offset, limit);
    }

    /** Returns this history's name. */
    public String getName() {
        return name;
    }

    /** Sets this history's name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the date when the object was updated. */
    public Date getUpdateDate() {
        return updateDate;
    }

    /** Returns the user that updated the object. */
    public ToolUser getUpdateUser() {
        return updateUser;
    }

    /** Returns the object's ID. */
    public UUID getObjectId() {
        return objectId;
    }

    /** Returns an unmodifiable map of all the original values. */
    public Map<String, Object> getObjectOriginals() {
        return objectOriginals == null ?
                Collections.<String, Object>emptyMap() :
                Collections.unmodifiableMap(objectOriginals);
    }

    /** Returns the object. */
    public Object getObject() {
        if (objectType == null) {
            return null;
        } else {
            Object object = objectType.createObject(objectId);
            State state = State.getInstance(object);
            if (objectOriginals != null) {
                state.getValues().putAll(objectOriginals);
            }
            return object;
        }
    }
}
