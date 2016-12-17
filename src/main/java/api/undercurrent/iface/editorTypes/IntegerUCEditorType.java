package api.undercurrent.iface.editorTypes;

import api.undercurrent.iface.UCEditorType;

import java.net.URL;

/**
 * Created by Niel on 10/16/2015.
 */
public class IntegerUCEditorType extends UCEditorType {

    private int minValue;
    private int maxValue;

    public IntegerUCEditorType(String fieldName, int fieldValue, String displayName, String displayDescription, int minValue, int maxValue, String editorGroup, URL image) {
        super(EditorTypes.INT, editorGroup, image);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.displayName = displayName;
        this.displayDescription = displayDescription;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return (Integer) fieldValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayDescription() {
        return displayDescription;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    @Override
    public boolean validateValue(Object obj) throws Exception {
        try {
            double objcast = Integer.valueOf(String.valueOf(obj));

            if (objcast > getMaxValue()) {
                return false;
            }

            if (objcast < getMinValue()) {
                return false;
            }
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
