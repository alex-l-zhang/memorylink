package com.memorylink.connection;

import java.util.Map;
import java.util.Set;

/**
 * 亲属关系字典：使用具体称谓，避免"子女/父母"这类笼统表述。
 */
public final class RelationCatalog {

    private RelationCatalog() {
    }

    public static final Set<String> CODES = Set.of(
            "SPOUSE", "FATHER", "MOTHER", "SON", "DAUGHTER",
            "OLDER_BROTHER", "OLDER_SISTER", "YOUNGER_BROTHER", "YOUNGER_SISTER",
            "GRANDFATHER_PATERNAL", "GRANDMOTHER_PATERNAL",
            "GRANDFATHER_MATERNAL", "GRANDMOTHER_MATERNAL",
            "GRANDSON", "GRANDDAUGHTER", "GRANDSON_DAUGHTER", "GRANDDAUGHTER_DAUGHTER",
            "DAUGHTER_IN_LAW", "SON_IN_LAW", "GRANDSON_WIFE", "GRANDDAUGHTER_HUSBAND",
            "IN_LAW_PARENT", "IN_LAW_GRANDPARENT",
            "AUNT_PATERNAL", "AUNT_MATERNAL",
            "UNCLE_PATERNAL_ELDER", "UNCLE_PATERNAL_YOUNGER", "UNCLE_MATERNAL",
            "NEPHEW", "NIECE", "FRIEND", "OTHER", "FAMILY"
    );

    /** 无法判断具体称谓时的兜底称呼：确认关系时可选"家人"，不强迫用户给出精确称谓。 */
    public static final String FALLBACK = "FAMILY";

    /** 确认关系时的系统建议称谓（仅建议，接收方可修改）。 */
    public static final Map<String, String> SUGGESTIONS = Map.ofEntries(
            Map.entry("SON", "FATHER"),
            Map.entry("DAUGHTER", "FATHER"),
            Map.entry("FATHER", "SON"),
            Map.entry("MOTHER", "SON"),
            Map.entry("GRANDSON", "GRANDFATHER_PATERNAL"),
            Map.entry("GRANDDAUGHTER", "GRANDFATHER_PATERNAL"),
            Map.entry("GRANDCHILD", "GRANDFATHER_PATERNAL"),
            Map.entry("SPOUSE", "SPOUSE"),
            Map.entry("OLDER_BROTHER", "YOUNGER_BROTHER"),
            Map.entry("OLDER_SISTER", "YOUNGER_BROTHER"),
            Map.entry("YOUNGER_BROTHER", "OLDER_BROTHER"),
            Map.entry("YOUNGER_SISTER", "OLDER_BROTHER"),
            Map.entry("SIBLING", "OLDER_BROTHER"),
            Map.entry("FRIEND", "FRIEND"),
            Map.entry("OTHER", "FAMILY"),
            Map.entry("FAMILY", "FAMILY")
    );

    public static String suggest(String code) {
        return code == null ? null : SUGGESTIONS.get(normalizeLegacy(code));
    }

    /** 旧数据/无法判定性别时的兜底映射。 */
    public static final Map<String, String> LEGACY_GENERIC = Map.of(
            "CHILD", "SON",
            "PARENT", "FATHER",
            "GRANDCHILD", "GRANDSON",
            "GRANDPARENT", "GRANDFATHER_PATERNAL",
            "SIBLING", "OLDER_BROTHER"
    );

    public static boolean isValid(String code) {
        return code != null && (CODES.contains(code) || LEGACY_GENERIC.containsKey(code));
    }

    public static String normalizeLegacy(String code) {
        if (code == null) {
            return null;
        }
        return LEGACY_GENERIC.getOrDefault(code, code);
    }
}
