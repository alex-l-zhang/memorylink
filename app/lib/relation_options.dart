class RelationOption {
  final String code;
  final String label;

  const RelationOption(this.code, this.label);
}

/// 亲属关系字典：使用具体称谓（儿子/女儿/父亲/母亲/爷爷/外婆…）
const List<RelationOption> relationOptions = [
  RelationOption('SPOUSE', '配偶'),
  RelationOption('FATHER', '父亲'),
  RelationOption('MOTHER', '母亲'),
  RelationOption('SON', '儿子'),
  RelationOption('DAUGHTER', '女儿'),
  RelationOption('OLDER_BROTHER', '哥哥'),
  RelationOption('OLDER_SISTER', '姐姐'),
  RelationOption('YOUNGER_BROTHER', '弟弟'),
  RelationOption('YOUNGER_SISTER', '妹妹'),
  RelationOption('GRANDFATHER_PATERNAL', '爷爷'),
  RelationOption('GRANDMOTHER_PATERNAL', '奶奶'),
  RelationOption('GRANDFATHER_MATERNAL', '外公（姥爷）'),
  RelationOption('GRANDMOTHER_MATERNAL', '外婆（姥姥）'),
  RelationOption('GRANDSON', '孙子'),
  RelationOption('GRANDDAUGHTER', '孙女'),
  RelationOption('GRANDSON_DAUGHTER', '外孙子'),
  RelationOption('GRANDDAUGHTER_DAUGHTER', '外孙女'),
  RelationOption('DAUGHTER_IN_LAW', '儿媳'),
  RelationOption('SON_IN_LAW', '女婿'),
  RelationOption('GRANDSON_WIFE', '孙媳妇'),
  RelationOption('GRANDDAUGHTER_HUSBAND', '孙女婿'),
  RelationOption('AUNT_PATERNAL', '姑'),
  RelationOption('AUNT_MATERNAL', '姨'),
  RelationOption('UNCLE_PATERNAL_ELDER', '伯父'),
  RelationOption('UNCLE_PATERNAL_YOUNGER', '叔父'),
  RelationOption('UNCLE_MATERNAL', '舅舅'),
  RelationOption('NEPHEW', '侄子'),
  RelationOption('NIECE', '侄女'),
  RelationOption('FRIEND', '朋友'),
  RelationOption('OTHER', '其他'),
];

/// 主动建立联系沿用同一套具体称谓
const List<RelationOption> connectionRelationOptions = relationOptions;

const Map<String, String> _legacyLabels = {
  'CHILD': '子女',
  'PARENT': '父母',
  'GRANDCHILD': '孙辈',
  'GRANDPARENT': '祖辈',
  'SIBLING': '兄弟姐妹',
};

String relationLabel(String? code) {
  if (code == null || code.isEmpty) return '家人';
  for (final option in relationOptions) {
    if (option.code == code) return option.label;
  }
  return _legacyLabels[code] ?? code;
}

/// 反向称谓建议：修改一侧时自动填充另一侧（性别/支系不明确时给常见默认值，可手动调整）
const Map<String, String> _inverseSuggestions = {
  'SPOUSE': 'SPOUSE',
  'FATHER': 'SON',
  'MOTHER': 'SON',
  'SON': 'FATHER',
  'DAUGHTER': 'FATHER',
  'OLDER_BROTHER': 'YOUNGER_BROTHER',
  'OLDER_SISTER': 'YOUNGER_BROTHER',
  'YOUNGER_BROTHER': 'OLDER_BROTHER',
  'YOUNGER_SISTER': 'OLDER_BROTHER',
  'GRANDFATHER_PATERNAL': 'GRANDSON',
  'GRANDMOTHER_PATERNAL': 'GRANDSON',
  'GRANDFATHER_MATERNAL': 'GRANDSON_DAUGHTER',
  'GRANDMOTHER_MATERNAL': 'GRANDSON_DAUGHTER',
  'GRANDSON': 'GRANDFATHER_PATERNAL',
  'GRANDDAUGHTER': 'GRANDFATHER_PATERNAL',
  'GRANDSON_DAUGHTER': 'GRANDFATHER_MATERNAL',
  'GRANDDAUGHTER_DAUGHTER': 'GRANDFATHER_MATERNAL',
  'AUNT_PATERNAL': 'NEPHEW',
  'AUNT_MATERNAL': 'NEPHEW',
  'UNCLE_PATERNAL_ELDER': 'NEPHEW',
  'UNCLE_PATERNAL_YOUNGER': 'NEPHEW',
  'UNCLE_MATERNAL': 'NEPHEW',
  'NEPHEW': 'UNCLE_PATERNAL_ELDER',
  'NIECE': 'UNCLE_PATERNAL_ELDER',
  'FRIEND': 'FRIEND',
  'OTHER': 'OTHER',
};

String? suggestedInverse(String code) => _inverseSuggestions[code];
