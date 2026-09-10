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
            "AUNT_PATERNAL", "AUNT_MATERNAL",
            "UNCLE_PATERNAL_ELDER", "UNCLE_PATERNAL_YOUNGER", "UNCLE_MATERNAL",
            "NEPHEW", "NIECE", "FRIEND", "OTHER"
    );

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
        return LEGACY_GENERIC.getOrDefault(code, code);
    }
}
