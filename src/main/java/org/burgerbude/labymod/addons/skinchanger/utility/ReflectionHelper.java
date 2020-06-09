package org.burgerbude.labymod.addons.skinchanger.utility;

import java.lang.reflect.Field;

/**
 * Some reflection help codes
 *
 * @author Robby
 */
public class ReflectionHelper {

    /**
     * Updates a field
     *
     * @param field  The field to be update
     * @param object The object of the field
     * @param value  The new value for the field
     */
    public void updateFieldValue(Field field, Object object, Object value) {
        try {
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            field.set(object, value);
            field.setAccessible(accessible);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the value from the given field
     *
     * @param field  The field from which the value is to be taken
     * @param object The object of the field
     * @return the field value or <b>null</b>
     */
    public Object getFieldValue(Field field, Object object) {
        Object value = null;
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        try {
            value = field.get(object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        field.setAccessible(accessible);
        return value;
    }

}
